package ma.ensa.full_backend.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Date;
@Getter
@Setter
public class ReservationInput {
    private Long clientId;
    private Date checkInDate;
    private Date checkOutDate;

//    private String typeChambre;

    // Getters and Setters

}