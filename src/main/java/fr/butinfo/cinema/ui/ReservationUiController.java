package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.common.error.CapaciteInsuffisanteException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import fr.butinfo.cinema.reservation.CreerReservationCommand;
import fr.butinfo.cinema.reservation.ReservationResult;
import fr.butinfo.cinema.reservation.ReservationService;
import fr.butinfo.cinema.seance.SeanceService;
import fr.butinfo.cinema.ui.form.ReservationForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservation")
public class ReservationUiController {

    private final SeanceService seances;
    private final ReservationService reservations;

    public ReservationUiController(SeanceService seances, ReservationService reservations) {
        this.seances = seances;
        this.reservations = reservations;
    }

    @GetMapping("/{seanceId}")
    public String formulaire(@PathVariable Long seanceId, Authentication authentication, Model model) {
        model.addAttribute("seance", seances.trouver(seanceId));
        if (!model.containsAttribute("formulaire")) {
            model.addAttribute("formulaire",
                    new ReservationForm(seanceId, emailParDefaut(authentication), 1));
        }
        return "reservation/form";
    }

    @PostMapping
    public String reserver(@Valid @ModelAttribute("formulaire") ReservationForm formulaire,
                           BindingResult binding, Model model, RedirectAttributes redirect) {
        if (formulaire.getSeanceId() == null) {
            redirect.addFlashAttribute("erreur", "La séance est introuvable.");
            return "redirect:/seances";
        }
        if (binding.hasErrors()) {
            model.addAttribute("seance", seances.trouver(formulaire.getSeanceId()));
            return "reservation/form";
        }
        try {
            ReservationResult resultat = reservations.creer(new CreerReservationCommand(
                    formulaire.getSeanceId(), formulaire.getEmailClient(), formulaire.getNombrePlaces()));
            redirect.addFlashAttribute("reservation", resultat);
            return "redirect:/reservation/confirmation";
        } catch (CapaciteInsuffisanteException ex) {
            binding.rejectValue("nombrePlaces", "capacite", ex.getMessage());
            model.addAttribute("seance", seances.trouver(formulaire.getSeanceId()));
            return "reservation/form";
        } catch (DonneeInvalideException ex) {
            binding.reject("metier", ex.getMessage());
            model.addAttribute("seance", seances.trouver(formulaire.getSeanceId()));
            return "reservation/form";
        } catch (RessourceIntrouvableException ex) {
            redirect.addFlashAttribute("erreur", ex.getMessage());
            return "redirect:/seances";
        }
    }

    @GetMapping("/confirmation")
    public String confirmation(Model model) {
        if (!model.containsAttribute("reservation")) {
            return "redirect:/";
        }
        return "reservation/confirmation";
    }

    private String emailParDefaut(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        return switch (authentication.getName()) {
            case "client" -> "client@example.test";
            case "admin" -> "admin@example.test";
            case "programmateur" -> "programmateur@example.test";
            default -> authentication.getName();
        };
    }
}
