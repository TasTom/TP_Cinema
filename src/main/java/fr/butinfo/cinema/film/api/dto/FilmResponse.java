package fr.butinfo.cinema.film.api.dto;

import fr.butinfo.cinema.film.Film;

import java.time.LocalDate;

public record FilmResponse(
        Long id,
        String titre,
        String genre,
        int dureeMinutes,
        LocalDate dateSortie
) {

    public static FilmResponse depuis(Film film) {
        return new FilmResponse(film.getId(), film.getTitre(), film.getGenre(),
                film.getDureeMinutes(), film.getDateSortie());
    }
}
