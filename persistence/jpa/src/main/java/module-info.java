module ai.timefold.solver.jpa {

    exports ai.timefold.solver.jpa.api.score;

    requires transitive ai.timefold.solver.core;
    requires jakarta.persistence;

}