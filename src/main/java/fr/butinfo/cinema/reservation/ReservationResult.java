package fr.butinfo.cinema.reservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResult(
        Long id,
        String code,
        Long seanceId,
        String filmTitre,
        String salleNom,
        LocalDateTime debut,
        String emailClient,
        int nombrePlaces,
        StatutReservation statut,
        BigDecimal montantTotal,
        LocalDateTime creeeLe
) {
}
