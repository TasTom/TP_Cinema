package fr.butinfo.cinema.ui;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(basePackages = "fr.butinfo.cinema.ui")
public class GlobalModelAttributes {

    @ModelAttribute("currentUser")
    public String currentUser(Authentication authentication) {
        return estConnecte(authentication) ? authentication.getName() : null;
    }

    @ModelAttribute("estAdmin")
    public boolean estAdmin(Authentication authentication) {
        return possedeRole(authentication, "ADMIN");
    }

    @ModelAttribute("estProgrammateur")
    public boolean estProgrammateur(Authentication authentication) {
        return possedeRole(authentication, "PROGRAMMATEUR");
    }

    @ModelAttribute("estClient")
    public boolean estClient(Authentication authentication) {
        return possedeRole(authentication, "CLIENT");
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
