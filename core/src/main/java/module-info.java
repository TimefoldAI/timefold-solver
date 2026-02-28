module ai.timefold.solver.core {
    // explicit exports to other modules
    exports ai.timefold.solver.core.impl.solver.scope to
            ai.timefold.solver.jackson, ai.timefold.solver.benchmark, ai.timefold.solver.spring.boot.autoconfigure,
            ai.timefold.solver.quarkus.deployment, ai.timefold.solver.quarkus.integration.test,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.constructionheuristic.event to
            ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.constructionheuristic.scope to
            ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.io.jaxb to
            ai.timefold.solver.jackson, ai.timefold.solver.jaxb, ai.timefold.solver.spring.boot.autoconfigure,
            ai.timefold.solver.benchmark,
            ai.timefold.solver.quarkus,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.domain.entity.descriptor
            to ai.timefold.solver.jackson, ai.timefold.solver.jaxb, ai.timefold.solver.benchmark,
            ai.timefold.solver.spring.boot.autoconfigure, ai.timefold.solver.quarkus.integration.test,
            ai.timefold.solver.quarkus,
            ai.timefold.solver.quarkus.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.domain.solution to ai.timefold.solver.jackson, ai.timefold.solver.quarkus.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.domain.variable.descriptor
            to ai.timefold.solver.jackson, ai.timefold.solver.jaxb, ai.timefold.solver.benchmark,
            ai.timefold.solver.quarkus, ai.timefold.solver.quarkus.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.util
            to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.quarkus.deployment, ai.timefold.solver.quarkus.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.stream to ai.timefold.solver.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.stream.collector
            to ai.timefold.solver.jackson, ai.timefold.solver.quarkus.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.solver
            to ai.timefold.solver.jackson, ai.timefold.solver.spring.boot.autoconfigure, ai.timefold.solver.benchmark,
            ai.timefold.solver.quarkus,
            ai.timefold.solver.quarkus.deployment, ai.timefold.solver.quarkus.integration.test,
            ai.timefold.solver.quarkus.jackson,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.io.jaxb.adapter to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.definition
            to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.entity to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.entity.pillar
            to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.move to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.value to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.localsearch.event to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.phase.event to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.phase.scope to ai.timefold.solver.jackson, ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.common.nearby
            to ai.timefold.solver.jackson,
            ai.timefold.solver.benchmark,
            ai.timefold.solver.benchmark.aggregator,
            ai.timefold.solver.spring.boot.autoconfigure, ai.timefold.solver.quarkus.deployment,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.constraint to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.partitionedsearch.partitioner to ai.timefold.solver.quarkus.deployment,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.common.decorator to ai.timefold.solver.quarkus.deployment,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.trend to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.director;
    exports ai.timefold.solver.core.impl.score.director.easy to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.score.director.incremental to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.solver.monitoring to ai.timefold.solver.benchmark,
            ai.timefold.solver.enterprise.core;

    // Preview APIs
    exports ai.timefold.solver.core.preview.api.move;
    exports ai.timefold.solver.core.preview.api.move.builtin;
    exports ai.timefold.solver.core.preview.api.domain.metamodel;
    exports ai.timefold.solver.core.preview.api.domain.solution.diff;
    exports ai.timefold.solver.core.api.score.stream.test;

    // enterprise specific exports
    exports ai.timefold.solver.core.impl.bavet.common to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.constructionheuristic.decider to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.constructionheuristic.decider.forager to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.domain.variable to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.domain.variable.supply to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.domain.variable.listener.support to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.common to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.common.iterator to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.entity.mimic to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.list.mimic to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.heuristic.selector.value.mimic to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.localsearch.decider to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.localsearch.decider.acceptor to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.localsearch.decider.forager to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.move to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.neighborhood to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.partitionedsearch to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.phase to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.solver.recaller to ai.timefold.solver.enterprise.core;
    exports ai.timefold.solver.core.impl.solver.event to ai.timefold.solver.enterprise.core;

    // Broad impl usage
    exports ai.timefold.solver.core.impl.solver.thread;
    exports ai.timefold.solver.core.impl.heuristic.move;
    exports ai.timefold.solver.core.impl.localsearch.scope;
    exports ai.timefold.solver.core.impl.heuristic.selector.list;
    exports ai.timefold.solver.core.impl.heuristic.selector.move.factory;
    exports ai.timefold.solver.core.impl.heuristic.selector.move.generic;
    exports ai.timefold.solver.core.impl.heuristic.selector.move.generic.list;
    exports ai.timefold.solver.core.impl.score.stream.common;
    exports ai.timefold.solver.core.impl.score.stream.common.inliner;
    exports ai.timefold.solver.core.impl.score.director.stream;
    exports ai.timefold.solver.core.impl.score.stream.bavet;
    exports ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;
    exports ai.timefold.solver.core.impl.domain.solution.descriptor;
    exports ai.timefold.solver.core.impl.domain.common.accessor;
    exports ai.timefold.solver.core.impl.domain.common;
    exports ai.timefold.solver.core.impl.domain.common.accessor.gizmo;
    exports ai.timefold.solver.core.impl.solver.termination;
    exports ai.timefold.solver.core.impl.domain.variable.declarative;
    exports ai.timefold.solver.core.impl.score.stream.test;

    // expected exports
    exports ai.timefold.solver.core.api.domain.common;
    exports ai.timefold.solver.core.api.domain.entity;
    exports ai.timefold.solver.core.api.domain.solution;
    exports ai.timefold.solver.core.api.domain.solution.cloner;
    exports ai.timefold.solver.core.api.domain.valuerange;
    exports ai.timefold.solver.core.api.domain.variable;
    exports ai.timefold.solver.core.api.function;
    exports ai.timefold.solver.core.api.score;
    exports ai.timefold.solver.core.api.score.analysis;
    exports ai.timefold.solver.core.api.score.stream;
    exports ai.timefold.solver.core.api.score.stream.common;
    exports ai.timefold.solver.core.api.score.stream.uni;
    exports ai.timefold.solver.core.api.score.stream.bi;
    exports ai.timefold.solver.core.api.score.stream.tri;
    exports ai.timefold.solver.core.api.score.stream.penta;
    exports ai.timefold.solver.core.api.score.stream.quad;
    exports ai.timefold.solver.core.api.score.calculator;
    exports ai.timefold.solver.core.config.solver;
    exports ai.timefold.solver.core.config.solver.random;
    exports ai.timefold.solver.core.config.solver.monitoring;
    exports ai.timefold.solver.core.config.solver.termination;
    exports ai.timefold.solver.core.config.heuristic.selector.entity;
    exports ai.timefold.solver.core.config.heuristic.selector.entity.pillar;
    exports ai.timefold.solver.core.config.heuristic.selector.list;
    exports ai.timefold.solver.core.config.heuristic.selector.move;
    exports ai.timefold.solver.core.config.heuristic.selector.move.factory;
    exports ai.timefold.solver.core.config.heuristic.selector.move.composite;
    exports ai.timefold.solver.core.config.heuristic.selector.move.generic;
    exports ai.timefold.solver.core.config.heuristic.selector.move.generic.list;
    exports ai.timefold.solver.core.config.heuristic.selector.value;
    exports ai.timefold.solver.core.config.heuristic.selector.common;
    exports ai.timefold.solver.core.config.heuristic.selector.common.decorator;
    exports ai.timefold.solver.core.config.heuristic.selector.common.nearby;
    exports ai.timefold.solver.core.config.localsearch;
    exports ai.timefold.solver.core.config.localsearch.decider.forager;
    exports ai.timefold.solver.core.config.localsearch.decider.acceptor;
    exports ai.timefold.solver.core.config.localsearch.decider.acceptor.stepcountinghillclimbing;
    exports ai.timefold.solver.core.config.partitionedsearch;
    exports ai.timefold.solver.core.config.phase;
    exports ai.timefold.solver.core.config.phase.custom;
    exports ai.timefold.solver.core.config.score.director;
    exports ai.timefold.solver.core.config.score.trend;
    exports ai.timefold.solver.core.config.util;
    exports ai.timefold.solver.core.config;
    exports ai.timefold.solver.core.api.score.constraint;
    exports ai.timefold.solver.core.api.solver;
    exports ai.timefold.solver.core.api.solver.event;
    exports ai.timefold.solver.core.api.solver.phase;
    exports ai.timefold.solver.core.api.solver.change;
    exports ai.timefold.solver.core.config.constructionheuristic;
    exports ai.timefold.solver.core.enterprise;

    requires commons.math3;
    requires jakarta.xml.bind;
    requires java.xml;
    requires micrometer.core;
    requires org.jspecify;
    requires org.slf4j;
    requires io.quarkus.gizmo2;
}