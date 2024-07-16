from typing import Annotated, Optional, List
from dataclasses import dataclass, field

from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *


def test_custom_shadow_variable():
    class MyVariableListener(VariableListener):
        def after_variable_changed(self, score_director: ScoreDirector, entity):
            score_director.before_variable_changed(entity, 'value_squared')
            if entity.value is None:
                entity.value_squared = None
            else:
                entity.value_squared = entity.value ** 2
            score_director.after_variable_changed(entity, 'value_squared')

    @planning_entity
    @dataclass
    class MyPlanningEntity:
        value: Annotated[Optional[int], PlanningVariable] \
            = field(default=None)
        value_squared: Annotated[Optional[int], ShadowVariable(variable_listener_class=MyVariableListener,
                                                               source_variable_name='value')] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(MyPlanningEntity)
            .filter(lambda entity: entity.value * 2 == entity.value_squared)
            .reward(SimpleScore.ONE)
            .as_constraint('Double value is value squared')
        ]

    @planning_solution
    @dataclass
    class MySolution:
        entity_list: Annotated[List[MyPlanningEntity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=MySolution,
        entity_class_list=[MyPlanningEntity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='1'
        )
    )

    solver_factory = SolverFactory.create(solver_config)
    solver = solver_factory.build_solver()
    problem = MySolution([MyPlanningEntity()], [1, 2, 3])
    solution: MySolution = solver.solve(problem)
    assert solution.score.score == 1
    assert solution.entity_list[0].value == 2
    assert solution.entity_list[0].value_squared == 4


def test_custom_shadow_variable_with_variable_listener_ref():
    class MyVariableListener(VariableListener):
        def after_variable_changed(self, score_director: ScoreDirector, entity):
            score_director.before_variable_changed(entity, 'twice_value')
            score_director.before_variable_changed(entity, 'value_squared')
            if entity.value is None:
                entity.twice_value = None
                entity.value_squared = None
            else:
                entity.twice_value = 2 * entity.value
                entity.value_squared = entity.value ** 2
            score_director.after_variable_changed(entity, 'value_squared')
            score_director.after_variable_changed(entity, 'twice_value')

    @planning_entity
    @dataclass
    class MyPlanningEntity:
        value: Annotated[Optional[int], PlanningVariable] = \
            field(default=None)
        value_squared: Annotated[Optional[int], ShadowVariable(
            variable_listener_class=MyVariableListener, source_variable_name='value')] = field(default=None)
        twice_value: Annotated[Optional[int], PiggybackShadowVariable(shadow_variable_name='value_squared')] = (
            field(default=None))

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(MyPlanningEntity)
            .filter(lambda entity: entity.twice_value == entity.value_squared)
            .reward(SimpleScore.ONE)
            .as_constraint('Double value is value squared')
        ]

    @planning_solution
    @dataclass
    class MySolution:
        entity_list: Annotated[List[MyPlanningEntity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=MySolution,
        entity_class_list=[MyPlanningEntity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='1'
        )
    )

    solver_factory = SolverFactory.create(solver_config)
    solver = solver_factory.build_solver()
    problem = MySolution([MyPlanningEntity()], [1, 2, 3])
    solution: MySolution = solver.solve(problem)
    assert solution.score.score == 1
    assert solution.entity_list[0].value == 2
    assert solution.entity_list[0].value_squared == 4
