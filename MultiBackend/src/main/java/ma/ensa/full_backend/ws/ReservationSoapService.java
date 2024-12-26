package ma.ensa.full_backend.ws;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import ma.ensa.full_backend.dto.ReservationDTO;
import ma.ensa.full_backend.model.Client;
import ma.ensa.full_backend.model.Reservation;
import ma.ensa.full_backend.model.TypeChambre;
import ma.ensa.full_backend.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
@WebService(targetNamespace = "http://ws.full_backend.ensa.ma/", serviceName = "ReservationWS")
@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
public class ReservationSoapService {

    @Autowired
    private ReservationService reservationService;

    @Transactional
    @WebMethod(action = "http://ws.full_backend.ensa.ma/getReservationById")
    @SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
    public ReservationDTO getReservationById(@WebParam(name = "id") Long id) {
        Reservation reservation = reservationService.getReservation(id);
        return reservationService.mapToDTO(reservation);
    }


    @Transactional
    @WebMethod(action = "http://ws.full_backend.ensa.ma/createReservation")
    @SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
    public ReservationDTO createReservation(
            @WebParam(name = "checkInDate") String checkInDateStr,
            @WebParam(name = "checkOutDate") String checkOutDateStr,
            @WebParam(name = "client") Client client,
            @WebParam(name = "chambreIds") List<Long> chambreIds) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date checkInDate = sdf.parse(checkInDateStr);
            Date checkOutDate = sdf.parse(checkOutDateStr);

            // Create a new reservation
            Reservation reservation = new Reservation();
            reservation.setCheckInDate(checkInDate);
            reservation.setCheckOutDate(checkOutDate);
            reservation.setClient(client);

            if (chambreIds == null) {
                chambreIds = new ArrayList<>();
            }

            // Save reservation and map to DTO
            Reservation createdReservation = reservationService.createReservation(reservation, chambreIds);
            return reservationService.mapToDTO(createdReservation);

        } catch (ParseException e) {
            throw new RuntimeException("Invalid date format", e);
        }
    }

    @Transactional
    @WebMethod(action = "http://ws.full_backend.ensa.ma/updateReservation")
    @SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
    public ReservationDTO updateReservation(
            @WebParam(name = "id") Long id,
            @WebParam(name = "checkInDate") String checkInDateStr,
            @WebParam(name = "checkOutDate") String checkOutDateStr,
            @WebParam(name = "client") Client client,
            @WebParam(name = "chambreIds") List<Long> chambreIds) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date checkInDate = sdf.parse(checkInDateStr);
            Date checkOutDate = sdf.parse(checkOutDateStr);

            // Create updated reservation object
            Reservation updatedReservation = new Reservation();
            updatedReservation.setCheckInDate(checkInDate);
            updatedReservation.setCheckOutDate(checkOutDate);
            updatedReservation.setClient(client);

            if (chambreIds == null) {
                chambreIds = new ArrayList<>();
            }

            // Update reservation and map to DTO
            Reservation updated = reservationService.updateReservation(id, updatedReservation, chambreIds);
            return reservationService.mapToDTO(updated);

        } catch (ParseException e) {
            throw new RuntimeException("Invalid date format", e);
        }
    }

    @Transactional
    @WebMethod(action = "http://ws.full_backend.ensa.ma/deleteReservation")
    @SOAPBinding(style = SOAPBinding.Style.DOCUMENT, use = SOAPBinding.Use.LITERAL)
    public boolean deleteReservation(@WebParam(name = "id") Long id) {
        try {
            reservationService.deleteReservation(id);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}