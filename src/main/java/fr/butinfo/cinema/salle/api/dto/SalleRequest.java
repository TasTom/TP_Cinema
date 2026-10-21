package fr.butinfo.cinema.salle.api.dto;

import fr.butinfo.cinema.salle.CreerSalleCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SalleRequest(
        @NotBlank(message = "Le nom de la salle est obligatoire.")
        @Size(max = 100, message = "Le nom de la salle ne doit pas dépasser 100 caractères.")
        String nom,

        @NotNull(message = "La capacité est obligatoire.")
        @Min(value = 20, message = "La capacité doit être d'au moins 20 places.")
        @Max(value = 500, message = "La capacité ne doit pas dépasser 500 places.")
        Integer capacite
) {

    public CreerSalleCommand versCommande() {
        return new CreerSalleCommand(nom, capacite);
    }
}
