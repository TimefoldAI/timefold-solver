module ai.timefold.solver.jpa {
    requires jakarta.persistence;
    requires ai.timefold.solver.core;

    exports ai.timefold.solver.jpa.api.score;
}