package ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.list;

import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;

record ListValueState(Object value, PositionInList positionInList) {
    int index() {
        return positionInList().index();
    }
}
