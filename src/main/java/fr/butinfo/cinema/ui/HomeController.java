package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.seance.SeanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import fr.butinfo.cinema.film.Film;

@Controller
public class HomeController {

    private final FilmService films;
    private final SeanceService seances;

    public HomeController(FilmService films, SeanceService seances) {
        this.films = films;
        this.seances = seances;
    }

    @GetMapping("/")
    public String accueil(Model model) {
        LocalDate aujourdHui = LocalDate.now();
        List<Film> catalogue = films.rechercher(null, null);
        var planning = seances.rechercher(aujourdHui, aujourdHui.plusDays(60));
        Film vedette = catalogue.stream().filter(f -> f.getPanorama() != null
                && planning.stream().anyMatch(s -> s.filmId().equals(f.getId()))).findFirst()
                .orElseGet(() -> catalogue.stream().filter(f -> f.getPanorama() != null).findFirst()
                        .orElse(catalogue.isEmpty() ? null : catalogue.getFirst()));
        model.addAttribute("filmVedette", vedette);
        model.addAttribute("films", catalogue.stream().sorted(java.util.Comparator.comparing(f -> f.getAffiche() == null)).limit(8).toList());
        model.addAttribute("nombreFilms", catalogue.size());
        model.addAttribute("seances", planning.stream().limit(6).toList());
        return "index";
    }
}
