module ai.timefold.solver.quarkus.benchmark {

    exports ai.timefold.solver.benchmark.quarkus;
    exports ai.timefold.solver.benchmark.quarkus.config;

    requires ai.timefold.solver.benchmark;
    requires ai.timefold.solver.core;
    requires ai.timefold.solver.quarkus;
    requires arc;
    requires io.smallrye.config;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires org.eclipse.microprofile.config;
    requires quarkus.core;

}