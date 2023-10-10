package ai.timefold.solver.core.impl.io.jaxb;

public class TimefoldXmlSerializationException extends RuntimeException {

    public TimefoldXmlSerializationException() {
        super();
    }

    public TimefoldXmlSerializationException(String message) {
        super(message);
    }

    public TimefoldXmlSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimefoldXmlSerializationException(Throwable cause) {
        super(cause);
    }
}
