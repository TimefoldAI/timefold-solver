package ai.timefold.solver.quarkus.gizmo;

public interface TimefoldGizmoBeanFactory {
    <T> T newInstance(Class<T> clazz);
}
