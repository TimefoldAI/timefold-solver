module ai.timefold.solver.benchmark {
    requires ai.timefold.solver.jaxb;
    requires ai.timefold.solver.persistence.common;
    requires freemarker;
    requires ai.timefold.solver.core;
    requires org.jspecify;
    requires jakarta.xml.bind;
    requires org.slf4j;
    requires micrometer.core;

    exports ai.timefold.solver.benchmark.api;
    exports ai.timefold.solver.benchmark.config;
    exports ai.timefold.solver.benchmark.config.report;
    exports ai.timefold.solver.benchmark.impl;
    exports ai.timefold.solver.benchmark.impl.report;
    exports ai.timefold.solver.benchmark.impl.result;
    exports ai.timefold.solver.benchmark.impl.statistic.common;
}