package fr.butinfo.cinema.film.api;

import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.film.api.dto.FilmResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/films")
@Tag(name = "Films", description = "Catalogue public des films")
public class FilmController {

    private final FilmService films;

    public FilmController(FilmService films) {
        this.films = films;
    }

    @GetMapping
    @Operation(summary = "Lister les films", description = "Liste filtrable par titre et/ou genre.")
    public List<FilmResponse> lister(
            @Parameter(description = "Filtre partiel et insensible à la casse sur le titre")
            @RequestParam(required = false) String titre,
            @Parameter(description = "Filtre exact et insensible à la casse sur le genre")
            @RequestParam(required = false) String genre) {
        return films.rechercher(titre, genre).stream()
                .map(FilmResponse::depuis)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un film")
    public FilmResponse trouver(@PathVariable Long id) {
        return FilmResponse.depuis(films.trouver(id));
    }
}
