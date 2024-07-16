from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from typing import Annotated, List
from dataclasses import dataclass, field


def test_easy_score_calculator():
    @planning_entity
    @dataclass
    class Entity:
        code: str
        value: Annotated[int, PlanningVariable] = field(default=None)

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_range: Annotated[List[int], ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    @easy_score_calculator
    def my_score_calculator(solution: Solution) -> SimpleScore:
        total_score = 0
        for entity in solution.entity_list:
            total_score += 0 if entity.value is None else entity.value
        return SimpleScore.of(total_score)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            easy_score_calculator_function=my_score_calculator
        ),
        termination_config=TerminationConfig(
            best_score_limit='9'
        )
    )
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [1, 2, 3])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == 9
    assert solution.entity_list[0].value == 3
    assert solution.entity_list[1].value == 3
    assert solution.entity_list[2].value == 3
