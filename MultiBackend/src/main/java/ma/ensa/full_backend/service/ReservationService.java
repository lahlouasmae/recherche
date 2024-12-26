package ma.ensa.full_backend.service;

import ma.ensa.full_backend.dto.ChambreDTO;
import ma.ensa.full_backend.dto.ReservationDTO;
import ma.ensa.full_backend.model.Chambre;
import ma.ensa.full_backend.model.Client;
import ma.ensa.full_backend.model.Reservation;
import ma.ensa.full_backend.model.TypeChambre;
import ma.ensa.full_backend.repository.ChambreRepository;
import ma.ensa.full_backend.repository.ClientRepository;
import ma.ensa.full_backend.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ChambreRepository chambreRepository;

    @Transactional
    public Reservation createReservation(Reservation reservation, List<Long> chambreIds) {

        // Validate reservation details
        validateReservation(reservation);

        // Ensure client exists
        Client client = getClientById(reservation.getClient().getId());
        reservation.setClient(client);

        // Find and assign available chambers
        List<Chambre> chambres = findAvailableChambres(chambreIds, reservation.getCheckInDate(), reservation.getCheckOutDate());

        if (chambres.isEmpty()) {
            throw new IllegalArgumentException("No available chambers match the reservation criteria");
        }

        // Save the reservation first
        Reservation savedReservation = reservationRepository.save(reservation);

        // Update chambers with the reservation
        for (Chambre chambre : chambres) {
            chambre.setReservation(savedReservation);
            chambre.setDisponible(false);
        }
        chambreRepository.saveAll(chambres);

        // Reload the reservation to include updated chambers
        savedReservation.setChambres(chambres);

        return savedReservation;
    }


    @Transactional(readOnly = true)
    public List<Chambre> findAvailableChambres(List<Long> chambreIds, Date checkInDate, Date checkOutDate) {
        // Find chambers that are either not in any reservation or free during the specified dates
        return chambreRepository.findAll().stream()
                .filter(chambre ->
                        chambre.isDisponible() &&
                                (chambreIds == null || chambreIds.contains(chambre.getId()))
                )
                .collect(Collectors.toList());
    }

    @Transactional
    public Reservation updateReservation(Long id, Reservation updatedReservation, List<Long> newChambreIds) {
        // Fetch the existing reservation
        Reservation existingReservation = getReservation(id);

        // Update basic reservation details
        existingReservation.setCheckInDate(updatedReservation.getCheckInDate());
        existingReservation.setCheckOutDate(updatedReservation.getCheckOutDate());

        // Validate updated reservation
        validateReservation(existingReservation);

        // Release current chambers
        releaseCurrentChambres(existingReservation);

        // Find and assign new chambers
        List<Chambre> newChambres = findAvailableChambres(newChambreIds,
                existingReservation.getCheckInDate(),
                existingReservation.getCheckOutDate());

        if (newChambres.isEmpty()) {
            throw new IllegalArgumentException("No available chambers match the updated reservation criteria");
        }

        // Update chambers for the reservation
        for (Chambre chambre : newChambres) {
            chambre.setReservation(existingReservation);
            chambre.setDisponible(false);
        }
        chambreRepository.saveAll(newChambres);

        // Update reservation with new chambers
        existingReservation.setChambres(newChambres);

        return reservationRepository.save(existingReservation);
    }

    @Transactional
    protected void releaseCurrentChambres(Reservation reservation) {
        if (reservation.getChambres() != null) {
            for (Chambre chambre : reservation.getChambres()) {
                chambre.setReservation(null);
                chambre.setDisponible(true);
            }
            chambreRepository.saveAll(reservation.getChambres());
        }
    }

    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = getReservation(id);

        // Release chambers associated with this reservation
        releaseCurrentChambres(reservation);

        // Delete the reservation
        reservationRepository.delete(reservation);
    }

    public Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reservation with ID " + id + " not found"));
    }

    public List<Reservation> listAllReservations() {
        return reservationRepository.findAll();
    }

    public Page<Reservation> listReservations(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return reservationRepository.findAll(pageable);
    }

    public Client getClientById(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new NoSuchElementException("Client with ID " + clientId + " not found"));
    }

    private void validateReservation(Reservation reservation) {
        // Existing validations
        if (reservation.getCheckInDate() == null || reservation.getCheckOutDate() == null) {
            throw new IllegalArgumentException("Check-in and check-out dates must not be null");
        }
        if (reservation.getCheckInDate().after(reservation.getCheckOutDate())) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        if (reservation.getClient() == null) {
            throw new IllegalArgumentException("Reservation must have a client");
        }
    }

    public ReservationDTO mapToDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setCheckInDate(reservation.getCheckInDate());
        dto.setCheckOutDate(reservation.getCheckOutDate());
        dto.setClient(reservation.getClient());
        dto.setChambres(reservation.getChambres().stream().map(this::mapToChambreDTO).collect(Collectors.toList()));
        return dto;
    }



    private ChambreDTO mapToChambreDTO(Chambre chambre) {
        if (chambre == null) {
            return null;
        }
        return new ChambreDTO(
                chambre.getId(),
                chambre.getTypeChambre().name(),
                chambre.getPrix(),
                chambre.isDisponible()
        );
    }
}