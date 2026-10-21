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
}
