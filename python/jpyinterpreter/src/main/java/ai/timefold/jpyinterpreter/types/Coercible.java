package ai.timefold.jpyinterpreter.types;

public interface Coercible {
    <T> T coerce(Class<T> targetType);
}
