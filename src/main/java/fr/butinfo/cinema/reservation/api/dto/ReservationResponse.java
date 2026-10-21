package fr.butinfo.cinema.reservation.api.dto;

import fr.butinfo.cinema.reservation.ReservationResult;
import fr.butinfo.cinema.reservation.StatutReservation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationResponse(
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

    public static ReservationResponse depuis(ReservationResult resultat) {
        return new ReservationResponse(resultat.id(), resultat.code(), resultat.seanceId(),
                resultat.filmTitre(), resultat.salleNom(), resultat.debut(), resultat.emailClient(),
                resultat.nombrePlaces(), resultat.statut(), resultat.montantTotal(), resultat.creeeLe());
    }
}
