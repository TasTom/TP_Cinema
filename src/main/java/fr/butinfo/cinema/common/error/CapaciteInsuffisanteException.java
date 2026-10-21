package fr.butinfo.cinema.common.error;

public class CapaciteInsuffisanteException extends MetierException {

    private final int placesRestantes;

    public CapaciteInsuffisanteException(int placesRestantes) {
        super("CAPACITE_INSUFFISANTE", message(placesRestantes));
        this.placesRestantes = placesRestantes;
    }

    public int getPlacesRestantes() {
        return placesRestantes;
    }

    private static String message(int placesRestantes) {
        if (placesRestantes <= 0) {
            return "La séance est complète.";
        }
        if (placesRestantes == 1) {
            return "Il ne reste qu'une place pour cette séance.";
        }
        return "Il ne reste que " + placesRestantes + " places pour cette séance.";
    }
}
