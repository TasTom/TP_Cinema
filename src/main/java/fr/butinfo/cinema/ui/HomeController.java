package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.seance.SeanceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;

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
        model.addAttribute("films", films.rechercher(null, null).stream().limit(3).toList());
        model.addAttribute("seances", seances.rechercher(aujourdHui, aujourdHui.plusDays(60))
                .stream()
                .limit(6)
                .toList());
        return "index";
    }
}
