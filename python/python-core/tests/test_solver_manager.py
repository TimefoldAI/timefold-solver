import logging
import pytest
from dataclasses import dataclass, field
from timefold.solver import *
from timefold.solver.config import *
from timefold.solver.domain import *
from timefold.solver.score import *
from typing import Annotated, List


@pytest.mark.xfail(reason='Flaky test')
def test_solve():
    from threading import Lock, Semaphore
    lock = Lock()

    def get_lock(entity):
        lock.acquire()
        lock.release()
        return False

    @dataclass
    class Value:
        value: Annotated[int, PlanningId]

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[str, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None, compare=False)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
                              .filter(get_lock)
                              .reward(SimpleScore.ONE)
                              .as_constraint('Wait for lock'),
            constraint_factory.for_each(Entity)
                              .reward(SimpleScore.ONE, lambda entity: entity.value.value)
                              .as_constraint('Maximize Value'),
            constraint_factory.for_each_unique_pair(Entity,
                                                    Joiners.equal(lambda entity: entity.value.value))
                              .penalize(SimpleScore.of(12))
                              .as_constraint('Same Value'),
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[Value],
                              DeepPlanningClone,
                              ProblemFactCollectionProperty,
                              ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    class UseOnlyEntityAndValueProblemChange(ProblemChange[Solution]):
        def __init__(self, entity, value):
            self.entity = entity
            self.value = value

        def do_change(self, solution: Solution, problem_change_director: ProblemChangeDirector):
            problem_facts_to_remove = solution.value_list.copy()
            entities_to_remove = solution.entity_list.copy()
            for problem_fact in problem_facts_to_remove:
                problem_change_director.remove_problem_fact(problem_fact,
                                                            lambda value: solution.value_list.remove(value))
            for removed_entity in entities_to_remove:
                problem_change_director.remove_entity(removed_entity,
                                                      lambda entity: solution.entity_list.remove(entity))
            problem_change_director.add_entity(self.entity, lambda entity: solution.entity_list.append(entity))
            problem_change_director.add_problem_fact(self.value, lambda value: solution.value_list.append(value))

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='6'
        )
    )
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [Value(1), Value(2), Value(3)],
                                 SimpleScore.ONE)

    def assert_solver_run(solver_manager, solver_job):
        assert solver_manager.get_solver_status(1) != SolverStatus.NOT_SOLVING
        lock.release()
        solution = solver_job.get_final_best_solution()
        assert solution.score.score == 6
        value_list = [entity.value.value for entity in solution.entity_list]
        assert 1 in value_list
        assert 2 in value_list
        assert 3 in value_list
        assert solver_manager.get_solver_status(1) == SolverStatus.NOT_SOLVING

    def assert_problem_change_solver_run(solver_manager, solver_job):
        assert solver_manager.get_solver_status(1) != SolverStatus.NOT_SOLVING
        solver_manager.add_problem_change(1, UseOnlyEntityAndValueProblemChange(Entity('D'), Value(6)))
        lock.release()
        solution = solver_job.get_final_best_solution()
        assert solution.score.score == 6
        assert len(solution.entity_list) == 1
        assert len(solution.value_list) == 1
        assert solution.entity_list[0].code == 'D'
        assert solution.entity_list[0].value.value == 6
        assert solution.value_list[0].value == 6
        assert solver_manager.get_solver_status(1) == SolverStatus.NOT_SOLVING


    with SolverManager.create(solver_config, SolverManagerConfig(parallel_solver_count='AUTO')) as solver_manager:
        lock.acquire()
        solver_job = solver_manager.solve(1, problem)
        assert_solver_run(solver_manager, solver_job)

        lock.acquire()
        solver_job = solver_manager.solve(1, problem)
        assert_problem_change_solver_run(solver_manager, solver_job)

        def get_problem(problem_id):
            assert problem_id == 1
            return problem

        lock.acquire()
        solver_job = (solver_manager.solve_builder()
                      .with_problem_id(1)
                      .with_problem_finder(get_problem)).run()
        assert_solver_run(solver_manager, solver_job)

        solution_list = []
        semaphore = Semaphore(0)

        def on_best_solution_changed(solution):
            solution_list.append(solution)
            semaphore.release()

        lock.acquire()
        solver_job = (solver_manager.solve_builder()
                      .with_problem_id(1)
                      .with_problem_finder(get_problem)
                      .with_best_solution_consumer(on_best_solution_changed)
                      ).run()
        assert_solver_run(solver_manager, solver_job)
        assert semaphore.acquire(timeout=1)
        assert len(solution_list) == 1

        solution_list = []
        lock.acquire()
        solver_job = (solver_manager.solve_builder()
                      .with_problem_id(1)
                      .with_problem_finder(get_problem)
                      .with_best_solution_consumer(on_best_solution_changed)
                      .with_final_best_solution_consumer(on_best_solution_changed)
                      ).run()
        assert_solver_run(solver_manager, solver_job)

        # Wait for 2 acquires, one for best solution consumer,
        # another for final best solution consumer
        assert semaphore.acquire(timeout=1)
        assert semaphore.acquire(timeout=1)
        assert len(solution_list) == 2


@pytest.mark.filterwarnings("ignore:.*Exception in thread.*:pytest.PytestUnhandledThreadExceptionWarning")
def test_error():
    @dataclass
    class Value:
        value: Annotated[int, PlanningId]

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[str, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
                              .filter(lambda e: e.missing_attribute == 1)
                              .reward(SimpleScore.ONE, lambda entity: entity.value.value)
                              .as_constraint('Maximize Value')
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[Value],
                              DeepPlanningClone,
                              ProblemFactCollectionProperty,
                              ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='6'
        )
    )
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [Value(1), Value(2), Value(3)],
                                 SimpleScore.ONE)
    with SolverManager.create(SolverFactory.create(solver_config)) as solver_manager:
        the_problem_id = None
        the_exception = None

        def my_exception_handler(problem_id, exception):
            nonlocal the_problem_id
            nonlocal the_exception
            the_problem_id = problem_id
            the_exception = exception

        try:
            (solver_manager.solve_builder()
             .with_problem_id(1)
             .with_problem(problem)
             .with_exception_handler(my_exception_handler)
             .run().get_final_best_solution())
        except:
            pass

        assert the_problem_id == 1
        assert the_exception is not None

        the_problem_id = None
        the_exception = None

        try:
            (solver_manager.solve_builder()
             .with_problem_id(1)
             .with_problem(problem)
             .with_best_solution_consumer(lambda solution: None)
             .with_exception_handler(my_exception_handler)
             .run().get_final_best_solution())
        except:
            pass

        assert the_problem_id == 1
        assert the_exception is not None


@pytest.mark.filterwarnings("ignore:.*Exception in thread.*:pytest.PytestUnhandledThreadExceptionWarning")
def test_default_error(caplog):
    @dataclass
    class Value:
        value: Annotated[int, PlanningId]

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[str, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .filter(lambda e: e.missing_attribute == 1)
            .reward(SimpleScore.ONE, lambda entity: entity.value.value)
            .as_constraint('Maximize Value')
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[Value],
        DeepPlanningClone,
        ProblemFactCollectionProperty,
        ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='6'
        )
    )
    problem: Solution = Solution([Entity('A'), Entity('B'), Entity('C')], [Value(1), Value(2), Value(3)],
                                 SimpleScore.ONE)
    with SolverManager.create(SolverFactory.create(solver_config)) as solver_manager:
        with caplog.at_level(logging.ERROR, logger="timefold.solver"):
            try:
                (solver_manager.solve_builder()
                 .with_problem_id(1)
                 .with_problem(problem)
                 .run().get_final_best_solution())
            except:
                pass

        assert len(caplog.records) == 1
        error_msg = str(caplog.records[0].exc_info[1])
        assert 'AttributeError' in error_msg
        assert 'e.missing_attribute == 1' in error_msg


def test_solver_config():
    @dataclass
    class Value:
        value: Annotated[int, PlanningId]

    @planning_entity
    @dataclass
    class Entity:
        code: Annotated[str, PlanningId]
        value: Annotated[Value, PlanningVariable] = field(default=None)

    @constraint_provider
    def my_constraints(constraint_factory: ConstraintFactory):
        return [
            constraint_factory.for_each(Entity)
            .reward(SimpleScore.ONE, lambda entity: entity.value.value)
            .as_constraint('Maximize Value')
        ]

    @planning_solution
    @dataclass
    class Solution:
        entity_list: Annotated[List[Entity], PlanningEntityCollectionProperty]
        value_list: Annotated[List[Value],
        DeepPlanningClone,
        ProblemFactCollectionProperty,
        ValueRangeProvider]
        score: Annotated[SimpleScore, PlanningScore] = field(default=None)

    solver_config = SolverConfig(
        solution_class=Solution,
        entity_class_list=[Entity],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=my_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='9'
        )
    )
    problem: Solution = Solution([Entity('A')], [Value(1), Value(2), Value(3)],
                                 SimpleScore.ONE)
    first_initialized_solution_consumer_called = []
    solver_job_started_consumer_called = []

    def on_first_initialized_solution_consumer(solution):
        first_initialized_solution_consumer_called.append(solution)

    def on_solver_job_started_consumer(solution):
        solver_job_started_consumer_called.append(solution)

    with SolverManager.create(SolverFactory.create(solver_config)) as solver_manager:
        solver_job = (solver_manager.solve_builder()
                      .with_problem_id(1)
                      .with_problem(problem)
                      .with_config_override(SolverConfigOverride(
                           termination_config=TerminationConfig(
                               best_score_limit='3'
                           )
                      ))
                      .with_first_initialized_solution_consumer(on_first_initialized_solution_consumer)
                      .with_solver_job_started_consumer(on_solver_job_started_consumer)
                      .run())

        solution = solver_job.get_final_best_solution()
        assert solution.score.score == 3
        assert len(first_initialized_solution_consumer_called) == 1
        assert len(solver_job_started_consumer_called) == 1