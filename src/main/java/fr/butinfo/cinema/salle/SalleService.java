package fr.butinfo.cinema.salle;

import fr.butinfo.cinema.common.error.ConflitMetierException;
import fr.butinfo.cinema.common.error.DonneeInvalideException;
import fr.butinfo.cinema.common.error.RessourceIntrouvableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SalleService {

    private static final int CAPACITE_MIN = 20;
    private static final int CAPACITE_MAX = 500;
    private static final int NOM_MAX = 100;

    private final SalleRepository salles;

    public SalleService(SalleRepository salles) {
        this.salles = salles;
    }

    public List<Salle> lister() {
        return salles.findAllByOrderByNomAsc();
    }

    public Salle trouver(Long id) {
        if (id == null) {
            throw new RessourceIntrouvableException("SALLE_INTROUVABLE", "La salle demandée n'existe pas.");
        }
        return salles.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("SALLE_INTROUVABLE",
                        "La salle " + id + " n'existe pas."));
    }

    @Transactional
    public Salle creer(CreerSalleCommand command) {
        String nom = command.nom() == null ? null : command.nom().trim();
        if (nom == null || nom.isEmpty()) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE", "Le nom de la salle est obligatoire.");
        }
        if (nom.length() > NOM_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "Le nom de la salle ne doit pas dépasser " + NOM_MAX + " caractères.");
        }
        if (command.capacite() < CAPACITE_MIN || command.capacite() > CAPACITE_MAX) {
            throw new DonneeInvalideException("VALIDATION_ECHOUEE",
                    "La capacité doit être comprise entre " + CAPACITE_MIN + " et " + CAPACITE_MAX + " places.");
        }
        if (salles.existsByNomIgnoreCase(nom)) {
            throw new ConflitMetierException("SALLE_DEJA_EXISTANTE",
                    "Une salle nommée '" + nom + "' existe déjà.");
        }
        try {
            return salles.saveAndFlush(new Salle(nom, command.capacite()));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflitMetierException("SALLE_DEJA_EXISTANTE",
                    "Une salle nommée '" + nom + "' existe déjà.");
        }
    }
}
