module ai.timefold.solver.spring.boot.autoconfigure {

    exports ai.timefold.solver.spring.boot.autoconfigure;
    exports ai.timefold.solver.spring.boot.autoconfigure.config;
    exports ai.timefold.solver.spring.boot.autoconfigure.util;

    opens ai.timefold.solver.spring.boot.autoconfigure;

    requires static ai.timefold.solver.benchmark;
    requires transitive ai.timefold.solver.core;
    requires transitive ai.timefold.solver.jackson;
    requires org.apache.commons.logging;
    requires org.jspecify;
    requires spring.beans;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.boot.persistence;
    requires spring.context;
    requires spring.core;
    requires static tools.jackson.databind;

}