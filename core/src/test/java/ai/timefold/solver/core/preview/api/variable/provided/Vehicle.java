package ai.timefold.solver.core.preview.api.variable.provided;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.variable.PlanningListVariable;

@PlanningEntity
public class Vehicle {
    String id;

    @PlanningListVariable
    List<Visit> visits;

    public Vehicle() {
        visits = new ArrayList<>();
    }

    public Vehicle(String id) {
        this.id = id;
        visits = new ArrayList<>();
    }

    @Override
    public String toString() {
        return id;
    }
}
