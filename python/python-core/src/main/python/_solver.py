from ._problem_change import ProblemChange, ProblemChangeWrapper
from ._timefold_java_interop import update_log_level
from ._jpype_type_conversions import to_python_score
from typing import TypeVar, TYPE_CHECKING, Generic, Callable
from datetime import timedelta
from jpype import JClass, JImplements, JOverride
from dataclasses import dataclass

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from .score import Score
    from ai.timefold.solver.core.api.solver import Solver as _JavaSolver

Solution_ = TypeVar('Solution_')


@dataclass
class BestSolutionChangedEvent(Generic[Solution_]):
    """
    Delivered when the best solution changes during solving.
    Delivered in the solver thread (which is the thread that calls `Solver.solve`).

    Attributes
    ----------
    new_best_score: Score
        Returns the Score of the `new_best_solution`.
        This is useful for generic code,
        which doesn't know the type of the `planning_solution` to retrieve the Score
        from `new_best_solution` easily.

    new_best_solution : Solution_
        Note that:

        - In real-time planning, not all ProblemChanges might be processed:
          check `is_every_problem_change_processed`.

        - This `planning_solution` might be uninitialized: check `Score.init_score`.

        - This `planning_solution` might be infeasible: check `Score.is_feasible`.

    is_every_problem_change_processed : bool
        Checks if all scheduled ProblemChanges have been processed.
        This method is thread-safe.

    time_spent: timedelta
        The duration between starting solving and finding the current best solution.
    """
    new_best_score: 'Score'
    new_best_solution: Solution_
    is_every_problem_change_processed: bool
    time_spent: timedelta


class Solver(Generic[Solution_]):
    """
    A `Solver` solves a planning problem and returns the best solution found.
    It's recommended to create a new `Solver` instance for each dataset.

    To create a `Solver`, use `SolverFactory.build_solver`.
    To solve a planning problem, call `solve`.
    To solve a planning problem without blocking the current thread, use `SolverManager` instead.

    These methods are not thread-safe and should be called from the same thread,
    except for the methods that are explicitly marked as thread-safe.
    Note that despite that solve is not thread-safe for clients of this class,
    that method is free to do multithreading inside itself.
    """
    _delegate: '_JavaSolver'
    _solution_class: JClass
    _has_event_listener: bool
    _event_listener_list: list[Callable[[BestSolutionChangedEvent[Solution_]], None]]

    def __init__(self, delegate: '_JavaSolver', solution_class: JClass):
        self._delegate = delegate
        self._solution_class = solution_class
        self._has_event_listener = False
        self._event_listener_list = []

    def solve(self, problem: Solution_):
        """
        Solves the planning problem and returns the best solution encountered
        (which might or might not be optimal, feasible or even initialized).

        It can take seconds, minutes, even hours or days before this method returns,
        depending on the termination configuration.
        To terminate a `Solver` early, call `terminate_early`.

        Parameters
        ----------
        problem : Solution_
            A `planning_solution`, usually its planning variables are uninitialized

        Returns
        -------
        Solution_
            The best solution encountered before terminating.
            It can return the original, uninitialized `planning_solution` with a ``None`` `Score`.
        """
        from java.lang import Exception as JavaException
        from ai.timefold.jpyinterpreter.types.errors import PythonBaseException
        from _jpyinterpreter import convert_to_java_python_like_object, unwrap_python_like_object
        java_problem = convert_to_java_python_like_object(problem)
        if not self._solution_class.isInstance(java_problem):
            raise ValueError(
                f'The problem ({problem}) is not an instance of the @planning_solution class ({self._solution_class})'
            )
        update_log_level()
        try:
            java_solution = self._delegate.solve(java_problem)
        except PythonBaseException as e:
            raise unwrap_python_like_object(e)
        except JavaException as e:
            raise RuntimeError(f'Solving failed due to an error: {e.getMessage()}.\n'
                               f'Java stack trace: {e.stacktrace()}') from e
        return unwrap_python_like_object(java_solution)

    def is_solving(self) -> bool:
        """
        This method is thread-safe.

        Returns
        -------
        bool
            ``True`` if the solve method is still running
        """
        return self._delegate.isSolving()

    def terminate_early(self) -> bool:
        """
        Notifies the solver that it should stop at its earliest convenience.
        This method returns immediately, but it takes an undetermined time for the `solve` to actually return.

        If the solver is running in daemon mode, this is the only way to terminate it normally.

        This method is thread-safe.
        It can only be called from a different thread because the original thread is still calling `solve`.

        Returns
        -------
        bool
            ``True`` if successful, ``False`` if was already terminating or terminated
        """
        return self._delegate.terminateEarly()

    def is_terminate_early(self) -> bool:
        """
        This method is thread-safe.

        Returns
        -------
        bool
            ``True`` if `terminate_early` has been called since the `Solver` started.
        """
        return self._delegate.isTerminateEarly()

    def add_problem_change(self, problem_change: ProblemChange[Solution_]) -> None:
        """
        Schedules a `ProblemChange` to be processed.
        As a side effect, this restarts the `Solver`, effectively resetting all Terminations, but not `terminate_early`.
        This method is thread-safe.
        Follow specifications of `queue.Queue.put` with by default a maxsize of 0.
        To learn more about problem change semantics, please refer to the `ProblemChange` docstring.

        Parameters
        ----------
        problem_change : ProblemChange
             A `ProblemChange` to be processed.

        See Also
        --------
        add_problem_changes
        """
        self._delegate.addProblemChange(ProblemChangeWrapper(problem_change))  # noqa

    def add_problem_changes(self, problem_changes: list[ProblemChange[Solution_]]) -> None:
        """
        Schedules multiple `ProblemChange`s to be processed.
        As a side effect, this restarts the `Solver`, effectively resetting all Terminations, but not `terminate_early`.
        This method is thread-safe.
        Follow specifications of `queue.Queue.put` with by default a maxsize of 0.
        To learn more about problem change semantics, please refer to the `ProblemChange` docstring.

        Parameters
        ----------
        problem_changes : list[ProblemChange]
             A list of `ProblemChange`s to be processed.

        See Also
        --------
        add_problem_change
        """
        self._delegate.addProblemChanges([ProblemChangeWrapper(problem_change) for problem_change in problem_changes])  # noqa

    def is_every_problem_change_processed(self) -> bool:
        """
        Checks if all scheduled `ProblemChange`s have been processed.
        This method is thread-safe.

        Returns
        -------
        bool
            ``True`` if there are no `ProblemChange`s left to do
        """
        return self._delegate.isEveryProblemChangeProcessed()

    def add_event_listener(self, event_listener: Callable[[BestSolutionChangedEvent[Solution_]], None]):
        """
        Adds a listener to be notified when a new best solution is found.

        Parameters
        ----------
        event_listener : Callable[[BestSolutionChangedEvent[Solution]], None]
            The listener to be notified when a new best solution is found.

        Examples
        --------
        >>> from timefold.solver import Solver, BestSolutionChangedEvent
        >>> from domain import Timetable, build_solver, generate_problem
        >>>
        >>> def best_solution_listener(event: BestSolutionChangedEvent[Timetable]) -> None:
        ...     print(event.new_best_score)
        ...
        >>> solver = build_solver()
        >>> solver.add_event_listener(best_solution_listener)
        >>> timetable = generate_problem()
        >>> solver.solve(timetable)
        """
        from ai.timefold.solver.core.api.solver.event import SolverEventListener
        event_listener_list = self._event_listener_list
        if not self._has_event_listener:
            @JImplements(SolverEventListener)
            class EventListener:
                @JOverride
                def bestSolutionChanged(self, event):
                    from _jpyinterpreter import unwrap_python_like_object
                    nonlocal event_listener_list
                    event = BestSolutionChangedEvent(
                        new_best_score=to_python_score(event.getNewBestScore()),
                        new_best_solution=unwrap_python_like_object(event.getNewBestSolution()),
                        is_every_problem_change_processed=event.isEveryProblemChangeProcessed(),
                        time_spent=timedelta(milliseconds=event.getTimeMillisSpent())
                    )
                    for listener in event_listener_list:
                        listener(event)

            self._has_event_listener = True
            self._delegate.addEventListener(EventListener())  # noqa

        event_listener_list.append(event_listener)

    def remove_event_listener(self, event_listener: Callable[[BestSolutionChangedEvent[Solution_]], None]):
        """
        Removes a listener added by `add_event_listener`.

        Parameters
        ----------
        event_listener : Callable[[BestSolutionChangedEvent[Solution]], None]
            The listener to be removed
        """
        self._event_listener_list.remove(event_listener)


__all__ = ['Solver', 'BestSolutionChangedEvent']
