package fr.butinfo.cinema.salle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalleRepository extends JpaRepository<Salle, Long> {

    List<Salle> findAllByOrderByNomAsc();

    boolean existsByNomIgnoreCase(String nom);

    Optional<Salle> findByNomIgnoreCase(String nom);
}
