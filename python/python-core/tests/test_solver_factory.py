from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from dataclasses import dataclass, field
from typing import Annotated, List


@planning_entity
@dataclass
class Entity:
    code: Annotated[str, PlanningId]
    value: Annotated[int, PlanningVariable] = field(default=None, compare=False)


@constraint_provider
def my_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(Entity)
        .reward(SimpleScore.ONE, lambda entity: entity.value)
        .as_constraint('Maximize value'),
    ]


@planning_solution
@dataclass
class Solution:
    entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
    value_range: Annotated[List[int], ValueRangeProvider]
    score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    def __str__(self) -> str:
        return str(self.entities)


def test_solver_config_override():
    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints,
        ),
        termination_config=TerminationConfig(
            best_score_limit='9'
        )
    )
    solver_factory = SolverFactory.create(solver_config)
    solver = solver_factory.build_solver(SolverConfigOverride(
        termination_config=TerminationConfig(
            best_score_limit='3'
        )
    ))
    problem = Solution([Entity('A')], [1, 2, 3])
    solution = solver.solve(problem)
    assert solution.score.score == 3
