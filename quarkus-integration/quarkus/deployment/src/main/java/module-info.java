open module ai.timefold.solver.quarkus.deployment {
    requires ai.timefold.solver.quarkus;
    requires quarkus.arc.deployment;
    requires quarkus.builder;
    requires quarkus.core.deployment;
    requires quarkus.devui.deployment.spi;
    requires ai.timefold.solver.core;
    requires io.quarkus.gizmo2;
    requires jakarta.cdi;
    requires quarkus.core;
    requires arc.processor;
    requires org.jspecify;
    requires io.quarkus.gizmo;
    requires org.jboss.jandex;
    requires io.smallrye.config;
    requires org.eclipse.microprofile.config;
    requires org.objectweb.asm;
    requires org.jboss.logging;
}