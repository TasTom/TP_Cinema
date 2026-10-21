package fr.butinfo.cinema.ui;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice(basePackages = "fr.butinfo.cinema.ui")
public class GlobalModelAttributes {

    @ModelAttribute("cheminCourant")
    public String cheminCourant(HttpServletRequest request) { return request.getRequestURI(); }

    @ModelAttribute
    public void ajouterAuModele(Authentication authentication, Model model) {
        model.addAttribute("currentUser", estConnecte(authentication) ? authentication.getName() : null);
        model.addAttribute("estAdmin", possedeRole(authentication, "ADMIN"));
        model.addAttribute("estProgrammateur", possedeRole(authentication, "PROGRAMMATEUR"));
        model.addAttribute("estClient", possedeRole(authentication, "CLIENT"));
    }

    private boolean estConnecte(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private boolean possedeRole(Authentication authentication, String role) {
        return estConnecte(authentication)
                && authentication.getAuthorities().stream()
                .anyMatch(authorite -> authorite.getAuthority().equals("ROLE_" + role));
    }
}
