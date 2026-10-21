package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.film.CreerFilmCommand;
import fr.butinfo.cinema.film.FilmService;
import fr.butinfo.cinema.reservation.ReservationService;
import fr.butinfo.cinema.salle.CreerSalleCommand;
import fr.butinfo.cinema.salle.SalleService;
import fr.butinfo.cinema.seance.CreerSeanceCommand;
import fr.butinfo.cinema.seance.SeanceService;
import fr.butinfo.cinema.ui.form.FilmForm;
import fr.butinfo.cinema.ui.form.SalleForm;
import fr.butinfo.cinema.ui.form.SeanceForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/admin")
public class AdminUiController {

    private final FilmService films;
    private final SalleService salles;
    private final SeanceService seances;
    private final ReservationService reservations;

    public AdminUiController(FilmService films, SalleService salles, SeanceService seances,
                             ReservationService reservations) {
        this.films = films;
        this.salles = salles;
        this.seances = seances;
        this.reservations = reservations;
    }

    @GetMapping
    public String tableauDeBord(Model model) {
        model.addAttribute("nbFilms", films.rechercher(null, null).size());
        model.addAttribute("nbSeances", seances.rechercher(null, null).size());
        model.addAttribute("nbSalles", salles.lister().size());
        model.addAttribute("nbReservations", reservations.listerToutes().size());
        return "admin/index";
    }

    @GetMapping("/films")
    public String gererFilms(Model model) {
        model.addAttribute("films", films.rechercher(null, null));
        if (!model.containsAttribute("filmForm")) {
            model.addAttribute("filmForm", new FilmForm());
        }
        return "admin/films";
    }

    @PostMapping("/films")
    public String creerFilm(@Valid @ModelAttribute("filmForm") FilmForm formulaire,
                            BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("films", films.rechercher(null, null));
            return "admin/films";
        }
        try {
            films.creer(new CreerFilmCommand(formulaire.getTitre(), formulaire.getGenre(),
                    formulaire.getDureeMinutes(), formulaire.getDateSortie()));
            redirect.addFlashAttribute("succes", "Le film a été créé.");
            return "redirect:/admin/films";
        } catch (DonneeInvalideException ex) {
            binding.reject("metier", ex.getMessage());
            model.addAttribute("films", films.rechercher(null, null));
            return "admin/films";
        }
    }

    @GetMapping("/seances")
    public String gererSeances(Model model) {
        model.addAttribute("seances", seances.rechercher(null, null));
        model.addAttribute("films", films.rechercher(null, null));
        model.addAttribute("salles", salles.lister());
        if (!model.containsAttribute("seanceForm")) {
            SeanceForm formulaire = new SeanceForm();
            formulaire.setDebut(LocalDateTime.now().plusDays(1)
                    .withHour(20).withMinute(0).withSecond(0).withNano(0));
            formulaire.setPrixUnitaire(new BigDecimal("9.90"));
            model.addAttribute("seanceForm", formulaire);
        }
        return "admin/seances";
    }

    @PostMapping("/seances")
    public String creerSeance(@Valid @ModelAttribute("seanceForm") SeanceForm formulaire,
                              BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            ajouterListesSeances(model);
            return "admin/seances";
        }
        try {
            seances.creer(new CreerSeanceCommand(formulaire.getFilmId(), formulaire.getSalleId(),
                    formulaire.getDebut(), formulaire.getPrixUnitaire()));
            redirect.addFlashAttribute("succes", "La séance a été créée.");
            return "redirect:/admin/seances";
        } catch (DonneeInvalideException ex) {
            binding.reject("metier", ex.getMessage());
            ajouterListesSeances(model);
            return "admin/seances";
        }
    }

    @GetMapping("/salles")
    public String gererSalles(Model model) {
        model.addAttribute("salles", salles.lister());
        if (!model.containsAttribute("salleForm")) {
            model.addAttribute("salleForm", new SalleForm());
        }
        return "admin/salles";
    }

    @PostMapping("/salles")
    public String creerSalle(@Valid @ModelAttribute("salleForm") SalleForm formulaire,
                             BindingResult binding, Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("salles", salles.lister());
            return "admin/salles";
        }
        try {
            salles.creer(new CreerSalleCommand(formulaire.getNom(), formulaire.getCapacite()));
            redirect.addFlashAttribute("succes", "La salle a été créée.");
            return "redirect:/admin/salles";
        } catch (DonneeInvalideException | ConflitMetierException ex) {
            binding.reject("metier", ex.getMessage());
            model.addAttribute("salles", salles.lister());
            return "admin/salles";
        }
    }

    @GetMapping("/reservations")
    public String listerReservations(Model model) {
        model.addAttribute("reservations", reservations.listerToutes());
        return "admin/reservations";
    }

    private void ajouterListesSeances(Model model) {
        model.addAttribute("seances", seances.rechercher(null, null));
        model.addAttribute("films", films.rechercher(null, null));
        model.addAttribute("salles", salles.lister());
    }
}
