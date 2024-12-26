package ma.ensa.full_backend.controller;

import ma.ensa.full_backend.model.Client;
import ma.ensa.full_backend.model.Reservation;
import ma.ensa.full_backend.model.ReservationInput;
import ma.ensa.full_backend.model.TypeChambre;
import ma.ensa.full_backend.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

@Controller
@CrossOrigin
public class ReservationGraphQLResolver {

    @Autowired
    private ReservationService service;

    @QueryMapping
    public Reservation getReservation(@Argument Long id) {
        return service.getReservation(id);
    }

    @QueryMapping
    public List<Reservation> getReservations() {
        return service.listAllReservations();
    }

    @MutationMapping
    public Reservation createReservation(@Argument ReservationInput input, @Argument List<Long> chambreIds) {
        Reservation reservation = new Reservation();

        // Fetch client by ID and set to reservation
        Client client = service.getClientById(input.getClientId());
        reservation.setClient(client);

        // Set check-in and check-out dates
        reservation.setCheckInDate(input.getCheckInDate());
        reservation.setCheckOutDate(input.getCheckOutDate());

        // Create reservation with specified chambers
        return service.createReservation(reservation, chambreIds);
    }

    @MutationMapping
    public Reservation updateReservation(
            @Argument Long id,
            @Argument ReservationInput input,
            @Argument List<Long> newChambreIds
    ) {
        Reservation updatedReservation = new Reservation();

        // Fetch client by ID if provided and set to reservation
        if (input.getClientId() != null) {
            Client client = service.getClientById(input.getClientId());
            updatedReservation.setClient(client);
        }

        // Update check-in and check-out dates
        if (input.getCheckInDate() != null) {
            updatedReservation.setCheckInDate(input.getCheckInDate());
        }
        if (input.getCheckOutDate() != null) {
            updatedReservation.setCheckOutDate(input.getCheckOutDate());
        }

        // Update reservation with new chambers
        return service.updateReservation(id, updatedReservation, newChambreIds);
    }

    @MutationMapping
    public Boolean deleteReservation(@Argument Long id) {
        service.deleteReservation(id);
        return true;
    }
}
