module ai.timefold.solver.benchmark {

    requires ai.timefold.solver.core;
    requires ai.timefold.solver.jaxb;
    requires commons.math3;
    requires freemarker;
    requires jakarta.xml.bind;
    requires micrometer.core;
    requires org.jspecify;
    requires org.slf4j;

    // Public API
    exports ai.timefold.solver.benchmark.api;

    // Config APIs; need to be open to JAXB for XML config parsing, happens below.
    exports ai.timefold.solver.benchmark.config;
    exports ai.timefold.solver.benchmark.config.blueprint;
    exports ai.timefold.solver.benchmark.config.ranking;
    exports ai.timefold.solver.benchmark.config.report;
    exports ai.timefold.solver.benchmark.config.statistic;

    // Contains JAXB serialization.
    exports ai.timefold.solver.benchmark.impl.result;

    // Needed by aggregator.
    exports ai.timefold.solver.benchmark.impl.report
            to ai.timefold.solver.benchmark.aggregator;
    exports ai.timefold.solver.benchmark.impl.statistic.common
            to ai.timefold.solver.benchmark.aggregator;

    // Open JAXB-serialized types to JAXB.
    opens ai.timefold.solver.benchmark.config to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens ai.timefold.solver.benchmark.config.blueprint to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens ai.timefold.solver.benchmark.config.ranking to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens ai.timefold.solver.benchmark.config.report to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens ai.timefold.solver.benchmark.config.statistic to jakarta.xml.bind, org.glassfish.jaxb.runtime;
    opens ai.timefold.solver.benchmark.impl.result to jakarta.xml.bind, org.glassfish.jaxb.runtime;

}