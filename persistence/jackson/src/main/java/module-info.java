module ai.timefold.solver.jackson {
    exports ai.timefold.solver.jackson.api;

    provides tools.jackson.databind.JacksonModule with
            ai.timefold.solver.jackson.api.TimefoldJacksonModule;

    requires ai.timefold.solver.core;
    requires org.jspecify;
    requires tools.jackson.databind;

    uses tools.jackson.databind.JacksonModule;
}