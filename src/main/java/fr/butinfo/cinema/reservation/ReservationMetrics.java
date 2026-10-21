package fr.butinfo.cinema.reservation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ReservationMetrics {

    private final Counter confirmees;
    private final Counter annulees;
    private final Counter refusees;
    private final DistributionSummary placesReservees;
    private final DistributionSummary placesRefusees;

    public ReservationMetrics(MeterRegistry registry) {
        this.confirmees = Counter.builder("cinema.reservations")
                .description("Nombre de réservations confirmées")
                .tag("resultat", "confirmee")
                .register(registry);
        this.annulees = Counter.builder("cinema.reservations")
                .description("Nombre de réservations annulées")
                .tag("resultat", "annulee")
                .register(registry);
        this.refusees = Counter.builder("cinema.reservations")
                .description("Nombre de réservations refusées faute de capacité")
                .tag("resultat", "refusee")
                .register(registry);
        this.placesReservees = DistributionSummary.builder("cinema.places")
                .description("Nombre de places réservées par opération")
                .baseUnit("places")
                .tag("resultat", "reservee")
                .register(registry);
        this.placesRefusees = DistributionSummary.builder("cinema.places")
                .description("Nombre de places demandées lors d'un refus")
                .baseUnit("places")
                .tag("resultat", "refusee")
                .register(registry);
    }

    public void confirmee(int nombrePlaces) {
        confirmees.increment();
        placesReservees.record(nombrePlaces);
    }

    public void annulee(int nombrePlaces) {
        annulees.increment();
    }

    public void refusee(int nombrePlaces) {
        refusees.increment();
        placesRefusees.record(nombrePlaces);
    }
}
