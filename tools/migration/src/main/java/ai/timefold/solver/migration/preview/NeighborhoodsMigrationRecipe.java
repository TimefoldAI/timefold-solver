package ai.timefold.solver.migration.preview;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangeType;

public class NeighborhoodsMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate the Neighborhoods preview API";
    }

    @Override
    public String getDescription() {
        return "Migrate the Neighborhoods preview API to its new class structure.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Sampling streams renamed to picking streams
                new ChangeType("ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.SamplingStream",
                        "ai.timefold.solver.core.preview.api.neighborhood.stream.picking.PickingStream", true),
                new ChangeType("ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.UniSamplingStream",
                        "ai.timefold.solver.core.preview.api.neighborhood.stream.picking.UniPickingStream", true),
                new ChangeType("ai.timefold.solver.core.preview.api.neighborhood.stream.sampling.BiSamplingStream",
                        "ai.timefold.solver.core.preview.api.neighborhood.stream.picking.BiPickingStream", true),
                new ChangeType("ai.timefold.solver.core.impl.neighborhood.stream.sampling.InnerSamplingStream",
                        "ai.timefold.solver.core.impl.neighborhood.stream.picking.InnerPickingStream", true),
                new ChangeType("ai.timefold.solver.core.impl.neighborhood.stream.sampling.InnerUniSamplingStream",
                        "ai.timefold.solver.core.impl.neighborhood.stream.picking.InnerUniPickingStream", true),
                new ChangeType("ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultUniSamplingStream",
                        "ai.timefold.solver.core.impl.neighborhood.stream.picking.DefaultUniPickingStream", true),
                new ChangeType("ai.timefold.solver.core.impl.neighborhood.stream.sampling.DefaultBiSamplingStream",
                        "ai.timefold.solver.core.impl.neighborhood.stream.picking.DefaultBiPickingStream", true));
    }
}
