package ai.timefold.solver.service.maps.service.integration.internal.model;

public class IllegalDistanceResponseException extends RuntimeException {

    private static final long serialVersionUID = 6469119449029210150L;
    private final String provider;

    public IllegalDistanceResponseException(String provider, String message) {
        super(message);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }

}
