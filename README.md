# GraphQL Performance Testing with Apollo Client

## Overview

This project is designed to perform performance testing on a GraphQL API using the Apollo Client in an Android application. It measures various metrics such as latency, throughput, and resource usage while executing multiple concurrent requests.

## Features

- **Concurrent Request Handling**: Tests the performance of multiple simultaneous GraphQL queries and mutations.
- **Resource Monitoring**: Captures CPU and memory usage during tests.
- **Result Generation**: Generates CSV files for latency, throughput, and resource metrics for analysis.

## Backend

This project supports a multibackend architecture with the following technologies:

- **GraphQL**: For flexible queries and mutations, handling complex data retrieval.
- **SOAP**: For legacy service integration, ensuring compatibility with existing systems.
- **REST**: For standard web service interactions, providing simplicity and ease of use.
- **gRPC**: For high-performance communication, particularly useful for microservices.

### Operations Supported

- **Create Reservation**: Mutation for creating a reservation.
- **Get Reservation**: Query for retrieving reservation details.
- **Update Reservation**: Mutation for updating a reservation.
- **Delete Reservation**: Mutation for deleting a reservation.

You can use any GraphQL server implementation (Node.js, Python, Java, etc.) for each backend, as long as they support the operations mentioned above.

## Requirements

- Android Studio
- Android SDK (API 31 or higher)
- Apollo GraphQL Client

## Setup

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/yourusername/graphql-performance-test.git
   cd graphql-performance-test
2. **Open in Android Studio**:
Open the project in Android Studio.
3. **Add Apollo Client Dependency**:
Make sure to add the Apollo Client dependency in your build.gradle file:
implementation 'com.apollographql.apollo3:apollo-runtime:3.x.x'
4. **Configure the GraphQL Endpoint**:
Update the GraphQL server URL in the GraphQLPerformanceTest constructor.

## Usage
1. **Run the Tests**:
You can run the performance tests by invoking the runAllTests() method from an Activity or Service.
2. **View Results**:
After the tests complete, check the performance_results directory in your app's internal storage for CSV files containing the metrics.

## Emulators Used
For testing, the following emulator configuration was used:
Name: Pixel 6a
Android Version: Android 12 (API 31)
RAM: 2048 MB
Processor: Intel x86
Storage: 5152 MB

## Project Structure
/app
    /src
        /main
            /java
                /com
                    /example
                        /recherche
                            GraphQLPerformanceTest.java
                        /graphqltest
                            (GraphQL query and mutation classes)
            /res
                (Resources)
## Logging
Logs are generated using the Android Log class. You can view logs in the Logcat window in Android Studio to see the details of the test execution and any potential errors.

## Example of Test 
<img width="960" alt="test" src="https://github.com/user-attachments/assets/99263d3b-4d3a-4d58-87a8-76202eca4af2" />
<img width="960" alt="test2" src="https://github.com/user-attachments/assets/9dbb2265-d5b7-4c85-b454-5173d4698326" />


