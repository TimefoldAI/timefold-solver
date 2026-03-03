module ai.timefold.solver.quarkus.jackson.deployment {

    exports ai.timefold.solver.quarkus.jackson.deployment;

    requires transitive ai.timefold.solver.quarkus.jackson;
    requires quarkus.core;
    requires quarkus.core.deployment;
    requires quarkus.jackson.spi;

}