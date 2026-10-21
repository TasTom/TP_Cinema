package fr.butinfo.cinema.seance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceResult(
        Long id,
        Long filmId,
        String filmTitre,
        Long salleId,
        String salleNom,
        LocalDateTime debut,
        BigDecimal prixUnitaire,
        int capaciteSalle,
        int placesRestantes
) {
}
