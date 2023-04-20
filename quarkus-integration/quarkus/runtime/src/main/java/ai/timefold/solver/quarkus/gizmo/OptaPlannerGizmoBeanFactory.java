package ai.timefold.solver.quarkus.gizmo;

public interface OptaPlannerGizmoBeanFactory {
    <T> T newInstance(Class<T> clazz);
}
