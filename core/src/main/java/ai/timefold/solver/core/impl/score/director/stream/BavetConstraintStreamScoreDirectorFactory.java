package ai.timefold.solver.core.impl.score.director.stream;

import java.util.Arrays;
import java.util.function.Consumer;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.stream.ConstraintMetaModel;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchPolicy;
import ai.timefold.solver.core.impl.score.director.AbstractScoreDirector;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintFactory;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSession;
import ai.timefold.solver.core.impl.score.stream.bavet.BavetConstraintSessionFactory;
import ai.timefold.solver.core.impl.score.stream.common.AbstractConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.stream.common.inliner.AbstractScoreInliner;

public final class BavetConstraintStreamScoreDirectorFactory<Solution_, Score_ extends Score<Score_>>
        extends
        AbstractConstraintStreamScoreDirectorFactory<Solution_, Score_, BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>> {

    public static <Solution_, Score_ extends Score<Score_>> BavetConstraintStreamScoreDirectorFactory<Solution_, Score_>
            buildScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor, ScoreDirectorFactoryConfig config,
                    EnvironmentMode environmentMode) {
        var providedConstraintProviderClass = config.getConstraintProviderClass();
        if (providedConstraintProviderClass == null
                || !ConstraintProvider.class.isAssignableFrom(providedConstraintProviderClass)) {
            throw new IllegalArgumentException(
                    "The constraintProviderClass (%s) does not implement %s."
                            .formatted(providedConstraintProviderClass, ConstraintProvider.class.getSimpleName()));
        }
        var constraintProviderClass = getConstraintProviderClass(config, providedConstraintProviderClass);
        var constraintProvider = ConfigUtils.newInstance(config, "constraintProviderClass", constraintProviderClass);
        ConfigUtils.applyCustomProperties(constraintProvider, "constraintProviderClass",
                config.getConstraintProviderCustomProperties(), "constraintProviderCustomProperties");
        return new BavetConstraintStreamScoreDirectorFactory<>(solutionDescriptor, constraintProvider, environmentMode);
    }

    private static Class<? extends ConstraintProvider> getConstraintProviderClass(ScoreDirectorFactoryConfig config,
            Class<? extends ConstraintProvider> providedConstraintProviderClass) {
        if (Boolean.TRUE.equals(config.getConstraintStreamAutomaticNodeSharing())) {
            var enterpriseService =
                    TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.AUTOMATIC_NODE_SHARING);
            return enterpriseService.buildLambdaSharedConstraintProvider(config.getConstraintProviderClass());
        } else {
            return providedConstraintProviderClass;
        }
    }

    private final BavetConstraintSessionFactory<Solution_, Score_> constraintSessionFactory;
    private final ConstraintMetaModel constraintMetaModel;

    public BavetConstraintStreamScoreDirectorFactory(SolutionDescriptor<Solution_> solutionDescriptor,
            ConstraintProvider constraintProvider, EnvironmentMode environmentMode) {
        super(solutionDescriptor);
        var constraintFactory = new BavetConstraintFactory<>(solutionDescriptor, environmentMode);
        constraintMetaModel = DefaultConstraintMetaModel.of(constraintFactory.buildConstraints(constraintProvider));
        constraintSessionFactory = new BavetConstraintSessionFactory<>(solutionDescriptor, constraintMetaModel);
    }

    public BavetConstraintSession<Score_> newSession(Solution_ workingSolution, ConstraintMatchPolicy constraintMatchPolicy,
            boolean scoreDirectorDerived) {
        return newSession(workingSolution, constraintMatchPolicy, scoreDirectorDerived, null);
    }

    public BavetConstraintSession<Score_> newSession(Solution_ workingSolution, ConstraintMatchPolicy constraintMatchPolicy,
            boolean scoreDirectorDerived, Consumer<String> nodeNetworkVisualizationConsumer) {
        return constraintSessionFactory.buildSession(workingSolution, constraintMatchPolicy, scoreDirectorDerived,
                nodeNetworkVisualizationConsumer);
    }

    @Override
    public AbstractScoreInliner<Score_> fireAndForget(Object... facts) {
        var session = newSession(null, ConstraintMatchPolicy.ENABLED, true);
        Arrays.stream(facts).forEach(session::insert);
        session.calculateScore();
        return session.getScoreInliner();
    }

    @Override
    public ConstraintMetaModel getConstraintMetaModel() {
        return constraintMetaModel;
    }

    @Override
    public BavetConstraintStreamScoreDirector.Builder<Solution_, Score_> createScoreDirectorBuilder() {
        return new BavetConstraintStreamScoreDirector.Builder<>(this);
    }

    @Override
    public AbstractScoreDirector<Solution_, Score_, ?> buildScoreDirector() {
        return createScoreDirectorBuilder().build();
    }

}
