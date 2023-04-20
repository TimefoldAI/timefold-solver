package ai.timefold.solver.quarkus.nativeimage;

import java.util.function.Supplier;

import javax.enterprise.inject.spi.CDI;

import com.oracle.svm.core.annotate.Substitute;

import ai.timefold.solver.quarkus.gizmo.TimefoldGizmoBeanFactory;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "ai.timefold.solver.core.config.util.ConfigUtils")
public final class Substitute_ConfigUtils {

    @Substitute
    public static <T> T newInstance(Supplier<String> ownerDescriptor, String propertyName, Class<T> clazz) {
        T out = CDI.current().getBeanManager().createInstance().select(TimefoldGizmoBeanFactory.class)
                .get().newInstance(clazz);
        if (out != null) {
            return out;
        } else {
            throw new IllegalArgumentException("Impossible state: could not find the " + ownerDescriptor.get() +
                    "'s " + propertyName + " (" + clazz.getName() + ") generated Gizmo supplier.");
        }
    }
}
