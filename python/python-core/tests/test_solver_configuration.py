from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from dataclasses import dataclass, field
from typing import Annotated, List


def test_solver_configuration():
    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[int, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
                              .reward_configurable(lambda entity: entity.value)
                              .as_constraint('Maximize value'),
        ]

    @constraint_configuration
    @dataclass
    class ConstraintConfiguration:
        maximize_value: Annotated[SimpleScore, ConstraintWeight('Maximize value')] = field(default=SimpleScore.ONE)

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_range: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)
        configuration: Annotated[ConstraintConfiguration, ConstraintConfigurationProvider] = (
            field(default_factory=ConstraintConfiguration))

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints,
        ),
        termination_config=TerminationConfig(
            best_score_limit='6'
        )
    )

    problem: Solution = Solution([Entity('A')], [1, 2, 3],
                                 configuration=ConstraintConfiguration(maximize_value=SimpleScore.of(2)))

    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.score == 6
    assert solution.entities[0].value == 3
