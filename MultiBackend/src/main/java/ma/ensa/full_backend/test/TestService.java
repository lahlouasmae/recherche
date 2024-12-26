package ma.ensa.full_backend.test;

import ma.ensa.full_backend.model.Chambre;
import ma.ensa.full_backend.model.Client;
import ma.ensa.full_backend.model.Reservation;
import ma.ensa.full_backend.model.TypeChambre;
import ma.ensa.full_backend.repository.ChambreRepository;
import ma.ensa.full_backend.repository.ClientRepository;
import ma.ensa.full_backend.repository.ReservationRepository;
import ma.ensa.full_backend.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TestService {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ChambreRepository chambreRepository;

    @Autowired
    private ReservationService reservationService;

    @Transactional
    public void createTestData() {
        // Create multiple clients
        Client client1 = createClient("John", "Doe", "john.doe@example.com");
        Client client2 = createClient("Jane", "Smith", "jane.smith@example.com");

        // Create chambers
        List<Chambre> chambers = createChambers();

        // Create reservations with chambers
        createReservationWithChambers(client1, chambers.subList(0, 2));
        createReservationWithChambers(client2, chambers.subList(2, 4));

        System.out.println("Test data creation completed successfully.");
    }

    private Client createClient(String firstName, String lastName, String email) {
        Client client = new Client();
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setEmail(email);
        client.setPhoneNumber("123-456-7890");
        return clientRepository.save(client);
    }

    private List<Chambre> createChambers() {
        List<Chambre> chambers = new ArrayList<>();

        // Create chambers with different types and prices
        chambers.add(createChambre(TypeChambre.SINGLE, 100.0f));
        chambers.add(createChambre(TypeChambre.SINGLE, 120.0f));
        chambers.add(createChambre(TypeChambre.DOUBLE, 150.0f));
        chambers.add(createChambre(TypeChambre.DOUBLE, 180.0f));
        chambers.add(createChambre(TypeChambre.SUITE, 250.0f));

        return chambers;
    }

    private Chambre createChambre(TypeChambre type, float price) {
        Chambre chambre = new Chambre();
        chambre.setTypeChambre(type);
        chambre.setPrix(price);
        chambre.setDisponible(true);
        return chambreRepository.save(chambre);
    }

    private Reservation createReservationWithChambers(Client client, List<Chambre> chambres) {
        // Create reservation dates
        Date checkInDate = new Date();
        checkInDate.setTime(checkInDate.getTime() + (24 * 60 * 60 * 1000));  // Tomorrow

        Date checkOutDate = new Date();
        checkOutDate.setTime(checkOutDate.getTime() + (5 * 24 * 60 * 60 * 1000));  // 5 days from now

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setClient(client);
        reservation.setCheckInDate(checkInDate);
        reservation.setCheckOutDate(checkOutDate);

        // Get chamber IDs
        List<Long> chambreIds = chambres.stream()
                .map(Chambre::getId)
                .collect(Collectors.toList());

        // Use reservation service to create reservation with chambers
        return reservationService.createReservation(reservation, chambreIds);
    }

    @Transactional
    public void printTestData() {
        // Print clients
        System.out.println("\n--- Clients ---");
        clientRepository.findAll().forEach(client ->
                System.out.println(client.getFirstName() + " " + client.getLastName())
        );

        // Print chambers
        System.out.println("\n--- Chambers ---");
        chambreRepository.findAll().forEach(chambre ->
                System.out.println("Type: " + chambre.getTypeChambre() +
                        ", Price: " + chambre.getPrix() +
                        ", Available: " + chambre.isDisponible())
        );

        // Print reservations
        System.out.println("\n--- Reservations ---");
        reservationRepository.findAll().forEach(reservation -> {
            System.out.println("Client: " + reservation.getClient().getFirstName() +
                    ", Check-in: " + reservation.getCheckInDate() +
                    ", Check-out: " + reservation.getCheckOutDate());
            System.out.println("Chambers:");
            reservation.getChambres().forEach(chambre ->
                    System.out.println("  - Type: " + chambre.getTypeChambre() +
                            ", Price: " + chambre.getPrix())
            );
        });
    }
}