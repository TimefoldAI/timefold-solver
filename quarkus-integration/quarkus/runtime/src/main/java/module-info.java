module ai.timefold.solver.quarkus {
    exports ai.timefold.solver.quarkus.bean;
    exports ai.timefold.solver.quarkus;
    exports ai.timefold.solver.quarkus.config;
    exports ai.timefold.solver.quarkus.devui;
    exports ai.timefold.solver.quarkus.gizmo;

    requires ai.timefold.solver.core;
    requires arc;
    requires io.vertx.core;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires org.graalvm.nativeimage;
    requires org.jspecify;
    requires quarkus.core;
    requires io.smallrye.config;
    requires org.eclipse.microprofile.config;
    requires org.jboss.logging;

}