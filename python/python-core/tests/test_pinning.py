from dataclasses import dataclass, field
from timefold.solver import *
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.score import *
from typing import Annotated, List


def test_planning_pin():
    @planning_entity
    @dataclass
    class Point:
        value: Annotated[int, PlanningVariable]
        is_pinned: Annotated[bool, PlanningPin] = field(default=False)

    @planning_solution
    @dataclass
    class Solution:
        values: Annotated[List[int], ValueRangeProvider]
        points: Annotated[List[Point], PlanningEntityCollectionProperty]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Point)
                              .penalize(SimpleScore.ONE, lambda point: point.value)
                              .as_constraint('Minimize Value')
        ]

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Point],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            unimproved_spent_limit=Duration(milliseconds=100)
        )
    )
    problem: Solution = Solution([0, 1, 2],
                                 [
                                     Point(0),
                                     Point(1),
                                     Point(2, is_pinned=True)
                                 ])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == -2


def test_planning_pin_to_index():
    @planning_entity
    @dataclass
    class Point:
        value: Annotated[list[int], PlanningListVariable]
        unpinned_start: Annotated[int, PlanningPinToIndex] = field(default=0)

    @planning_solution
    @dataclass
    class Solution:
        values: Annotated[List[int], ValueRangeProvider]
        points: Annotated[List[Point], PlanningEntityCollectionProperty]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    def penalty_function(point: Point):
        penalty = 0
        for i in range(len(point.value)):
            penalty += point.value[i] * i
        return penalty

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Point)
            .penalize(SimpleScore.ONE, penalty_function)
            .as_constraint('Minimize Value')
        ]

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Point],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            unimproved_spent_limit=Duration(milliseconds=100)
        )
    )
    problem: Solution = Solution([0, 1, 2],
                                 [
                                     Point([0, 2], unpinned_start=2),
                                     Point([1]),
                                 ])
    solver = SolverFactory.create(solver_config).build_solver()
    solution = solver.solve(problem)
    assert solution.score.score == -2
