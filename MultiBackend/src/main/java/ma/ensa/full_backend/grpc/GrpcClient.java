package ma.ensa.full_backend.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ma.ensa.full_backend.stubs.CreateReservationRequest;
import ma.ensa.full_backend.stubs.CreateReservationResponse;
import ma.ensa.full_backend.stubs.ReservationServiceGrpc;

public class GrpcClient {
    public static void main(String[] args) {
        // Create channel and stub
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        ReservationServiceGrpc.ReservationServiceBlockingStub stub = ReservationServiceGrpc.newBlockingStub(channel);

        // Build request using the updated proto definition
        CreateReservationRequest request = CreateReservationRequest.newBuilder()
                .setClientId(1)  // Client ID
                .setCheckInDate("2024-12-12")  // Check-in date
                .setCheckOutDate("2024-12-26")  // Check-out date
                .addChambreIds(1)  // Chambre ID (example)
                .addChambreIds(2)  // Another Chambre ID (example)
                .build();

        // Call the service
        System.out.println("Sending request with clientId: " + request.getClientId());
        CreateReservationResponse response = stub.createReservation(request);

        // Print the response
        System.out.println("Response: " + response.getMessage());

        // Close the channel
        channel.shutdown();
    }
}