package fr.butinfo.cinema.common.error;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ErreurHttpController implements ErrorController {
    @RequestMapping("/error")
    public Object erreur(HttpServletRequest request, HttpServletResponse response, Model model) {
        Object valeur = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statut = valeur instanceof Integer nombre ? nombre : 500;
        HttpStatus http = HttpStatus.resolve(statut);
        if (http == null) http = HttpStatus.INTERNAL_SERVER_ERROR;
        String chemin = String.valueOf(request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
        if (chemin.startsWith("/api/") || chemin.startsWith("/actuator/")) {
            String code = switch (statut) {
                case 404 -> "RESSOURCE_INTROUVABLE";
                case 405 -> "METHODE_NON_AUTORISEE";
                case 415 -> "FORMAT_NON_PRIS_EN_CHARGE";
                case 400 -> "REQUETE_MAL_FORMEE";
                default -> "ERREUR_INTERNE";
            };
            return ResponseEntity.status(http).contentType(MediaType.APPLICATION_JSON)
                    .body(ApiError.of(http, code, "La requête ne peut pas être traitée.", chemin));
        }
        response.setStatus(http.value());
        model.addAttribute("status", http.value());
        model.addAttribute("path", chemin.equals("null") ? "" : chemin);
        return "error";
    }
}
