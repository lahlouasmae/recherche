package ma.ensa.full_backend.repository;

import ma.ensa.full_backend.model.Chambre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChambreRepository extends JpaRepository<Chambre, Long> {
    // You can add custom queries here if needed
}
