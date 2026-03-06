module ai.timefold.solver.quarkus.benchmark.deployment {

    // Friendly exports.
    exports ai.timefold.solver.benchmark.quarkus.deployment
            to ai.timefold.solver.enterprise.quarkus.deployment;

    requires ai.timefold.solver.quarkus.benchmark;
    requires ai.timefold.solver.quarkus.deployment;
    requires io.smallrye.config;
    requires org.jboss.logging;
    requires quarkus.arc.deployment;
    requires quarkus.builder;
    requires quarkus.core;
    requires quarkus.core.deployment;

}