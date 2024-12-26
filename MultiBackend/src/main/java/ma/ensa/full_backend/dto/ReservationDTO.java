package ma.ensa.full_backend.dto;

import ma.ensa.full_backend.model.Client;

import java.util.Date;
import java.util.List;

@lombok.Data
public class ReservationDTO {
    private Long id;
    private Date checkInDate;
    private Date checkOutDate;
    private Client client;
    private List<ChambreDTO> chambres;
}
