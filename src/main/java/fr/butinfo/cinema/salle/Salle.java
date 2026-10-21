package fr.butinfo.cinema.salle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "salles")
public class Salle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom", nullable = false, length = 100, unique = true)
    private String nom;

    @Column(name = "capacite", nullable = false)
    private int capacite;

    protected Salle() {
    }

    public Salle(String nom, int capacite) {
        this.nom = Objects.requireNonNull(nom, "nom");
        this.capacite = capacite;
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public int getCapacite() {
        return capacite;
    }
}
