package fr.butinfo.cinema.ui;

import fr.butinfo.cinema.catalogue.CatalogueExterneService;
import fr.butinfo.cinema.catalogue.ImportFilmService;
import fr.butinfo.cinema.common.error.MetierException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
public class CatalogueImportController {
    private final CatalogueExterneService catalogue;
    private final ImportFilmService imports;
    public CatalogueImportController(CatalogueExterneService catalogue, ImportFilmService imports) {
        this.catalogue = catalogue;
        this.imports = imports;
    }

    @GetMapping("/admin/catalogue")
    public String catalogue(@RequestParam(defaultValue = "GHIBLI") String source,
                             @RequestParam(defaultValue = "") String q, Model model) {
        model.addAttribute("source", source);
        model.addAttribute("q", q);
        model.addAttribute("tmdbConfigure", catalogue.tmdbConfigure());
        try { model.addAttribute("filmsExternes", catalogue.rechercher(source, q)); }
        catch (MetierException ex) {
            model.addAttribute("filmsExternes", List.of());
            model.addAttribute("erreur", ex.getMessage());
        }
        return "admin/catalogue";
    }

    @PostMapping("/admin/catalogue/importer")
    public String importer(@RequestParam String source, @RequestParam String id, RedirectAttributes redirect) {
        Long film = imports.importer(source, id);
        redirect.addFlashAttribute("succes", "Le film et ses informations ont été ajoutés au catalogue. Vous pouvez maintenant programmer une séance.");
        return "redirect:/films/" + film;
    }
}
