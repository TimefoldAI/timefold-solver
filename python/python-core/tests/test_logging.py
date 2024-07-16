import logging
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


def assert_in_logs(log_messages: list[str], message: str):
    for log_message in log_messages:
        if message in log_message:
            return
    raise AssertionError(f'Expected message {message} in {log_messages}, but it was not found.')

def test_log_events_are_forwarded(caplog):
    with caplog.at_level(logging.INFO, logger="timefold.solver"):
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
        solver.solve(problem)
        log_messages = [rec.message for rec in caplog.records]
        # INFO
        assert_in_logs(log_messages, 'Solving started')
        # INFO
        assert_in_logs(log_messages, 'Solving ended')


def test_support_changing_log_levels(caplog):
    with caplog.at_level(logging.DEBUG, logger="timefold.solver"):
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
        solver.solve(problem)
        log_messages = [rec.message for rec in caplog.records]
        # INFO
        assert_in_logs(log_messages, 'Solving started')
        # DEBUG
        assert_in_logs(log_messages, 'CH step (0)')
        # INFO
        assert_in_logs(log_messages, 'Solving ended')


def test_support_trace_logs(caplog):
    # Python has no logging.TRACE, logging.DEBUG = 10, so TRACE = 5
    with caplog.at_level(5, logger="timefold.solver"):
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
        solver.solve(problem)
        log_messages = [rec.message for rec in caplog.records]
        # INFO
        assert_in_logs(log_messages, 'Solving started')
        # DEBUG
        assert_in_logs(log_messages, 'CH step (0)')
        # TRACE/5
        assert_in_logs(log_messages, 'Move index (1)')
        # INFO
        assert_in_logs(log_messages, 'Solving ended')
