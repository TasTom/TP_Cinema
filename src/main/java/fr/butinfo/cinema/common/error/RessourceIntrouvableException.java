package fr.butinfo.cinema.common.error;

public class RessourceIntrouvableException extends MetierException {

    public RessourceIntrouvableException(String code, String message) {
        super(code, message);
    }
}
