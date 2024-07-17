from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from dataclasses import dataclass, field
from typing import Annotated, List


def test_solver_events():
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
                              .as_constraint('Maximize value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_range: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

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

    problem: Solution = Solution([Entity('A'), Entity('B')], [1, 2, 3])
    score_list = []
    solution_list = []

    def on_best_solution_changed(event):
        solution_list.append(event.new_best_solution)
        score_list.append(event.new_best_score)

    solver = SolverFactory.create(solver_config).build_solver()
    solver.add_event_listener(on_best_solution_changed)
    solution = solver.solve(problem)

    assert solution.score.score == 6
    assert solution.entities[0].value == 3
    assert solution.entities[1].value == 3
    assert len(score_list) == len(solution_list)
    assert len(solution_list) == 1
    assert score_list[0].score == 6
    assert solution_list[0].score.score == 6
    assert solution_list[0].entities[0].value == 3
    assert solution_list[0].entities[1].value == 3
