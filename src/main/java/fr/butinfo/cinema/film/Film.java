package fr.butinfo.cinema.film;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "films")
public class Film {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titre", nullable = false, length = 255)
    private String titre;

    @Column(name = "genre", length = 80)
    private String genre;

    @Column(name = "duree_minutes", nullable = false)
    private int dureeMinutes;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @Column(columnDefinition = "TEXT")
    private String synopsis;
    private String realisateur;
    private String affiche;
    private String panorama;
    @Column(name = "source_externe", length = 20)
    private String sourceExterne;
    @Column(name = "identifiant_externe", length = 100)
    private String identifiantExterne;
    @Column(name = "annee_sortie")
    private Integer anneeSortie;

    protected Film() {
    }

    public Film(String titre, String genre, int dureeMinutes, LocalDate dateSortie) {
        this.titre = Objects.requireNonNull(titre, "titre");
        this.genre = genre;
        this.dureeMinutes = dureeMinutes;
        this.dateSortie = dateSortie;
    }

    public Long getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getGenre() {
        return genre;
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public LocalDate getDateSortie() {
        return dateSortie;
    }

    public String getSynopsis() { return synopsis; }
    public String getRealisateur() { return realisateur; }
    public String getAffiche() { return affiche; }
    public String getPanorama() { return panorama; }
    public String getSourceExterne() { return sourceExterne; }
    public Integer getAnneeSortie() { return dateSortie != null ? dateSortie.getYear() : anneeSortie; }

    public void enrichir(String synopsis, String realisateur, String affiche, String panorama,
                         String source, String identifiant, Integer annee) {
        this.synopsis = synopsis;
        this.realisateur = realisateur;
        if (affiche != null) this.affiche = affiche;
        if (panorama != null) this.panorama = panorama;
        this.sourceExterne = source;
        this.identifiantExterne = identifiant;
        this.anneeSortie = annee;
    }
}
