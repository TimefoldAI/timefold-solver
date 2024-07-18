from dataclasses import dataclass, field
from timefold.solver import *
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.score import *
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
                              .reward(SimpleScore.ONE, lambda entity: entity.value)
            .as_constraint('maximize_value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_range: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)
        weight_overrides: ConstraintWeightOverrides = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints,
        ),
        termination_config=TerminationConfig(
            best_score_limit='6',
            spent_limit=Duration(seconds=1)
        )
    )

    overrides = ConstraintWeightOverrides({
        "maximize_value": SimpleScore.of(2)
    })
    problem: Solution = Solution([Entity('A')], [1, 2, 3], weight_overrides=overrides)

    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)

    assert solution.score.score == 6
    assert solution.entities[0].value == 3
