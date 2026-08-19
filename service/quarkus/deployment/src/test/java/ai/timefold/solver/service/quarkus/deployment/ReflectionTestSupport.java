package ai.timefold.solver.service.quarkus.deployment;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Invokes private members of {@link TimefoldModelDescriptorProcessor} directly, so pure logic
 * (file processing, OpenAPI augmentation, JSON schema generation) can be unit tested without
 * spinning up a full Quarkus deployment.
 */
final class ReflectionTestSupport {

    private ReflectionTestSupport() {
        throw new UnsupportedOperationException();
    }

    static Object invoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method method = TimefoldModelDescriptorProcessor.class.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
