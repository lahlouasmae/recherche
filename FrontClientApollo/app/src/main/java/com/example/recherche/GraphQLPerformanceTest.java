package com.example.recherche;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.SystemClock;
import android.util.Log;
import com.apollographql.apollo3.ApolloClient;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Optional;
import com.apollographql.apollo3.rx3.Rx3Apollo;
import com.example.graphqltest.*;
import com.example.graphqltest.type.ReservationInput;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import android.os.Process;

public class GraphQLPerformanceTest {
    private static final String TAG = "GraphQLPerformanceTest";
    private final Context context;
    private final ApolloClient apolloClient;
    private final String outputDir;
    private ThreadPoolExecutor executorService;
    private final Map<String, List<OperationMetric>> operationMetrics;
    private final List<ResourceMetric> resourceMetrics;

    // Performance test configurations
    private static final int[] MESSAGE_SIZES = {1}; // KB
    private static final int[] CONCURRENT_REQUESTS = {10};
    private static final int WARMUP_ITERATIONS = 2;
    private static final int TEST_ITERATIONS = 5;

    // Configurable parameters for thread pool
    private static final int CORE_POOL_SIZE = 50;
    private static final int MAXIMUM_POOL_SIZE = 1200;
    private static final long KEEP_ALIVE_TIME = 60L;
    private static final int QUEUE_CAPACITY = 2000;

    // Chamber management
private final List<String> AVAILABLE_ROOM_IDS = new ArrayList<>();

   // Constructeur ou méthode d'initialisation



    private final AtomicInteger currentRoomIndex = new AtomicInteger(0);
    private final AtomicInteger totalReservations = new AtomicInteger(0);

    // Error handling and retry configuration
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private final AtomicInteger errorCount = new AtomicInteger(0);
    private static final int MAX_ERRORS = 100;

    public GraphQLPerformanceTest(Context context, String serverUrl) {
        this.context = context;
        this.apolloClient = ApolloClient.builder()
                .serverUrl(serverUrl)
                .build();
        this.outputDir = context.getFilesDir().getPath() + "/performance_results/";
        this.operationMetrics = new ConcurrentHashMap<>();
        this.resourceMetrics = Collections.synchronizedList(new ArrayList<>());
        initializeThreadPool();
        createOutputDirectory();
        for (int i = 5; i <= 1205; i++) {
            AVAILABLE_ROOM_IDS.add(String.valueOf(i));
        }
    }

    private void initializeThreadPool() {
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        this.executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAXIMUM_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                workQueue,
                new ThreadPoolExecutor.CallerRunsPolicy()  // Prevents task rejection
        );
    }

    private void createOutputDirectory() {
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void runAllTests() {
        Log.i(TAG, "Starting tests...");
        resetCounters();

        try {
            // Warm up phase
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                executeTestSequence(10);
                pauseBetweenTests();
            }

            // Main test phase
            for (int size : MESSAGE_SIZES) {
                for (int concurrentRequests : CONCURRENT_REQUESTS) {
                    if (!runConcurrentTest(size, concurrentRequests)) {
                        Log.e(TAG, "Test aborted due to too many errors");
                        break;
                    }
                    pauseBetweenTests();
                }
            }
        } finally {
            generateResultFiles();
            cleanup();
        }
    }

    private boolean runConcurrentTest(int sizeKB, int concurrentRequests) {
        Log.i(TAG, String.format("Running test: size=%dKB, concurrent=%d", sizeKB, concurrentRequests));
        errorCount.set(0);  // Reset error count for new test

        ResourceMetric resourceMetric = captureResourceMetrics(concurrentRequests);
        resourceMetrics.add(resourceMetric);

        CountDownLatch completionLatch = new CountDownLatch(concurrentRequests * TEST_ITERATIONS);
        List<CompletableFuture<TestResult>> futures = new ArrayList<>();

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            if (errorCount.get() >= MAX_ERRORS) {
                Log.e(TAG, "Aborting test due to too many errors");
                return false;
            }

            List<CompletableFuture<TestResult>> iterationFutures =
                    submitBatch(sizeKB, concurrentRequests, completionLatch);
            futures.addAll(iterationFutures);

            // Wait for batch completion or timeout
            try {
                if (!completionLatch.await(5, TimeUnit.MINUTES)) {
                    Log.w(TAG, "Test timeout reached");
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        processTestResults(futures, sizeKB, concurrentRequests);
        return true;
    }
    private void resetCounters() {
        currentRoomIndex.set(0);
        totalReservations.set(0);
        errorCount.set(0);
    }private void pauseBetweenTests() {
        try {
            Thread.sleep(2000); // 2 secondes entre les tests
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }private void processTestResults(List<CompletableFuture<TestResult>> futures, int sizeKB, int concurrentRequests) {
        for (CompletableFuture<TestResult> future : futures) {
            try {
                TestResult result = future.get();
                if (!result.hasError) {
                    for (OperationTiming timing : result.timings) {
                        String key = timing.operation + "_" + sizeKB + "_" + concurrentRequests;
                        operationMetrics.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
                                .add(new OperationMetric(
                                        timing.operation,
                                        sizeKB,
                                        concurrentRequests,
                                        timing.duration
                                ));
                        Log.i(TAG, String.format("Operation %s recorded: duration = %d ms", timing.operation, timing.duration));
                    }
                } else {
                    Log.e(TAG, "Test execution resulted in an error.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing test result", e);
            }
        }
    }
    private List<CompletableFuture<TestResult>> submitBatch(
            int sizeKB,
            int concurrentRequests,
            CountDownLatch completionLatch) {

        List<CompletableFuture<TestResult>> futures = new ArrayList<>();
        Semaphore rateLimiter = new Semaphore(Math.min(200, concurrentRequests));

        for (int j = 0; j < concurrentRequests; j++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    rateLimiter.acquire();
                    return executeTestSequence(sizeKB);
                } catch (Exception e) {
                    Log.e(TAG, "Error in test execution", e);
                    errorCount.incrementAndGet();
                    return new TestResult(true, new ArrayList<>());
                } finally {
                    rateLimiter.release();
                    completionLatch.countDown();
                }
            }, executorService));
        }

        return futures;
    }

    private TestResult executeTestSequence(int sizeKB) {
        List<OperationTiming> timings = new ArrayList<>();
        boolean hasError = false;
        int retryCount = 0;

        while (retryCount < MAX_RETRIES && !hasError) {
            try {
                // Create
                long startTime = SystemClock.elapsedRealtime();
                ApolloResponse<CreateReservationMutation.Data> createResponse =
                        executeCreate(sizeKB);
                timings.add(new OperationTiming("Create", startTime));

                if (createResponse.data != null &&
                        createResponse.data.getCreateReservation() != null) {

                    String reservationId = createResponse.data.getCreateReservation().getId();

                    // Read
                    startTime = SystemClock.elapsedRealtime();
                    executeRead(reservationId);
                    timings.add(new OperationTiming("Read", startTime));

                    // Update
                    startTime = SystemClock.elapsedRealtime();
                    executeUpdate(reservationId, sizeKB);
                    timings.add(new OperationTiming("Update", startTime));

                    // Delete
                    startTime = SystemClock.elapsedRealtime();
                    executeDelete(reservationId);
                    timings.add(new OperationTiming("Delete", startTime));

                    break;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in test sequence, attempt " + (retryCount + 1), e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * retryCount);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        hasError = true;
                        break;
                    }
                } else {
                    hasError = true;
                }
            }
        }

        return new TestResult(hasError, timings);
    }


    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }


    private void pauseBetweenOperations() {
        try {
            Thread.sleep(1000); // 1 seconde entre les opérations
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ApolloResponse<CreateReservationMutation.Data> executeCreate(int sizeKB) {
        int maxRetries = 3;
        int currentTry = 0;

        while (currentTry < maxRetries) {
            try {
                ReservationInput input = createTestReservation(sizeKB);
                List<String> chambreIds = generateChambreIds();

                Log.d(TAG, String.format("Attempt %d: Creating reservation with dates %s to %s, chamber %s",
                        currentTry + 1, input.getCheckInDate(), input.getCheckOutDate(), chambreIds));

                ApolloResponse<CreateReservationMutation.Data> response = Rx3Apollo.single(
                                apolloClient.mutation(new CreateReservationMutation(input, Optional.present(chambreIds))))
                        .subscribeOn(Schedulers.io())
                        .blockingGet();

                if (response != null && !response.hasErrors() && response.data != null) {
                    totalReservations.incrementAndGet();
                    return response;
                }

                Log.w(TAG, "Create attempt " + (currentTry + 1) + " failed, retrying...");
                Thread.sleep(1000);
            } catch (Exception e) {
                Log.e(TAG, "Error in create attempt " + (currentTry + 1), e);
            }
            currentTry++;
        }

        throw new RuntimeException("Failed to create reservation after " + maxRetries + " attempts");
    }

    private void executeRead(String reservationId) {
        Rx3Apollo.single(apolloClient.query(new GetReservationQuery(reservationId)))
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    private void executeUpdate(String reservationId, int sizeKB) {
        ReservationInput updateInput = createTestReservation(sizeKB);
        List<String> chambreIds = generateChambreIds();

        Rx3Apollo.single(apolloClient.mutation(
                        new UpdateReservationMutation(
                                reservationId,
                                updateInput,
                                Optional.present(chambreIds))))
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    private void executeDelete(String reservationId) {
        Rx3Apollo.single(apolloClient.mutation(
                        new DeleteReservationMutation(reservationId)))
                .subscribeOn(Schedulers.io())
                .blockingGet();
    }

    private ReservationInput createTestReservation(int sizeKB) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Calendar cal = Calendar.getInstance();
        int offset = totalReservations.get() * 2; // 2 jours entre chaque réservation
        cal.add(Calendar.DAY_OF_MONTH, 1 + offset);
        String checkInDate = dateFormat.format(cal.getTime());

        cal.add(Calendar.DAY_OF_MONTH, 1);
        String checkOutDate = dateFormat.format(cal.getTime());

        return new ReservationInput(
                "1",
                checkInDate,
                checkOutDate
        );
    }

    private List<String> generateChambreIds() {
        int index = currentRoomIndex.getAndIncrement() % AVAILABLE_ROOM_IDS.size();
        return Collections.singletonList(AVAILABLE_ROOM_IDS.get(index));
    }

    private ResourceMetric captureResourceMetrics(int concurrentRequests) {
        ActivityManager activityManager =
                (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memInfo);

        return new ResourceMetric(
                concurrentRequests,
                getCpuUsage(),
                (memInfo.totalMem - memInfo.availMem) / (1024.0 * 1024.0)
        );
    }

    private double getCpuUsage() {
        try {
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Debug.MemoryInfo[] memoryInfo =
                    activityManager.getProcessMemoryInfo(new int[]{Process.myPid()});
            return memoryInfo[0].getTotalPss() / 1024.0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting CPU usage", e);
            return 0.0;
        }
    }

    private void generateResultFiles() {
        generateLatencyResults();
        generateThroughputResults();
        generateResourceResults();
    }

    private void generateLatencyResults() {
        File file = new File(outputDir + "latency.csv");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Taille du Message (KB),Opération,REST (ms),SOAP (ms),GraphQL (ms),gRPC (ms)\n");

            for (int size : MESSAGE_SIZES) {
                for (String operation : Arrays.asList("Create", "Read", "Update", "Delete")) {
                    double avgLatency = getAverageLatency(operation, size);
                    writer.write(String.format("%d,%s,,,%.2f,\n",
                            size, operation, avgLatency));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing latency results", e);
        }
    }

    private void generateThroughputResults() {
        File file = new File(outputDir + "throughput.csv");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Nombre de Requêtes Simultanées,REST (req/s),SOAP (req/s),GraphQL (req/s),gRPC (req/s)\n");

            for (int concurrentRequests : CONCURRENT_REQUESTS) {
                double throughput = calculateThroughput(concurrentRequests);
                Log.i(TAG, String.format("Throughput for %d concurrent requests: %.2f req/s", concurrentRequests, throughput));
                writer.write(String.format("%d,,,%.2f,\n", concurrentRequests, throughput));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing throughput results", e);
        }
    }
    private void generateResourceResults() {
        File file = new File(outputDir + "resources.csv");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("Nombre de Requêtes,CPU REST,CPU SOAP,CPU GraphQL,CPU gRPC," +
                    "Mémoire REST,Mémoire SOAP,Mémoire GraphQL,Mémoire gRPC\n");

            for (ResourceMetric metric : resourceMetrics) {
                writer.write(String.format("%d,,,%.2f,,,%.2f,\n",
                        metric.concurrentRequests, metric.cpuUsage, metric.memoryUsage));
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing resource results", e);
        }
    }

    private double getAverageLatency(String operation, int messageSize) {
        List<OperationMetric> metrics = operationMetrics.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.operation.equals(operation) && m.messageSize == messageSize)
                .collect(Collectors.toList());

        return metrics.stream()
                .mapToLong(m -> m.duration)
                .average()
                .orElse(0.0);
    }

    private double calculateThroughput(int concurrentRequests) {
        List<OperationMetric> metrics = operationMetrics.values().stream()
                .flatMap(List::stream)
                .filter(m -> m.concurrentRequests == concurrentRequests)
                .collect(Collectors.toList());

        double avgDuration = metrics.stream()
                .mapToLong(m -> m.duration)
                .average()
                .orElse(0.0);

        // Log the average duration for debugging
        Log.i(TAG, String.format("Average duration for %d concurrent requests: %.2f ms", concurrentRequests, avgDuration));

        return avgDuration > 0 ? (concurrentRequests * 1000.0) / avgDuration : 0.0;
    }

    public void cleanup() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    private static class TestResult {
        final boolean hasError;
        final List<OperationTiming> timings;

        TestResult(boolean hasError, List<OperationTiming> timings) {
            this.hasError = hasError;
            this.timings = timings;
        }
    }

    private static class OperationTiming {
        final String operation;
        final long duration;

        OperationTiming(String operation, long startTime) {
            this.operation = operation;
            this.duration = SystemClock.elapsedRealtime() - startTime;
        }
    }

    private static class OperationMetric {
        final String operation;
        final int messageSize;
        final int concurrentRequests;
        final long duration;

        OperationMetric(String operation, int messageSize, int concurrentRequests, long duration) {
            this.operation = operation;
            this.messageSize = messageSize;
            this.concurrentRequests = concurrentRequests;
            this.duration = duration;
        }
    }

    private static class ResourceMetric {
        final int concurrentRequests;
        final double cpuUsage;
        final double memoryUsage;

        ResourceMetric(int concurrentRequests, double cpuUsage, double memoryUsage) {
            this.concurrentRequests = concurrentRequests;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
        }
    }
}