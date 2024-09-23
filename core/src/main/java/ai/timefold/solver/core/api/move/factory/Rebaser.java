package ai.timefold.solver.core.api.move.factory;

@FunctionalInterface
public interface Rebaser {

    <T> T apply(T sourceObject);

}
