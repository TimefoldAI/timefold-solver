module ai.timefold.solver.quarkus.deployment {

    // Friendly exports.
    exports ai.timefold.solver.quarkus.deployment
            to ai.timefold.solver.enterprise.quarkus.deployment;
    exports ai.timefold.solver.quarkus.deployment.config
            to ai.timefold.solver.enterprise.quarkus.deployment;
    exports ai.timefold.solver.quarkus.deployment.api
            to ai.timefold.solver.enterprise.quarkus.deployment,
            ai.timefold.solver.quarkus.benchmark.deployment,
            ai.timefold.sdk.quarkus.deployment;

    requires transitive ai.timefold.solver.quarkus;
    requires arc.processor;
    requires io.quarkus.gizmo;
    requires io.quarkus.gizmo2;
    requires io.smallrye.config;
    requires jakarta.cdi;
    requires org.jboss.jandex;
    requires org.jboss.logging;
    requires org.jspecify;
    requires org.objectweb.asm;
    requires quarkus.arc.deployment;
    requires quarkus.builder;
    requires quarkus.core;
    requires quarkus.core.deployment;
    requires quarkus.devui.deployment.spi;

}