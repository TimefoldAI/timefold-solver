package ai.timefold.solver.core.preview.api.move.ruin;

import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.api.score.Score;

import org.jspecify.annotations.NonNull;

public interface ListRuinAndRecreatePicker<Solution_, Entity_, Value_, Score_ extends Score<@NonNull Score_>> {
    void initialize(Solution_ solution);

    List<ListAssignmentAction<Entity_, Value_, Score_>> pickAssignments(Random random);
}
