package fr.butinfo.cinema.seance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeanceRepository extends JpaRepository<Seance, Long> {

    @EntityGraph(attributePaths = {"film", "salle"})
    @Query("""
            select s from Seance s
            where (:du is null or s.debut >= :du)
              and (:au is null or s.debut < :au)
            order by s.debut
            """)
    List<Seance> rechercher(@Param("du") LocalDateTime du, @Param("au") LocalDateTime au);

    @EntityGraph(attributePaths = {"film", "salle"})
    @Query("select s from Seance s where s.id = :id")
    Optional<Seance> findAvecFilmEtSalle(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seance s where s.id = :id")
    Optional<Seance> findByIdForUpdate(@Param("id") Long id);
}
