package fr.butinfo.cinema.reservation;

import fr.butinfo.cinema.seance.Seance;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 32, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seance_id", nullable = false)
    private Seance seance;

    @Column(name = "email_client", nullable = false, length = 255)
    private String emailClient;

    @Column(name = "nombre_places", nullable = false)
    private int nombrePlaces;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutReservation statut;

    @Column(name = "montant_total", nullable = false, precision = 11, scale = 2)
    private BigDecimal montantTotal;

    @Column(name = "creee_le", nullable = false)
    private LocalDateTime creeeLe;

    protected Reservation() {
    }

    public Reservation(String code, Seance seance, String emailClient, int nombrePlaces, BigDecimal montantTotal) {
        this.code = Objects.requireNonNull(code, "code");
        this.seance = Objects.requireNonNull(seance, "seance");
        this.emailClient = Objects.requireNonNull(emailClient, "emailClient");
        this.nombrePlaces = nombrePlaces;
        this.montantTotal = Objects.requireNonNull(montantTotal, "montantTotal");
        this.statut = StatutReservation.CONFIRMEE;
        this.creeeLe = LocalDateTime.now(ZoneOffset.UTC);
    }

    @PrePersist
    void prePersist() {
        if (creeeLe == null) {
            creeeLe = LocalDateTime.now(ZoneOffset.UTC);
        }
        if (statut == null) {
            statut = StatutReservation.CONFIRMEE;
        }
    }

    public void annuler() {
        this.statut = StatutReservation.ANNULEE;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Seance getSeance() {
        return seance;
    }

    public String getEmailClient() {
        return emailClient;
    }

    public int getNombrePlaces() {
        return nombrePlaces;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public LocalDateTime getCreeeLe() {
        return creeeLe;
    }
}
