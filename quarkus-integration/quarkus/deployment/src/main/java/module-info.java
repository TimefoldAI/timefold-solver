module ai.timefold.solver.quarkus.deployment {

    exports ai.timefold.solver.quarkus.deployment;
    exports ai.timefold.solver.quarkus.deployment.config;
    exports ai.timefold.solver.quarkus.deployment.api;

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