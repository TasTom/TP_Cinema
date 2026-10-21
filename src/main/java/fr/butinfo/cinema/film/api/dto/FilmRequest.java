package fr.butinfo.cinema.film.api.dto;

import fr.butinfo.cinema.film.CreerFilmCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record FilmRequest(
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.")
        String titre,

        @Size(max = 80, message = "Le genre ne doit pas dépasser 80 caractères.")
        String genre,

        @NotNull(message = "La durée est obligatoire.")
        @Min(value = 40, message = "La durée doit être d'au moins 40 minutes.")
        @Max(value = 300, message = "La durée ne doit pas dépasser 300 minutes.")
        Integer dureeMinutes,

        LocalDate dateSortie
) {

    public CreerFilmCommand versCommande() {
        return new CreerFilmCommand(titre, genre, dureeMinutes, dateSortie);
    }
}
