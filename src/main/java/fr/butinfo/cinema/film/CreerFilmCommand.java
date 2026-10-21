package fr.butinfo.cinema.film;

import java.time.LocalDate;

public record CreerFilmCommand(
        String titre,
        String genre,
        int dureeMinutes,
        LocalDate dateSortie
) {
}
