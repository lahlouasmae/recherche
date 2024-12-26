package ma.ensa.full_backend.grpc;

import io.grpc.stub.StreamObserver;
import ma.ensa.full_backend.model.Client;
import ma.ensa.full_backend.model.Reservation;
import ma.ensa.full_backend.model.TypeChambre;
import ma.ensa.full_backend.service.ReservationService;
import ma.ensa.full_backend.stubs.*;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@GrpcService
public class ReservationGrpcService extends ReservationServiceGrpc.ReservationServiceImplBase {

    @Autowired
    private ReservationService reservationService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    public void createReservation(CreateReservationRequest request, StreamObserver<CreateReservationResponse> responseObserver) {
        try {
            Date checkInDate = parseDate(request.getCheckInDate());
            Date checkOutDate = parseDate(request.getCheckOutDate());

            if (checkInDate.after(checkOutDate)) {
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Check-in date must be before check-out date.")
                        .asRuntimeException());
                return;
            }

            // Fetch client by ID
            Client client = reservationService.getClientById(request.getClientId());

            // Create reservation
            List<Long> chambreIds = request.getChambreIdsList();
            Reservation reservation = new Reservation();
            reservation.setClient(client);
            reservation.setCheckInDate(checkInDate);
            reservation.setCheckOutDate(checkOutDate);

            // Save reservation
            Reservation savedReservation = reservationService.createReservation(reservation, chambreIds);

            CreateReservationResponse response = CreateReservationResponse.newBuilder()
                    .setId(savedReservation.getId())
                    .setMessage("Reservation created successfully.")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ParseException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription("Invalid date format. Expected format is yyyy-MM-dd: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Unexpected error during reservation creation: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void getReservation(GetReservationRequest request, StreamObserver<GetReservationResponse> responseObserver) {
        try {
            Reservation reservation = reservationService.getReservation(request.getId());

            ma.ensa.full_backend.stubs.Reservation grpcReservation = ma.ensa.full_backend.stubs.Reservation.newBuilder()
                    .setId(reservation.getId())
                    .setCheckInDate(DATE_FORMAT.format(reservation.getCheckInDate()))
                    .setCheckOutDate(DATE_FORMAT.format(reservation.getCheckOutDate()))
                    .build();

            GetReservationResponse response = GetReservationResponse.newBuilder()
                    .setReservation(grpcReservation)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Reservation not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Unexpected error retrieving reservation: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void updateReservation(UpdateReservationRequest request, StreamObserver<UpdateReservationResponse> responseObserver) {
        try {
            Date checkInDate = parseDate(request.getCheckInDate());
            Date checkOutDate = parseDate(request.getCheckOutDate());

            if (checkInDate.after(checkOutDate)) {
                responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                        .withDescription("Check-in date must be before check-out date.")
                        .asRuntimeException());
                return;
            }

            // Fetch client by ID (added clientId field in UpdateReservationRequest)
            Client client = reservationService.getClientById(request.getClientId());

            // Update reservation
            List<Long> chambreIds = request.getChambreIdsList();
            Reservation updatedReservation = new Reservation();
            updatedReservation.setId(request.getId());
            updatedReservation.setCheckInDate(checkInDate);
            updatedReservation.setCheckOutDate(checkOutDate);
            updatedReservation.setClient(client);

            // Call service method to update reservation
            Reservation savedReservation = reservationService.updateReservation(updatedReservation.getId(), updatedReservation, chambreIds);

            ma.ensa.full_backend.stubs.Reservation grpcReservation = ma.ensa.full_backend.stubs.Reservation.newBuilder()
                    .setId(savedReservation.getId())
                    .setCheckInDate(DATE_FORMAT.format(savedReservation.getCheckInDate()))
                    .setCheckOutDate(DATE_FORMAT.format(savedReservation.getCheckOutDate()))
                    .build();

            UpdateReservationResponse response = UpdateReservationResponse.newBuilder()
                    .setReservation(grpcReservation)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (NoSuchElementException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Reservation or client not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Unexpected error updating reservation: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void deleteReservation(DeleteReservationRequest request, StreamObserver<DeleteReservationResponse> responseObserver) {
        try {
            reservationService.deleteReservation(request.getId());

            DeleteReservationResponse response = DeleteReservationResponse.newBuilder().build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (NoSuchElementException e) {
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Reservation not found: " + e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Unexpected error deleting reservation: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private Date parseDate(String date) throws ParseException {
        if (date == null || date.isEmpty()) {
            throw new IllegalArgumentException("Date must not be null or empty.");
        }
        return DATE_FORMAT.parse(date);
    }
}