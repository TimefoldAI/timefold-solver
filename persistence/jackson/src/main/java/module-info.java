module ai.timefold.solver.jackson {

    exports ai.timefold.solver.jackson.api;

    provides com.fasterxml.jackson.databind.Module with
            ai.timefold.solver.jackson.api.TimefoldJacksonModule;

    requires transitive ai.timefold.solver.core;
    requires org.jspecify;
    requires com.fasterxml.jackson.databind;

    uses com.fasterxml.jackson.databind.Module;

}