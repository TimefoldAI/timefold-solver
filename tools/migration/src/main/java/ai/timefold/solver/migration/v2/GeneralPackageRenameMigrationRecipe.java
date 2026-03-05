package ai.timefold.solver.migration.v2;

import java.util.List;

import ai.timefold.solver.migration.AbstractRecipe;

import org.openrewrite.Recipe;
import org.openrewrite.java.ChangePackage;

public class GeneralPackageRenameMigrationRecipe extends AbstractRecipe {
    @Override
    public String getDisplayName() {
        return "Migrate legacy packages to the new structure";
    }

    @Override
    public String getDescription() {
        return "Migrate all legacy packages to the new structure.";
    }

    @Override
    public List<Recipe> getRecipeList() {
        return List.of(
                // Persistence API
                new ChangePackage("ai.timefold.solver.persistence.common.api.domain.solution",
                        "ai.timefold.solver.core.api.domain.solution",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.bendablebigdecimal",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.bendable",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.hardmediumsoftbigdecimal",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.hardmediumsoft",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.hardsoftbigdecimal",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.hardsoft",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.simplebigdecimal",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jpa.api.score.buildin.simple",
                        "ai.timefold.solver.jpa.api.score",
                        true),
                // Jackson API
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.bendable",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftbigdecimal",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.hardsoftbigdecimal",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.hardsoft",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.simplebigdecimal",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                new ChangePackage("ai.timefold.solver.jackson.api.score.buildin.simple",
                        "ai.timefold.solver.jackson.api.score.buildin",
                        true),
                // JAXB API
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.bendablebigdecimal",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.bendable",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoftbigdecimal",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoft",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.hardsoftbigdecimal",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.hardsoft",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.simplebigdecimal",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                new ChangePackage("ai.timefold.solver.jaxb.api.score.buildin.simple",
                        "ai.timefold.solver.jaxb.api.score",
                        true),
                // Jackson Quarkus API
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.bendablebigdecimal",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.bendable",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.hardmediumsoftbigdecimal",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.hardmediumsoft",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.hardsoftbigdecimal",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.hardsoft",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.simplebigdecimal",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                new ChangePackage("ai.timefold.solver.quarkus.jackson.score.buildin.simple",
                        "ai.timefold.solver.quarkus.jackson.score",
                        true),
                // Value Range API
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.bigdecimal",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.biginteger",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.primboolean",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.primint",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.collection",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.primlong",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin.temporal",
                        "ai.timefold.solver.core.impl.domain.valuerange", true),
                new ChangePackage("ai.timefold.solver.core.impl.domain.valuerange.buildin",
                        "ai.timefold.solver.core.impl.domain.valuerange", true));

    }
}
