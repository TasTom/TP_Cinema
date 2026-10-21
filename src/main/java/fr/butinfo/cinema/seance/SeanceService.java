package fr.butinfo.cinema.seance;

import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.film.FilmRepository;
import fr.butinfo.cinema.reservation.ReservationRepository;
import fr.butinfo.cinema.reservation.StatutReservation;
import fr.butinfo.cinema.salle.Salle;
import fr.butinfo.cinema.salle.SalleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class SeanceService {

    private static final BigDecimal PRIX_MAX = new BigDecimal("99999999.99");

    private final SeanceRepository seances;
    private final FilmRepository films;
    private final SalleRepository salles;
    private final ReservationRepository reservations;

    public SeanceService(SeanceRepository seances, FilmRepository films, SalleRepository salles,
                         ReservationRepository reservations) {
        this.seances = seances;
        this.films = films;
        this.salles = salles;
        this.reservations = reservations;
    }

    public List<SeanceResult> rechercher(LocalDate du, LocalDate au) {
        validerDate(du);
        validerDate(au);
        if (du != null && au != null && du.isAfter(au)) {
            throw new DonneeInvalideException("INTERVALLE_INVALIDE",
                    "La date de début doit être antérieure ou égale à la date de fin.");
        }
        LocalDateTime debut = du == null ? null : du.atStartOfDay();
        LocalDateTime fin = au == null ? null : au.plusDays(1).atStartOfDay();
        List<Seance> liste = seances.rechercher(debut, fin);
        Map<Long, Long> placesParSeance = reservations.sumPlacesParSeance(StatutReservation.CONFIRMEE)
                .stream()
                .collect(Collectors.toMap(
                        ReservationRepository.SeancePlacesProjection::getSeanceId,
                        projection -> projection.getPlaces() == null ? 0L : projection.getPlaces()));
        return liste.stream()
                .map(seance -> versResultat(seance, placesParSeance.getOrDefault(seance.getId(), 0L)))
                .toList();
    }

    public SeanceResult trouver(Long id) {
        Seance seance = seances.findAvecFilmEtSalle(id)
                .orElseThrow(() -> new RessourceIntrouvableException("SEANCE_INTROUVABLE",
                        "La séance " + id + " n'existe pas."));
        long places = reservations.sumPlacesBySeanceIdAndStatut(seance.getId(), StatutReservation.CONFIRMEE);
        return versResultat(seance, places);
    }

    @Transactional
    public SeanceResult creer(CreerSeanceCommand command) {
        if (command.debut() == null) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "La date et l'heure de la séance sont obligatoires.");
        }
        validerDate(command.debut().toLocalDate());
        BigDecimal prix = command.prixUnitaire();
        if (prix == null || prix.signum() <= 0) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "Le prix unitaire doit être strictement positif.");
        }
        if (prix.scale() > 2) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "Le prix doit avoir au maximum deux décimales.");
        }
        if (prix.compareTo(PRIX_MAX) > 0) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "Le prix unitaire est trop élevé.");
        }
        Film film = films.findById(command.filmId())
                .orElseThrow(() -> new DonneeInvalideException("FILM_INTROUVABLE",
                        "Le film " + command.filmId() + " n'existe pas."));
        Salle salle = salles.findById(command.salleId())
                .orElseThrow(() -> new DonneeInvalideException("SALLE_INTROUVABLE",
                        "La salle " + command.salleId() + " n'existe pas."));
        Seance seance = seances.save(new Seance(film, salle, command.debut(), prix));
        return versResultat(seance, 0L);
    }

    private SeanceResult versResultat(Seance seance, long placesReservees) {
        Salle salle = seance.getSalle();
        Film film = seance.getFilm();
        int placesRestantes = Math.max(0, salle.getCapacite() - (int) placesReservees);
        return new SeanceResult(
                seance.getId(),
                film.getId(),
                film.getTitre(),
                salle.getId(),
                salle.getNom(),
                seance.getDebut(),
                seance.getPrixUnitaire(),
                salle.getCapacite(),
                placesRestantes);
    }

    private void validerDate(LocalDate date) {
        if (date != null && (date.getYear() < 1000 || date.isAfter(LocalDate.of(9999, 12, 30)))) {
            throw new DonneeInvalideException("DATE_INVALIDE", "La date doit être comprise entre 1000 et le 30 décembre 9999.");
        }
    }
}
