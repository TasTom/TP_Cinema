package fr.butinfo.cinema.seance;

import fr.butinfo.cinema.film.Film;
import fr.butinfo.cinema.salle.Salle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "seances")
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "film_id", nullable = false)
    private Film film;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "salle_id", nullable = false)
    private Salle salle;

    @Column(name = "debut", nullable = false)
    private LocalDateTime debut;

    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    protected Seance() {
    }

    public Seance(Film film, Salle salle, LocalDateTime debut, BigDecimal prixUnitaire) {
        this.film = Objects.requireNonNull(film, "film");
        this.salle = Objects.requireNonNull(salle, "salle");
        this.debut = Objects.requireNonNull(debut, "debut");
        this.prixUnitaire = Objects.requireNonNull(prixUnitaire, "prixUnitaire");
    }

    public Long getId() {
        return id;
    }

    public Film getFilm() {
        return film;
    }

    public Salle getSalle() {
        return salle;
    }

    public LocalDateTime getDebut() {
        return debut;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }
}
