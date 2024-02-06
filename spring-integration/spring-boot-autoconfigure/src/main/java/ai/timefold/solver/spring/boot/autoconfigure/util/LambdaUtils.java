package ai.timefold.solver.spring.boot.autoconfigure.util;

import java.util.function.Function;

public class LambdaUtils {

    public static <T, R> Function<T, R> rethrowFunction(ThrowingFunction<T, R> throwingFunction) {
        return v -> {
            try {
                return throwingFunction.apply(v);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    private LambdaUtils() {
        // No external instances.
    }
}
