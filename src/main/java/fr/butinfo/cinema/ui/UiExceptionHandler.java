package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.common.error.CapaciteInsuffisanteException;
import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.MetierException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.ui.Model;

@ControllerAdvice(basePackages = "fr.butinfo.cinema.ui")
public class UiExceptionHandler {

    @ExceptionHandler({DonneeInvalideException.class, RessourceIntrouvableException.class,
            ConflitMetierException.class, CapaciteInsuffisanteException.class})
    public String erreurMetier(MetierException exception, Model model, HttpServletResponse response) {
        HttpStatus statut = statut(exception);
        response.setStatus(statut.value());
        model.addAttribute("statut", statut.value());
        model.addAttribute("titre", "Opération impossible");
        model.addAttribute("code", exception.getCode());
        model.addAttribute("message", exception.getMessage());
        return "erreur-metier";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String parametreInvalide(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        model.addAttribute("statut", HttpStatus.BAD_REQUEST.value());
        model.addAttribute("titre", "Paramètre invalide");
        model.addAttribute("code", "PARAMETRE_INVALIDE");
        model.addAttribute("message", "Le paramètre fourni n'est pas valide.");
        return "erreur-metier";
    }

    private HttpStatus statut(MetierException exception) {
        if (exception instanceof RessourceIntrouvableException) {
            return HttpStatus.NOT_FOUND;
        }
        if (exception instanceof DonneeInvalideException) {
            return HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.CONFLICT;
    }
}
