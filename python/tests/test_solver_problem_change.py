from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from dataclasses import dataclass, field
from typing import Annotated, List
from threading import Thread


@planning_entity
@dataclass
class Entity:
    code: Annotated[str, PlanningId]
    value: Annotated[int, PlanningVariable] = field(default=None, compare=False)


@constraint_provider
def maximize_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(Entity)
        .reward(SimpleScore.ONE, lambda entity: entity.value)
        .as_constraint('Maximize value'),
    ]


@constraint_provider
def minimize_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(Entity)
        .penalize(SimpleScore.ONE, lambda entity: entity.value)
        .as_constraint('Minimize value'),
    ]


@planning_solution
@dataclass
class Solution:
    entities: Annotated[List[Entity], PlanningEntityCollectionProperty]
    value_range: Annotated[List[int], ValueRangeProvider]
    score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    def __str__(self) -> str:
        return str(self.entities)


class AddEntity(ProblemChange[Solution]):
    entity: Entity

    def __init__(self, entity: Entity):
        self.entity = entity

    def do_change(self, working_solution: Solution, problem_change_director: ProblemChangeDirector):
        problem_change_director.add_entity(self.entity,
                                           lambda working_entity: working_solution.entities.append(working_entity))


class RemoveEntity(ProblemChange[Solution]):
    entity: Entity

    def __init__(self, entity: Entity):
        self.entity = entity

    def do_change(self, working_solution: Solution, problem_change_director: ProblemChangeDirector):
        problem_change_director.remove_entity(self.entity,
                                              lambda working_entity: working_solution.entities.remove(working_entity))


def test_add_entity():
    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=maximize_constraints,
        ),
        termination_config=TerminationConfig(
            best_score_limit='6'
        )
    )

    problem: Solution = Solution([Entity('A')], [1, 2, 3])
    solver = SolverFactory.create(solver_config).build_solver()
    result: Solution | None = None

    def do_solve(problem: Solution):
        nonlocal solver, result
        result = solver.solve(problem)

    thread = Thread(target=do_solve, args=(problem,), daemon=True)

    thread.start()
    solver.add_problem_change(AddEntity(Entity('B')))
    thread.join(timeout=1)

    if thread.is_alive():
        raise AssertionError(f'Thread {thread} did not finish after 5 seconds')

    assert result is not None
    assert len(result.entities) == 2
    assert result.score.score == 6


def test_remove_entity():
    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=minimize_constraints,
        ),
        termination_config=TerminationConfig(
            best_score_limit='-1'
        )
    )

    problem: Solution = Solution([Entity('A'), Entity('B')], [1, 2, 3])
    solver = SolverFactory.create(solver_config).build_solver()
    result: Solution | None = None

    def do_solve(problem: Solution):
        nonlocal solver, result
        result = solver.solve(problem)

    thread = Thread(target=do_solve, args=(problem,), daemon=True)

    thread.start()
    solver.add_problem_change(RemoveEntity(Entity('B')))
    thread.join(timeout=1)

    if thread.is_alive():
        raise AssertionError(f'Thread {thread} did not finish after 5 seconds')

    assert result is not None
    assert len(result.entities) == 1
    assert result.score.score == -1
