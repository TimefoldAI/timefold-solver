open module timefold.solver.quarkus.deployment {
    requires timefold.solver.quarkus;
    requires quarkus.arc.deployment;
    requires quarkus.builder;
    requires quarkus.core.deployment;
    requires quarkus.devui.deployment.spi;
    requires ai.timefold.solver.core;
    requires io.quarkus.gizmo2;
    requires jakarta.cdi;
    requires quarkus.core;
    requires org.jboss.logging;
    requires arc.processor;
    requires org.jspecify;
    requires io.quarkus.gizmo;
    requires org.objectweb.asm;
    requires org.jboss.jandex;
}