package ai.timefold.solver.core.impl.score.director.stream;

import static ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType.BAVET;

import java.util.Arrays;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintStreamImplType;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSessionFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.ConstraintLibrary;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class BavetConstraintStreamScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_> {

    public static <Solution_, Score_ extends Score<Score_>> BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>
            buildScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor, ScoreDirectorFactoryConfig config,
                    EnvironmentMode environmentMode) {
        var constraintStreamImplType_ =
                Objects.requireNonNullElse(config.getConstraintStreamImplType(), ConstraintStreamImplType.BAVET);
        if (constraintStreamImplType_ != BAVET) {
            throw new IllegalStateException("The constraintStreamImplType (%s) is not supported."
                    .formatted(constraintStreamImplType_));
        }
        if (!ConstraintProvider.class.isAssignableFrom(config.getConstraintProviderClass())) {
            throw new IllegalArgumentException(
                    "The constraintProviderClass (%s) does not implement %s."
                            .formatted(config.getConstraintProviderClass(), ConstraintProvider.class.getSimpleName()));
        }
        var constraintProviderClass = getConstraintProviderClass(config);
        var constraintProvider = ConfigUtils.newInstance(config, "constraintProviderClass", constraintProviderClass);
        ConfigUtils.applyCustomProperties(constraintProvider, "constraintProviderClass",
                config.getConstraintProviderCustomProperties(), "constraintProviderCustomProperties");
        return new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor, constraintProvider, environmentMode);
    }

    private static Class<? extends ConstraintProvider> getConstraintProviderClass(ScoreDirectorFactoryConfig config) {
        if (Boolean.TRUE.equals(config.getConstraintStreamAutomaticNodeSharing())) {
            var enterpriseService =
                    TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.AUTOMATIC_NODE_SHARING);
            return enterpriseService.buildLambdaSharedConstraintProvider(config.getConstraintProviderClass());
        } else {
            return config.getConstraintProviderClass();
        }
    }

    private final BavetConstraintSessionFactory<Solution_, Score_> constraintSessionFactory;
    private final ConstraintLibrary<Score_> constraintLibrary;

    public BavetConstraintStreamScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintProvider constraintProvider, EnvironmentMode environmentMode) {
        super(solutionDescriptor);
        var constraintFactory = new BavetConstraintFactory<>(solutionDescriptor, environmentMode);
        constraintLibrary = ConstraintLibrary.of(constraintFactory.buildConstraints(constraintProvider));
        constraintSessionFactory = new BavetConstraintSessionFactory<>(solutionDescriptor, constraintLibrary);
    }

    @Override
    public BavetConstraintStreamScoreDirector<Solution_, Score_> buildScoreDirector(boolean lookUpEnabled,
            boolean constraintMatchEnabledPreference, boolean expectShadowVariablesInCorrectState) {
        return new BavetConstraintStreamScoreDirector<>(this, lookUpEnabled, constraintMatchEnabledPreference,
                expectShadowVariablesInCorrectState);
    }

    @Override
    public InnerScoreDirector<Solution_, Score_> buildDerivedScoreDirector(boolean lookUpEnabled,
            boolean constraintMatchEnabledPreference) {
        return new BavetConstraintStreamScoreDirector<>(this, lookUpEnabled, constraintMatchEnabledPreference,
                true, true);
    }

    public BavetConstraintSession<Score_> newSession(Solution_ workingSolution, boolean constraintMatchEnabled,
            boolean scoreDirectorDerived) {
        return constraintSessionFactory.buildSession(workingSolution, constraintMatchEnabled, scoreDirectorDerived);
    }

    @Override
    public AbstractScoreInliner<Score_> fireAndForget(Object... facts) {
        var session = newSession(null, true, true);
        Arrays.stream(facts).forEach(session::insert);
        session.calculateScore(0);
        return session.getScoreInliner();
    }

    @Override
    public ConstraintLibrary<Score_> getConstraintLibrary() {
        return constraintLibrary;
    }

}
