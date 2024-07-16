from timefold.solver import *
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.heuristic import *
from timefold.solver.score import *

import pytest
import re
from typing import Annotated, List
from dataclasses import dataclass, field


@planning_entity
@dataclass
class Entity:
    value: Annotated[str, PlanningVariable] = field(default=None)


@planning_solution
@dataclass
class Solution:
    entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
    value_list: Annotated[List[str], ValueRangeProvider]
    score: Annotated[SimpleScore, PlanningScore] = field(default=None)


@constraint_provider
def my_constraints(constraint_factory: ConstraintFactory):
    return [
        constraint_factory.for_each(Entity)
            .penalize(SimpleScore.ONE, lambda entity: 'TEN')  # noqa
            .as_constraint('Penalize each entity')
    ]


def test_non_planning_solution_being_passed_to_solve():
    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        )
    )
    solver = SolverFactory.create(solver_config).build_solver()
    with pytest.raises(ValueError, match=re.escape(
            f'The problem ({10}) is not an instance of the @planning_solution class'
    )):
        solver.solve(10)


def test_none_passed_to_solve():
    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        )
    )
    solver = SolverFactory.create(solver_config).build_solver()
    with pytest.raises(ValueError, match=re.escape(
            f'The problem ({None}) is not an instance of the @planning_solution class'
    )):
        solver.solve(None)


def test_bad_return_type():
    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            spent_limit=Duration(milliseconds=100)
        )
    )

    problem = Solution([Entity()], ['1', '2', '3'])
    solver = SolverFactory.create(solver_config).build_solver()
    with pytest.raises(RuntimeError):
        solver.solve(problem)


def test_non_proxied_class_passed():
    class NonProxied:
        pass

    with pytest.raises(TypeError, match=re.escape(
            f'is not a @planning_solution class'
    )):
        solver_config = SolverConfig(
            solution_class=NonProxied
        )._to_java_solver_config()


def test_non_proxied_function_passed():
    def not_proxied():
        pass

    with pytest.raises(TypeError, match=re.escape(
            f'is not a @constraint_provider function')):
        solver_config = SolverConfig(
            score_director_factory_config=ScoreDirectorFactoryConfig(
                constraint_provider_function=not_proxied  # noqa
            )
        )._to_java_solver_config()


def test_missing_enterprise():
    with pytest.raises(RequiresEnterpriseError, match=re.escape('multithreaded solving')):
        solver_config = SolverConfig(
            move_thread_count=MoveThreadCount.AUTO
        )._to_java_solver_config()

    @nearby_distance_meter
    def my_distance_meter(entity: Entity, value: str) -> float:
        return 0.0

    with pytest.raises(RequiresEnterpriseError, match=re.escape('nearby selection')):
        solver_config = SolverConfig(
            nearby_distance_meter_function=my_distance_meter
        )._to_java_solver_config()
