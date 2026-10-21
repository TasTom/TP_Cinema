package fr.butinfo.cinema.reservation;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByCode(String code);

    @EntityGraph(attributePaths = {"seance", "seance.film", "seance.salle"})
    @Query("select r from Reservation r where r.code = :code")
    Optional<Reservation> findByCodeWithSeance(@Param("code") String code);

    @EntityGraph(attributePaths = {"seance", "seance.film", "seance.salle"})
    List<Reservation> findAllByOrderByCreeeLeDesc();

    boolean existsByCode(String code);

    @Modifying(flushAutomatically = true)
    @Query("update Reservation r set r.statut = :annulee where r.id = :id and r.statut = :confirmee")
    int annulerSiConfirmee(@Param("id") Long id, @Param("confirmee") StatutReservation confirmee,
                           @Param("annulee") StatutReservation annulee);

    @Query("""
            select coalesce(sum(r.nombrePlaces), 0)
            from Reservation r
            where r.seance.id = :seanceId and r.statut = :statut
            """)
    long sumPlacesBySeanceIdAndStatut(@Param("seanceId") Long seanceId,
                                      @Param("statut") StatutReservation statut);

    @Query("""
            select r.seance.id as seanceId, coalesce(sum(r.nombrePlaces), 0) as places
            from Reservation r
            where r.statut = :statut
            group by r.seance.id
            """)
    List<SeancePlacesProjection> sumPlacesParSeance(@Param("statut") StatutReservation statut);

    interface SeancePlacesProjection {
        Long getSeanceId();

        Long getPlaces();
    }
}
