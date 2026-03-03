module ai.timefold.solver.quarkus {

    exports ai.timefold.solver.quarkus;
    exports ai.timefold.solver.quarkus.bean;
    exports ai.timefold.solver.quarkus.config;
    exports ai.timefold.solver.quarkus.devui;
    exports ai.timefold.solver.quarkus.gizmo;
    exports ai.timefold.solver.quarkus.nativeimage;

    requires transitive ai.timefold.solver.core;
    requires arc;
    requires io.smallrye.config;
    requires io.vertx.core;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires org.eclipse.microprofile.config;
    requires org.graalvm.nativeimage;
    requires org.jboss.logging;
    requires org.jspecify;
    requires quarkus.core;

}