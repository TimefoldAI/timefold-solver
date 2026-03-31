module ai.timefold.solver.quarkus.benchmark {

    exports ai.timefold.solver.benchmark.quarkus;
    exports ai.timefold.solver.benchmark.quarkus.config;

    requires transitive ai.timefold.solver.benchmark;
    requires transitive ai.timefold.solver.quarkus;
    requires io.quarkus.arc;
    requires io.quarkus.core;
    requires io.smallrye.config;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires org.eclipse.microprofile.config;

}