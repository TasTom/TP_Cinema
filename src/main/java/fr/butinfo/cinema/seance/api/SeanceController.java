package fr.butinfo.cinema.seance.api;

import fr.butinfo.cinema.seance.SeanceService;
import fr.butinfo.cinema.seance.api.dto.SeanceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/seances")
@Tag(name = "Séances", description = "Planning public des séances")
public class SeanceController {

    private final SeanceService seances;

    public SeanceController(SeanceService seances) {
        this.seances = seances;
    }

    @GetMapping
    @Operation(summary = "Lister les séances",
            description = "Filtre optionnel par intervalle de dates, bornes incluses (format ISO yyyy-MM-dd).")
    public List<SeanceResponse> lister(
            @Parameter(description = "Date de début incluse (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate du,
            @Parameter(description = "Date de fin incluse (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate au) {
        return seances.rechercher(du, au).stream()
                .map(SeanceResponse::depuis)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter une séance")
    public SeanceResponse trouver(@PathVariable Long id) {
        return SeanceResponse.depuis(seances.trouver(id));
    }
}
