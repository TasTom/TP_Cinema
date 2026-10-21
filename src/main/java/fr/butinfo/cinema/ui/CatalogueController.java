package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.seance.SeanceResult;
import fr.butinfo.cinema.seance.SeanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class CatalogueController {

    private final FilmService films;
    private final SeanceService seances;

    public CatalogueController(FilmService films, SeanceService seances) {
        this.films = films;
        this.seances = seances;
    }

    @GetMapping("/films")
    public String listerFilms(@RequestParam(required = false) String titre,
                              @RequestParam(required = false) String genre,
                              Model model) {
        model.addAttribute("films", films.rechercher(titre, genre));
        model.addAttribute("titre", titre);
        model.addAttribute("genre", genre);
        return "films/index";
    }

    @GetMapping("/seances")
    public String listerSeances(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au,
            @RequestParam(required = false) Long filmId,
            Model model) {
        LocalDate duEffectif = du;
        if (du == null && au == null) {
            duEffectif = LocalDate.now();
        }
        List<SeanceResult> liste = seances.rechercher(duEffectif, au);
        if (filmId != null) {
            Film film = films.trouver(filmId);
            liste = liste.stream().filter(seance -> filmId.equals(seance.filmId())).toList();
            model.addAttribute("film", film);
        }
        model.addAttribute("seances", liste);
        model.addAttribute("du", duEffectif);
        model.addAttribute("au", au);
        model.addAttribute("filmId", filmId);
        return "seances/index";
    }
}
