module ai.timefold.solver.quarkus.jackson {

    exports ai.timefold.solver.quarkus.jackson;
    exports ai.timefold.solver.quarkus.jackson.solution;

    requires transitive ai.timefold.solver.core;
    requires com.fasterxml.jackson.databind;
    requires org.jspecify;

}