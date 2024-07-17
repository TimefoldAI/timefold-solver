from ..score import ConstraintFactory, Constraint, IncrementalScoreCalculator
from .._timefold_java_interop import is_enterprise_installed

from typing import Any, Optional, Callable, TypeVar, Generic, Literal, TYPE_CHECKING
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from jpype import JClass

if TYPE_CHECKING:
    from java.time import Duration as _JavaDuration
    from ai.timefold.solver.core.config.solver import SolverConfig as _JavaSolverConfig
    from ai.timefold.solver.core.config.solver.termination import TerminationConfig as _JavaTerminationConfig
    from ai.timefold.solver.core.config.score.director import (
        ScoreDirectorFactoryConfig as _JavaScoreDirectorFactoryConfig)


_java_environment_mode = 'ai.timefold.solver.core.config.solver.EnvironmentMode'
_java_termination_composition_style = 'ai.timefold.solver.core.config.solver.termination.TerminationCompositionStyle'


def _lookup_on_java_class(java_class: str, attribute: str) -> Any:
    return getattr(JClass(java_class), attribute)


@dataclass(kw_only=True)
class Duration:
    """
    Represents a duration of time.
    """
    milliseconds: int = field(default=0)
    seconds: int = field(default=0)
    minutes: int = field(default=0)
    hours: int = field(default=0)
    days: int = field(default=0)

    def to_milliseconds(self) -> int:
        return self._to_java_duration().toMillis()

    def to_seconds(self) -> int:
        return self._to_java_duration().toSeconds()

    def to_minutes(self) -> int:
        return self._to_java_duration().toMinutes()

    def to_hours(self) -> int:
        return self._to_java_duration().toHours()

    def to_days(self) -> int:
        return self._to_java_duration().toDays()

    @staticmethod
    def _from_java_duration(duration: '_JavaDuration'):
        return Duration(
            milliseconds=duration.toMillis()
        )

    def _to_java_duration(self) -> '_JavaDuration':
        from java.time import Duration
        return (Duration.ZERO
                .plusMillis(self.milliseconds)
                .plusSeconds(self.seconds)
                .plusMinutes(self.minutes)
                .plusHours(self.hours)
                .plusDays(self.days)
                )


class EnvironmentMode(Enum):
    """
    The environment mode also allows you to detect common bugs in your implementation.
    Also, a `Solver` has a single Random instance.
    Some optimization algorithms use the Random instance a lot more than others.
    For example simulated annealing depends highly on random numbers,
    while tabu search only depends on it to deal with score ties.
    This environment mode influences the seed of that Random instance.
    """

    NON_REPRODUCIBLE = 'NON_REPRODUCIBLE'
    """
    The non reproducible mode is equally fast or slightly faster than REPRODUCIBLE.
    The random seed is different on every run,
    which makes it more robust against an unlucky random seed.
    An unlucky random seed gives a bad result on a certain data set with a certain solver configuration.
    Note that in most use cases the impact of the random seed is relatively low on the result.
    An occasional bad result is far more likely to be caused by another issue (such as a score trap).
    In multithreaded scenarios, this mode allows the use of work stealing and other non deterministic speed tricks.
    """

    REPRODUCIBLE = 'REPRODUCIBLE'
    """
    The reproducible mode is the default mode because it is recommended during development.
    In this mode, 2 runs on the same computer will execute the same code in the same order.
    They will also yield the same result,
    except if they use a time based termination and they have a sufficiently large difference in allocated CPU time.
    This allows you to benchmark new optimizations (such as a new Move implementation)
    fairly and reproduce bugs in your code reliably.
    
    Warning: some code can disrupt reproducibility regardless of this mode.
    See the reference manual for more info.
    In practice, this mode uses the default random seed,
    and it also disables certain concurrency optimizations (such as work stealing).
    """

    FAST_ASSERT = 'FAST_ASSERT'
    """
    This mode turns on several assertions (but not all of them) to fail-fast on a bug in a Move implementation,
    a constraint rule, the engine itself or something else at a reasonable performance cost (in development at least).
    This mode is reproducible (see REPRODUCIBLE mode).
    This mode is intrusive because it calls calculate_score more frequently than a non assert mode.
    This mode is slow.
    """

    NON_INTRUSIVE_FULL_ASSERT = 'NON_INTRUSIVE_FULL_ASSERT'
    """
    This mode turns on several assertions (but not all of them) to fail-fast on a bug in a Move implementation,
    a constraint, the engine itself or something else at an overwhelming performance cost.
    This mode is reproducible (see REPRODUCIBLE mode).
    This mode is non-intrusive, unlike FULL_ASSERT and FAST_ASSERT.
    This mode is horribly slow.
    """

    FULL_ASSERT = 'FULL_ASSERT'
    """
    This mode turns on all assertions to fail-fast on a bug in a Move implementation,
    a constraint, the engine itself or something else at a horrible performance cost.
    This mode is reproducible (see REPRODUCIBLE mode).
    This mode is intrusive because it calls calculate_score more frequently than a non assert mode.
    This mode is horribly slow.
    """

    TRACKED_FULL_ASSERT = 'TRACKED_FULL_ASSERT'
    """
    This mode turns on FULL_ASSERT and enables variable tracking to fail-fast on a bug in a Move implementation,
    a constraint, the engine itself or something else at the highest performance cost.
    Because it tracks genuine and shadow variables,
    it is able to report precisely what variables caused the corruption and report any missed VariableListener events.
    This mode is reproducible (see REPRODUCIBLE mode).
    This mode is intrusive because it calls calculate_score more frequently than a non assert mode.
    This mode is by far the slowest of all the modes.
    """

    def _get_java_enum(self):
        return _lookup_on_java_class(_java_environment_mode, self.name)


class TerminationCompositionStyle(Enum):
    OR = 'OR'
    AND = 'AND'

    def _get_java_enum(self):
        return _lookup_on_java_class(_java_termination_composition_style, self.name)


class MoveThreadCount(Enum):
    AUTO = 'AUTO'
    """
    Configure the number of move threads dynamically based on the
    computer's core count.
    """

    NONE = 'NONE'
    """
    Disables multithreaded solving.
    """


class RequiresEnterpriseError(EnvironmentError):
    def __init__(self, feature):
        super().__init__(f'Feature {feature} requires timefold-enterprise to be installed. '
                         f'See https://docs.timefold.ai/timefold-solver/latest/enterprise-edition/'
                         f'enterprise-edition#switchToEnterpriseEdition for instructions on how to '
                         f'install timefold-enterprise.')


Solution_ = TypeVar('Solution_')


@dataclass(kw_only=True)
class SolverConfig(Generic[Solution_]):
    """
    To read it from XML, use `create_from_xml_resource`.
    To build a `SolverFactory` with it, use `SolverFactory.create`.
    """
    solution_class: Optional[type[Solution_]] = field(default=None)
    entity_class_list: Optional[list[type]] = field(default=None)
    environment_mode: Optional[EnvironmentMode] = field(default=EnvironmentMode.REPRODUCIBLE)
    random_seed: Optional[int] = field(default=None)
    move_thread_count: int | MoveThreadCount = field(default=MoveThreadCount.NONE)
    nearby_distance_meter_function: Optional[Callable[[Any, Any], float]] = field(default=None)
    termination_config: Optional['TerminationConfig'] = field(default=None)
    score_director_factory_config: Optional['ScoreDirectorFactoryConfig'] = field(default=None)
    xml_source_text: Optional[str] = field(default=None)
    xml_source_file: Optional[Path] = field(default=None)

    @staticmethod
    def create_from_xml_resource(path: Path) -> 'SolverConfig':
        return SolverConfig(xml_source_file=path)

    @staticmethod
    def create_from_xml_text(xml_text: str) -> 'SolverConfig':
        return SolverConfig(xml_source_text=xml_text)

    def _to_java_solver_config(self) -> '_JavaSolverConfig':
        from .._timefold_java_interop import OverrideClassLoader, get_class, _process_compilation_queue
        from ai.timefold.solver.core.config.solver import SolverConfig as JavaSolverConfig
        from java.io import File, ByteArrayInputStream  # noqa
        from java.lang import IllegalArgumentException
        from java.util import ArrayList

        _process_compilation_queue()

        out = JavaSolverConfig()
        with OverrideClassLoader() as class_loader:
            out.setClassLoader(class_loader)
            # First, inherit the config from the xml text/file
            if self.xml_source_text is not None:
                inherited = JavaSolverConfig.createFromXmlInputStream(
                    ByteArrayInputStream(self.xml_source_text.encode()))
                out.inherit(inherited)
            if self.xml_source_file is not None:
                try:
                    inherited = JavaSolverConfig.createFromXmlFile(File(str(self.xml_source_file)))
                    out.inherit(inherited)
                except IllegalArgumentException as e:
                    raise FileNotFoundError(e.getMessage()) from e

            # Next, override fields
            if self.move_thread_count is not MoveThreadCount.NONE:
                if not is_enterprise_installed():
                    raise RequiresEnterpriseError('multithreaded solving')
                if isinstance(self.move_thread_count, MoveThreadCount):
                    out.setMoveThreadCount(self.move_thread_count.name)
                else:
                    out.setMoveThreadCount(str(self.move_thread_count))
            elif out.getMoveThreadCount() is not None and not is_enterprise_installed():
                raise RequiresEnterpriseError('multithreaded solving')

            if self.nearby_distance_meter_function is not None:
                if not is_enterprise_installed():
                    raise RequiresEnterpriseError('nearby selection')
                out.setNearbyDistanceMeterClass(get_class(self.nearby_distance_meter_function))
            elif out.getNearbyDistanceMeterClass() is not None and not is_enterprise_installed():
                raise RequiresEnterpriseError('nearby selection')

            if self.solution_class is not None:
                from ai.timefold.solver.core.api.domain.solution import PlanningSolution as JavaPlanningSolution
                java_class = get_class(self.solution_class)
                if java_class is None:
                    raise RuntimeError(f'Unable to generate Java class for {self.solution_class}')
                if java_class.getAnnotation(JavaPlanningSolution) is None:
                    raise TypeError(f'{self.solution_class} is not a @planning_solution class. '
                                    f'Maybe move the @planning_solution decorator to the top of the decorator list?')
                out.setSolutionClass(get_class(self.solution_class))

            if self.entity_class_list is not None:
                from ai.timefold.solver.core.api.domain.entity import PlanningEntity as JavaPlanningEntity
                entity_class_list = ArrayList()
                for entity_class in self.entity_class_list:
                    java_class = get_class(entity_class)
                    if java_class is None:
                        raise RuntimeError(f'Unable to generate Java class for {entity_class}')
                    if java_class.getAnnotation(JavaPlanningEntity) is None:
                        raise TypeError(f'{entity_class} is not a @planning_entity class. '
                                        f'Maybe move the @planning_entity decorator to the top of the decorator list?')
                    entity_class_list.add(java_class)
                out.setEntityClassList(entity_class_list)

            if self.environment_mode is not None:
                out.setEnvironmentMode(self.environment_mode._get_java_enum())

            if self.random_seed is not None:
                out.setRandomSeed(self.random_seed)

            if self.score_director_factory_config is not None:
                out.setScoreDirectorFactoryConfig(
                    self.score_director_factory_config._to_java_score_director_factory_config())

            if self.termination_config is not None:
                out.setTerminationConfig(
                    self.termination_config._to_java_termination_config(out.getTerminationConfig()))

            return out


@dataclass(kw_only=True)
class ScoreDirectorFactoryConfig:
    constraint_provider_function: Optional[Callable[[ConstraintFactory], list[Constraint]]] =\
        field(default=None)
    easy_score_calculator_function: Optional[Callable] = field(default=None)
    incremental_score_calculator_class: Optional[type[IncrementalScoreCalculator]] = field(default=None)

    def _to_java_score_director_factory_config(self, inherited_config: '_JavaScoreDirectorFactoryConfig' = None):
        from ai.timefold.solver.core.config.score.director import (
            ScoreDirectorFactoryConfig as JavaScoreDirectorFactoryConfig)
        from .._timefold_java_interop import get_class
        out = JavaScoreDirectorFactoryConfig()
        if inherited_config is not None:
            out.inherit(inherited_config)
        if self.constraint_provider_function is not None:
            from ai.timefold.solver.core.api.score.stream import ConstraintProvider
            java_class = get_class(self.constraint_provider_function)
            if not issubclass(java_class, ConstraintProvider):
                raise TypeError(f'{self.constraint_provider_function} is not a @constraint_provider function. '
                                f'Maybe move the @constraint_provider decorator to the top of the decorator list?')
            out.setConstraintProviderClass(java_class)  # noqa
        if self.easy_score_calculator_function is not None:
            out.setEasyScoreCalculatorClass(get_class(self.easy_score_calculator_function))  # noqa
        if self.incremental_score_calculator_class is not None:
            out.setIncrementalScoreCalculatorClass(get_class(self.incremental_score_calculator_class))  # noqa
        return out


@dataclass(kw_only=True)
class TerminationConfig:
    score_calculation_count_limit: Optional[int] = field(default=None)
    step_count_limit: Optional[int] = field(default=None)
    best_score_feasible: Optional[bool] = field(default=None)
    best_score_limit: Optional[str] = field(default=None)
    spent_limit: Optional[Duration] = field(default=None)
    unimproved_spent_limit: Optional[Duration] = field(default=None)
    unimproved_score_difference_threshold: Optional[str] = field(default=None)
    unimproved_step_count_limit: Optional[int] = field(default=None)
    termination_config_list: Optional[list['TerminationConfig']] = field(default=None)
    termination_composition_style: Optional[TerminationCompositionStyle] = field(default=None)

    def _to_java_termination_config(self, inherited_config: '_JavaTerminationConfig' = None) -> \
            '_JavaTerminationConfig':
        from java.util import ArrayList
        from ai.timefold.solver.core.config.solver.termination import TerminationConfig as JavaTerminationConfig
        out = JavaTerminationConfig()
        if inherited_config is not None:
            out.inherit(inherited_config)

        if self.score_calculation_count_limit is not None:
            out.setScoreCalculationCountLimit(self.score_calculation_count_limit)
        if self.step_count_limit is not None:
            out.setStepCountLimit(self.step_count_limit)
        if self.best_score_limit is not None:
            out.setBestScoreLimit(self.best_score_limit)
        if self.best_score_feasible is not None:
            out.setBestScoreFeasible(self.best_score_feasible)
        if self.spent_limit is not None:
            out.setSpentLimit(self.spent_limit._to_java_duration())
        if self.unimproved_spent_limit is not None:
            out.setUnimprovedSpentLimit(self.unimproved_spent_limit._to_java_duration())
        if self.unimproved_score_difference_threshold is not None:
            out.setUnimprovedScoreDifferenceThreshold(self.unimproved_score_difference_threshold)
        if self.unimproved_step_count_limit is not None:
            out.setUnimprovedStepCountLimit(self.unimproved_step_count_limit)
        if self.termination_config_list is not None:
            termination_config_list = ArrayList()
            for termination_config in self.termination_config_list:
                termination_config_list.add(termination_config._to_java_termination_config())
            out.setTerminationConfigList(termination_config_list)
        if self.termination_composition_style is not None:
            out.setTerminationCompositionStyle(self.termination_composition_style._get_java_enum())
        return out


@dataclass(kw_only=True)
class SolverConfigOverride:
    """
    Includes settings to override default Solver configuration.

    Attributes
    ----------
    termination_config: TerminationConfig, optional
        sets the solver TerminationConfig.
    """
    termination_config: Optional[TerminationConfig] = field(default=None)

    def _to_java_solver_config_override(self):
        from ai.timefold.solver.core.api.solver import SolverConfigOverride
        out = SolverConfigOverride()
        if self.termination_config is not None:
            out = out.withTerminationConfig(self.termination_config._to_java_termination_config())
        return out


@dataclass(kw_only=True)
class SolverManagerConfig:
    """
    Includes settings to configure a SolverManager.

    Attributes
    ----------
    parallel_solver_count: int | 'AUTO', optional
        If set to an integer, the number of parallel jobs that can be run
        simultaneously.
        If unset or set to 'AUTO', the number of parallel jobs is determined
        based on the number of CPU cores available.
    """
    parallel_solver_count: Optional[int | Literal['AUTO']] = field(default=None)

    def _to_java_solver_manager_config(self):
        from ai.timefold.solver.core.config.solver import SolverManagerConfig as JavaSolverManagerConfig
        out = JavaSolverManagerConfig()
        if self.parallel_solver_count is not None:
            out = out.withParallelSolverCount(str(self.parallel_solver_count))
        return out


__all__ = ['Duration', 'EnvironmentMode', 'TerminationCompositionStyle',
           'RequiresEnterpriseError', 'MoveThreadCount', 'SolverManagerConfig',
           'SolverConfig', 'SolverConfigOverride', 'ScoreDirectorFactoryConfig', 'TerminationConfig']
