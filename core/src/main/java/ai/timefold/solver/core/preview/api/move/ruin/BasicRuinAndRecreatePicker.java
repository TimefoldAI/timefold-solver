package ai.timefold.solver.core.preview.api.move.ruin;

import java.util.List;
import java.util.Random;

public interface BasicRuinAndRecreatePicker<Solution_, Entity_, Value_, Metadata_ extends RecreateMetadata> {
    void initialize(Solution_ solution);

    List<BasicRuinedOrRecreatedElement<Entity_, Value_, Metadata_>> pickValues(Random random);
}
