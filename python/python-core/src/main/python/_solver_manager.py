from ._problem_change import ProblemChange, ProblemChangeWrapper
from .config import SolverConfig, SolverConfigOverride, SolverManagerConfig
from ._solver_factory import SolverFactory
from ._future import wrap_future
from ._timefold_java_interop import update_log_level

from typing import Awaitable, TypeVar, Generic, Callable, TYPE_CHECKING
from datetime import timedelta
from enum import Enum

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from ai.timefold.solver.core.api.solver import (SolverManager as _JavaSolverManager,
                                                    SolverJob as _JavaSolverJob,
                                                    SolverJobBuilder as _JavaSolverJobBuilder)

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')


class SolverStatus(Enum):
    """
    The status of the problem submitted to the SolverManager.
    Retrieve this status with `SolverManager.get_solver_status` or
    `SolverJob.get_solver_status`.
    """

    NOT_SOLVING = 'NOT_SOLVING'
    """
    The problem's solving has terminated or the problem was never submitted to the `SolverManager`.
    `SolverManager.get_solver_status` cannot tell the difference, but `SolverJob.get_solver_status` can.
    """

    SOLVING_SCHEDULED = 'SOLVING_SCHEDULED'
    """
    No solver thread started solving this problem yet, but sooner or later a solver thread will solve it.
    For example, submitting 7 problems to a `SolverManager` with a `SolverManagerConfig.parallel_solver_count` of 4,
    puts 3 into this state for non-trivial amount of time.
    
    Transitions into `SOLVING_ACTIVE` (or `NOT_SOLVING` if it is terminated early, before it starts).
    """

    SOLVING_ACTIVE = 'SOLVING_ACTIVE'
    """
    A solver thread started solving the problem, but hasn't finished yet.
    If CPU resource are scarce and that solver thread is waiting for CPU time,
    the state doesn't change, it's still considered solving active.
    Transitions into `NOT_SOLVING` when terminated.
    """

    @staticmethod
    def _from_java_enum(enum_value):
        return getattr(SolverStatus, enum_value.name())


class SolverJob(Generic[Solution_, ProblemId_]):
    """
    Represents a problem that has been submitted to solve on the SolverManager.
    """
    _delegate: '_JavaSolverJob'

    def __init__(self, delegate: '_JavaSolverJob'):
        self._delegate = delegate

    def get_problem_id(self) -> ProblemId_:
        """
        A value given to `SolverManager.solve`, `SolverManager.solve_and_listen` or
        `SolverJobBuilder.with_problem_id`.

        Returns
        -------
        ProblemId_
            The problem id corresponding to this `SolverJob`.
        """
        from _jpyinterpreter import unwrap_python_like_object
        return unwrap_python_like_object(self._delegate.getProblemId())

    def get_solver_status(self) -> SolverStatus:
        """
        Returns whether the `Solver` is scheduled to solve, actively solving or not.
        Returns `SolverStatus.NOT_SOLVING` if the solver already terminated.

        Returns
        -------
        SolverStatus
            The `SolverStatus` for this `SolverJob`.
        """
        return SolverStatus._from_java_enum(self._delegate.getSolverStatus())

    def get_solving_duration(self) -> timedelta:
        """
        Returns the duration spent solving since the last start.
        If it hasn't started it yet, it returns ``timedelta(0)``.
        If it hasn't ended yet, it returns the time between the last start and now.
        If it has ended already, it returns the time between the last start and the ending.

        Returns
        -------
        timedelta
            The duration spent solving since the last (re)start, at least 0.
        """
        return timedelta(milliseconds=self._delegate.getSolvingDuration().toMillis())

    def get_final_best_solution(self) -> Solution_:
        """
        Waits if necessary for the solver to complete and then returns the final best `planning_solution`.

        Returns
        -------
        Solution_
            Never ``None``, but it could be the original uninitialized problem.
        """
        from _jpyinterpreter import unwrap_python_like_object
        return unwrap_python_like_object(self._delegate.getFinalBestSolution())

    def terminate_early(self) -> None:
        """
        Terminates the solver or cancels the solver job if it hasn't (re) started yet.
        Does nothing if the solver already terminated.

        Waits for the termination or cancellation to complete before returning.
        During termination, a best_solution_consumer could still be called.
        When the solver terminates, the final_best_solution_consumer is executed with the latest best solution.
        These consumers run on a consumer thread independently of the termination
        and may still run even after this method returns.
        """
        self._delegate.terminateEarly()

    def is_terminated_early(self) -> bool:
        """
        Checks if `terminate_early` has been called on this `SolverJob`.

        Returns
        -------
        bool
            ``True`` if `terminate_early` has been called since the underlying `Solver` started solving.
        """
        return self._delegate.isTerminatedEarly()

    def add_problem_change(self, problem_change: ProblemChange[Solution_]) -> Awaitable[None]:
        """
        Schedules a `ProblemChange` to be processed by the underlying `Solver` and returns immediately.
        To learn more about problem change semantics, please refer to the `ProblemChange` docstring.

        Parameters
        ----------
        problem_change : ProblemChange
            The `ProblemChange` to be processed.

        Returns
        -------
        Awaitable
            An awaitable that completes after the best solution containing this change has been consumed.
        """
        return wrap_future(self._delegate.addProblemChange(ProblemChangeWrapper(problem_change)))


class SolverJobBuilder(Generic[Solution_, ProblemId_]):
    """
    Provides a fluent contract that allows customization and submission of planning problems to solve.
    A `SolverManager` can solve multiple planning problems and can be used across different threads.

    Hence, it is possible to have multiple distinct build configurations
    that are scheduled to run by the `SolverManager` instance.

    To solve a planning problem, set the problem configuration:
    `with_problem_id`, `with_problem_finder` and `with_problem`.

    Then solve it by calling `run`.
    """
    _delegate: '_JavaSolverJobBuilder'

    def __init__(self, delegate: '_JavaSolverJobBuilder'):
        self._delegate = delegate

    def with_problem_id(self, problem_id: ProblemId_) -> 'SolverJobBuilder':
        """
        Sets the problem id.

        Parameters
        ----------
        problem_id : ProblemId_
            A ID for each planning problem. This must be unique.

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        return SolverJobBuilder(self._delegate.withProblemId(convert_to_java_python_like_object(problem_id)))

    def with_problem(self, problem: Solution_) -> 'SolverJobBuilder':
        """
        Sets the problem definition.

        Parameters
        ----------
        problem : Solution_
            A `planning_solution`, usually with uninitialized planning variables

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        return SolverJobBuilder(self._delegate.withProblem(convert_to_java_python_like_object(problem)))

    def with_config_override(self, config_override: SolverConfigOverride) -> 'SolverJobBuilder':
        """
        Sets the solver config override.

        Parameters
        ----------
        config_override : SolverConfigOverride
            Allows overriding the default behavior of Solver

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        return SolverJobBuilder(self._delegate.withConfigOverride(config_override._to_java_solver_config_override()))

    def with_problem_finder(self, problem_finder: Callable[[ProblemId_], Solution_]) -> 'SolverJobBuilder':
        """
        Sets the mapping function to the problem definition.

        Parameters
        ----------
        problem_finder : Callable[[ProblemId_], Solution_]
            A function that returns a `planning_solution`, usually with uninitialized planning variables

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        from java.util.function import Function
        from _jpyinterpreter import convert_to_java_python_like_object, unwrap_python_like_object
        java_finder = Function @ (lambda problem_id: convert_to_java_python_like_object(
            problem_finder(unwrap_python_like_object(problem_id))))
        return SolverJobBuilder(self._delegate.withProblemFinder(java_finder))

    def with_best_solution_consumer(self, best_solution_consumer: Callable[[Solution_], None]) -> 'SolverJobBuilder':
        """
        Sets the best solution consumer, which may be called multiple times during the solving process.

        Parameters
        ----------
        best_solution_consumer : Callable[[Solution_], None]
            Called multiple times for each new best solution on a consumer thread

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        from java.util.function import Consumer
        from _jpyinterpreter import unwrap_python_like_object

        java_consumer = Consumer @ (lambda solution: best_solution_consumer(unwrap_python_like_object(solution)))
        return SolverJobBuilder(self._delegate.withBestSolutionConsumer(java_consumer))

    def with_final_best_solution_consumer(self, final_best_solution_consumer: Callable[[Solution_], None]) -> 'SolverJobBuilder':
        """
        Sets the final best solution consumer,
        which is called at the end of the solving process and returns the final best solution.

        Parameters
        ----------
        final_best_solution_consumer : Callable[[Solution_], None]
            Called only once at the end of the solving process on a consumer thread

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        from java.util.function import Consumer
        from _jpyinterpreter import unwrap_python_like_object

        java_consumer = Consumer @ (lambda solution: final_best_solution_consumer(unwrap_python_like_object(solution)))
        return SolverJobBuilder(
            self._delegate.withFinalBestSolutionConsumer(java_consumer))

    def with_exception_handler(self, exception_handler: Callable[[ProblemId_, Exception], None]) -> 'SolverJobBuilder':
        """
        Sets the custom exception handler.

        Parameters
        ----------
        exception_handler : Callable[[ProblemId_, Exception], None]
             Called if an exception or error occurs.
             If not present, it defaults to logging the exception as an error.

        Returns
        -------
        SolverJobBuilder
            This `SolverJobBuilder`.
        """
        from java.util.function import BiConsumer
        from _jpyinterpreter import unwrap_python_like_object

        java_consumer = BiConsumer @ (lambda problem_id, error: exception_handler(unwrap_python_like_object(problem_id),
                                                                                  error))
        return SolverJobBuilder(
            self._delegate.withExceptionHandler(java_consumer))

    def run(self) -> SolverJob[Solution_, ProblemId_]:
        """
        Submits a planning problem to solve and returns immediately.
        The planning problem is solved on a solver thread, as soon as one is available.

        Returns
        -------
        SolverJob
            The `SolverJob` built from this `SolverJobBuilder`.
        """
        update_log_level()
        return SolverJob(self._delegate.run())


class SolverManager(Generic[Solution_, ProblemId_]):
    """
    A `SolverManager` solves multiple planning problems of the same domain,
    asynchronously without blocking the calling thread.
    To create a `SolverManager`, use `create`.
    To solve a planning problem, call `solve`, `solve_and_listen` or `solve_builder`.

    These methods are thread-safe unless explicitly stated otherwise.

    Internally a `SolverManager` manages a thread pool of solver threads (which call `Solver.solve`)
    and consumer threads (to handle the `BestSolutionChangedEvents`).

    To learn more about problem change semantics, please refer to the `ProblemChange` Javadoc.
    """
    _delegate: '_JavaSolverManager'

    def __init__(self, delegate: '_JavaSolverManager'):
        self._delegate = delegate

    @staticmethod
    def create(solver_factory_or_config: 'SolverConfig | SolverFactory[Solution_]',
               solver_manager_config: 'SolverManagerConfig' = None) -> 'SolverManager[Solution_, ProblemId_]':
        """
        Use a `SolverConfig` or `SolverFactory` to build a `SolverManager`.

        Parameters
        ----------
        solver_factory_or_config : SolverConfig | SolverFactory[Solution_]
            The `SolverConfig` or `SolverFactory` to build the `SolverManager` from.

        solver_manager_config: SolverManagerConfig, optional
            Additional settings that can be used to configure the `SolverManager`.

        Returns
        -------
        SolverManager
            A new `SolverManager` instance.
        """
        from ai.timefold.solver.core.api.solver import SolverManager as JavaSolverManager
        from ai.timefold.solver.python import DaemonThreadFactory

        if solver_manager_config is None:
            solver_manager_config = SolverManagerConfig()

        java_solver_manager_config = solver_manager_config._to_java_solver_manager_config()  # noqa
        java_solver_manager_config.setThreadFactoryClass(DaemonThreadFactory.class_)

        if isinstance(solver_factory_or_config, SolverConfig):
            solver_factory_or_config = SolverFactory.create(solver_factory_or_config)

        return SolverManager(JavaSolverManager.create(solver_factory_or_config._delegate,  # noqa
                                                      java_solver_manager_config))

    def solve(self, problem_id: ProblemId_, problem: Solution_,
              final_best_solution_listener: Callable[[Solution_], None] = None) -> SolverJob[Solution_, ProblemId_]:
        """
        Submits a planning problem to solve and returns immediately.
        The planning problem is solved on a solver Thread, as soon as one is available.
        To retrieve the final best solution, use `SolverJob.get_final_best_solution`.
        In server applications, it's recommended to set `final_best_solution_listener`,
        to avoid loading the problem going stale if solving can't start immediately.
        To listen to intermediate best solutions too, use `solve_and_listen` instead.

        Defaults to logging exceptions as an error.

        To stop a solver job before it naturally terminates, call `SolverJob.terminate_early`.

        Parameters
        ----------
        problem_id : ProblemId_
            A ID for each planning problem.
            This must be unique.
            Use this problemId to terminate the solver early,
            to get the status or if the problem changes while solving.

        problem : Solution_
            A `planning_solution` usually with uninitialized planning variables

        final_best_solution_listener : Callable[[Solution_], None], optional
            Called only once, at the end, on a consumer thread

        Returns
        -------
        SolverJob
            A new `SolverJob`.
        """
        builder = (self.solve_builder()
                   .with_problem_id(problem_id)
                   .with_problem(problem))

        if final_best_solution_listener is not None:
            builder = builder.with_final_best_solution_consumer(final_best_solution_listener)

        return builder.run()

    def solve_and_listen(self, problem_id: ProblemId_, problem: Solution_, listener: Callable[[Solution_], None]) \
            -> SolverJob[Solution_, ProblemId_]:
        """
        Submits a planning problem to solve and returns immediately.
        The planning problem is solved on a solver thread, as soon as one is available.
        When the solver finds a new best solution, the `best_solution_consumer` is called every time,
        on a consumer thread, as soon as one is available (taking into account any throttling waiting time),
        unless a newer best solution is already available by then (in which case skip ahead discards it).

        Defaults to logging exceptions as an error.

        To stop a solver job before it naturally terminates, call `terminate_early`.

        Parameters
        ----------
        problem_id : ProblemId_
            A ID for each planning problem.
            This must be unique.
            Use this problemId to terminate the solver early,
            to get the status or if the problem changes while solving.

        problem: Solution_
            A `planning_solution` usually with uninitialized planning variables.

        listener : Callable[[Solution_], None]
            Called multiple times, on a consumer thread.

        Returns
        -------
        SolverJob
            A new `SolverJob`.
        """
        return (self.solve_builder()
                .with_problem_id(problem_id)
                .with_problem(problem)
                .with_best_solution_consumer(listener)
                .run())

    def solve_builder(self) -> SolverJobBuilder[Solution_, ProblemId_]:
        """
        Creates a `SolverJobBuilder` that allows to customize and submit a planning problem to solve.

        Returns
        -------
        SolverJobBuilder
            A new `SolverJobBuilder`.
        """
        return SolverJobBuilder(self._delegate.solveBuilder())

    def get_solver_status(self, problem_id: ProblemId_) -> SolverStatus:
        """
        Returns if the Solver is scheduled to solve, actively solving or not.
        Returns `SolverStatus.NOT_SOLVING` if the solver already terminated or if the `problem_id` was never added.
        To distinguish between both cases, use `SolverJob.get_solver_status` instead.
        Here, that distinction is not supported because it would cause a memory leak.

        Parameters
        ----------
        problem_id : ProblemId_
            A value given to `SolverManager.solve`, `SolverManager.solve_and_listen` or
            `SolverJobBuilder.with_problem_id`.

        Returns
        -------
        SolverStatus
            The `SolverStatus` corresponding to `problem_id`.
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        return SolverStatus._from_java_enum(self._delegate.getSolverStatus(
            convert_to_java_python_like_object(problem_id)))

    def terminate_early(self, problem_id: ProblemId_) -> None:
        """
        Terminates the solver or cancels the solver job if it hasn't (re)started yet.
        Does nothing if the solver already terminated or the `problem_id` was never added.
        To distinguish between both cases, use `SolverJob.terminate_early` instead.
        Here, that distinction is not supported because it would cause a memory leak.

        Waits for the termination or cancellation to complete before returning.
        During termination, a `best_solution_consumer` could still be called.
        When the solver terminates, the `final_best_solution_consumer` is executed with the latest best solution.
        These consumers run on a consumer thread independently of the termination
        and may still run even after this method returns.

        Parameters
        ----------
        problem_id : ProblemId_
            A value given to `SolverManager.solve`, `SolverManager.solve_and_listen` or
            `SolverJobBuilder.with_problem_id`.
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        self._delegate.terminateEarly(convert_to_java_python_like_object(problem_id))

    def add_problem_change(self, problem_id: ProblemId_, problem_change: ProblemChange[Solution_]) -> Awaitable[None]:
        """
        Schedules a `ProblemChange` to be processed by the underlying `Solver` and returns immediately.
        If the solver already terminated or the `problem_id` was never added, throws an exception.
        The same applies if the underlying `Solver` is not in the `SolverStatus.SOLVING_ACTIVE` state.

        Parameters
        ----------
        problem_id : ProblemId_
            A value given to `SolverManager.solve`, `SolverManager.solve_and_listen` or
            `SolverJobBuilder.with_problem_id`.
        problem_change : ProblemChange
            A problem change to be processed by the underlying `Solver`.

        Returns
        -------
        Awaitable
            An awaitable that completes after the best solution containing this change has been consumed.
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        return wrap_future(self._delegate.addProblemChange(convert_to_java_python_like_object(problem_id),
                                                                       ProblemChangeWrapper(problem_change)))

    def close(self) -> None:
        """
        Terminates all solvers,
        cancels all solver jobs that haven't (re)started yet and discards all queued ProblemChanges.
        Releases all thread pool resources.

        No new planning problems can be submitted after calling this method.
        """
        self._delegate.close()

    def __enter__(self) -> 'SolverManager[Solution_, ProblemId_]':
        """
        Returns self, so it can be used as a context manager.

        Returns
        -------
        SolverManager
            This `SolverManager`.
        """
        return self

    def __exit__(self, exc_type, exc_val, exc_tb) -> None:
        """
        Calls `close` to release resources associated with this `SolverManager`.
        """
        self._delegate.close()


__all__ = ['SolverManager', 'SolverJobBuilder', 'SolverJob', 'SolverStatus']
