package ai.timefold.solver.migration;

import java.util.List;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;

public class ScoreClassesMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate to the new score class structure";
    }

    @Override
    public String getDescription() {
        return "Migrate all legacy score classes to the new class structure.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                new ChangeType("ai.timefold.solver.core.api.score.buildin.simple.SimpleScore",
                        "ai.timefold.solver.core.api.score.SimpleScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore",
                        "ai.timefold.solver.core.api.score.SimpleScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore",
                        "ai.timefold.solver.core.api.score.SimpleBigDecimalScore", true),

                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore",
                        "ai.timefold.solver.core.api.score.HardSoftScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore",
                        "ai.timefold.solver.core.api.score.HardSoftScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardsoftbigdecimal.HardSoftBigDecimalScore",
                        "ai.timefold.solver.core.api.score.HardSoftBigDecimalScore", true),

                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore",
                        "ai.timefold.solver.core.api.score.HardMediumSoftScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore",
                        "ai.timefold.solver.core.api.score.HardMediumSoftScore", true),
                new ChangeType(
                        "ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore",
                        "ai.timefold.solver.core.api.score.HardMediumSoftBigDecimalScore", true),

                new ChangeType("ai.timefold.solver.core.api.score.buildin.bendable.BendableScore",
                        "ai.timefold.solver.core.api.score.BendableScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore",
                        "ai.timefold.solver.core.api.score.BendableScore", true),
                new ChangeType("ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore",
                        "ai.timefold.solver.core.api.score.BendableBigDecimalScore", true));
    }
}
