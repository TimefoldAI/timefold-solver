open module ai.timefold.solver.quarkus.jackson {
    exports ai.timefold.solver.quarkus.jackson;

    requires ai.timefold.solver.core;
    requires ai.timefold.solver.persistence.common;
    requires com.fasterxml.jackson.databind;
    requires org.jspecify;
}