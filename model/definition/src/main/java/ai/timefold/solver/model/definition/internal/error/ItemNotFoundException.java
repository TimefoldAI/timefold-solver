package ai.timefold.solver.model.definition.internal.error;

public class ItemNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public ItemNotFoundException(String code, String message) {
        super(message);

        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
