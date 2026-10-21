package fr.butinfo.cinema.reservation;

import fr.butinfo.cinema.common.error.CapaciteInsuffisanteException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import fr.butinfo.cinema.seance.Seance;
import fr.butinfo.cinema.seance.SeanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReservationService {

    private static final int PLACES_MIN = 1;
    private static final int PLACES_MAX = 8;
    private static final int EMAIL_MAX = 255;
    private static final int TENTATIVES_CODE = 5;

    private final ReservationRepository reservations;
    private final SeanceRepository seances;
    private final ReservationMetrics metrics;

    public ReservationService(ReservationRepository reservations, SeanceRepository seances,
                              ReservationMetrics metrics) {
        this.reservations = reservations;
        this.seances = seances;
        this.metrics = metrics;
    }

    @Transactional
    public ReservationResult creer(CreerReservationCommand command) {
        valider(command);
        String email = command.emailClient().trim();

        Seance seance = seances.findByIdForUpdate(command.seanceId())
                .orElseThrow(() -> new RessourceIntrouvableException("SEANCE_INTROUVABLE",
                        "La séance " + command.seanceId() + " n'existe pas."));

        long dejaReservees = reservations.sumPlacesBySeanceIdAndStatut(
                seance.getId(), StatutReservation.CONFIRMEE);
        int placesRestantes = Math.max(0, seance.getSalle().getCapacite() - (int) dejaReservees);
        if (command.nombrePlaces() > placesRestantes) {
            metrics.refusee(command.nombrePlaces());
            throw new CapaciteInsuffisanteException(placesRestantes);
        }

        BigDecimal montantTotal = seance.getPrixUnitaire()
                .multiply(BigDecimal.valueOf(command.nombrePlaces()))
                .setScale(2, RoundingMode.HALF_UP);
        Reservation reservation = new Reservation(
                genererCodeUnique(), seance, email, command.nombrePlaces(), montantTotal);
        Reservation enregistree = reservations.saveAndFlush(reservation);
        metrics.confirmee(command.nombrePlaces());
        return versResultat(enregistree);
    }

    @Transactional
    public void annuler(String code) {
        if (code == null || code.isBlank()) {
            throw new RessourceIntrouvableException("RESERVATION_INTROUVABLE",
                    "La réservation demandée n'existe pas.");
        }
        Reservation reservation = reservations.findByCode(code.trim())
                .orElseThrow(() -> new RessourceIntrouvableException("RESERVATION_INTROUVABLE",
                        "La réservation '" + code + "' n'existe pas."));
        seances.findByIdForUpdate(reservation.getSeance().getId())
                .orElseThrow(() -> new RessourceIntrouvableException("SEANCE_INTROUVABLE",
                        "La séance associée à la réservation n'existe plus."));
        if (reservation.getStatut() == StatutReservation.CONFIRMEE) {
            reservation.annuler();
            reservations.save(reservation);
            metrics.annulee(reservation.getNombrePlaces());
        }
    }

    public ReservationResult trouverParCode(String code) {
        Reservation reservation = reservations.findByCodeWithSeance(code)
                .orElseThrow(() -> new RessourceIntrouvableException("RESERVATION_INTROUVABLE",
                        "La réservation '" + code + "' n'existe pas."));
        return versResultat(reservation);
    }

    public List<ReservationResult> listerToutes() {
        return reservations.findAllByOrderByCreeeLeDesc().stream()
                .map(this::versResultat)
                .toList();
    }

    private void valider(CreerReservationCommand command) {
        if (command.seanceId() == null) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "La séance est obligatoire.");
        }
        if (command.emailClient() == null || command.emailClient().isBlank()) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "L'adresse e-mail est obligatoire.");
        }
        if (command.emailClient().trim().length() > EMAIL_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "L'adresse e-mail ne doit pas dépasser " + EMAIL_MAX + " caractères.");
        }
        if (command.nombrePlaces() < PLACES_MIN || command.nombrePlaces() > PLACES_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "Le nombre de places doit être compris entre " + PLACES_MIN + " et " + PLACES_MAX + ".");
        }
    }

    private String genererCodeUnique() {
        for (int tentative = 0; tentative < TENTATIVES_CODE; tentative++) {
            String code = "RES-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);
            if (!reservations.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Impossible de générer un code de réservation unique.");
    }

    private ReservationResult versResultat(Reservation reservation) {
        Seance seance = reservation.getSeance();
        return new ReservationResult(
                reservation.getId(),
                reservation.getCode(),
                seance.getId(),
                seance.getFilm().getTitre(),
                seance.getSalle().getNom(),
                seance.getDebut(),
                reservation.getEmailClient(),
                reservation.getNombrePlaces(),
                reservation.getStatut(),
                reservation.getMontantTotal(),
                reservation.getCreeeLe());
    }
}
