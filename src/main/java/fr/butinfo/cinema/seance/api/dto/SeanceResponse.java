package fr.butinfo.cinema.seance.api.dto;

import fr.butinfo.cinema.seance.SeanceResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceResponse(
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

    public static SeanceResponse depuis(SeanceResult resultat) {
        return new SeanceResponse(resultat.id(), resultat.filmId(), resultat.filmTitre(),
                resultat.salleId(), resultat.salleNom(), resultat.debut(), resultat.prixUnitaire(),
                resultat.capaciteSalle(), resultat.placesRestantes());
    }
}
