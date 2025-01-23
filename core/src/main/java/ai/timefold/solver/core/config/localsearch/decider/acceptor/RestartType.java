package ai.timefold.solver.core.config.localsearch.decider.acceptor;

import jakarta.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum RestartType {
    UNIMPROVED_TIME,
    UNIMPROVED_MOVE_COUNT
}
