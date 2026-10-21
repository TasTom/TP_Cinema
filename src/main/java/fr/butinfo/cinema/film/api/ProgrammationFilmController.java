package fr.butinfo.cinema.film.api;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.film.api.dto.FilmRequest;
import fr.butinfo.cinema.film.api.dto.FilmResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/programmation/films")
@Tag(name = "Programmation des films", description = "Création de films réservée aux rôles PROGRAMMATEUR et ADMIN")
@SecurityRequirement(name = "basicAuth")
public class ProgrammationFilmController {

    private final FilmService films;

    public ProgrammationFilmController(FilmService films) {
        this.films = films;
    }

    @PostMapping
    @Operation(summary = "Créer un film", description = "Réservé aux rôles PROGRAMMATEUR et ADMIN.")
    public ResponseEntity<FilmResponse> creer(@Valid @RequestBody FilmRequest request) {
        Film film = films.creer(request.versCommande());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/films/{id}")
                .buildAndExpand(film.getId())
                .toUri();
        return ResponseEntity.created(location).body(FilmResponse.depuis(film));
    }
}
