package ai.timefold.solver.model.maps.service.integration.internal.model;

public class TravelTimeAndDistanceConverterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public TravelTimeAndDistanceConverterException(String code, String message) {
        super(message);
        this.code = code;
    }

    public TravelTimeAndDistanceConverterException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
