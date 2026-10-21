package fr.butinfo.cinema.common.error;

public abstract class MetierException extends RuntimeException {

    private final String code;

    protected MetierException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
