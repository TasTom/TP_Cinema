package fr.butinfo.cinema.seance.api.dto;

import fr.butinfo.cinema.seance.CreerSeanceCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeanceRequest(
        @NotNull(message = "Le film est obligatoire.")
        Long filmId,

        @NotNull(message = "La salle est obligatoire.")
        Long salleId,

        @NotNull(message = "La date et l'heure de la séance sont obligatoires.")
        LocalDateTime debut,

        @NotNull(message = "Le prix unitaire est obligatoire.")
        @DecimalMin(value = "0.01", message = "Le prix unitaire doit être strictement positif.")
        @Digits(integer = 8, fraction = 2, message = "Le prix unitaire doit comporter au maximum 8 chiffres entiers et 2 décimales.")
        BigDecimal prixUnitaire
) {

    public CreerSeanceCommand versCommande() {
        return new CreerSeanceCommand(filmId, salleId, debut, prixUnitaire);
    }
}
