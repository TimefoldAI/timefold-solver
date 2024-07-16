from abc import ABC, abstractmethod
from typing import TypeVar, Optional, Callable, TYPE_CHECKING, Generic
from types import FunctionType
from _jpyinterpreter import (convert_to_java_python_like_object,
                            unwrap_python_like_object,
                            update_python_object_from_java,
                            translate_python_bytecode_to_java_bytecode)
from jpype import JOverride, JImplements

if TYPE_CHECKING:
    from ai.timefold.solver.core.api.solver.change import (ProblemChangeDirector as _ProblemChangeDirector)

Solution_ = TypeVar('Solution_')


class ProblemChangeDirector:
    """
    Allows external changes to the working solution.
    If the changes are not applied through the `ProblemChangeDirector`,
    both internal and custom variable listeners are never notified about them,
    resulting to inconsistencies in the working solution.
    Should be used only from a `ProblemChange` implementation.

    To see an example implementation, please refer to the `ProblemChange` docstring.
    """
    _delegate: '_ProblemChangeDirector'
    _java_solution: Solution_
    _python_solution: Solution_

    Entity = TypeVar('Entity')
    ProblemFact = TypeVar('ProblemFact')
    EntityOrProblemFact = TypeVar('EntityOrProblemFact')

    def __init__(self, delegate: '_ProblemChangeDirector',
                 java_solution: Solution_,
                 python_solution: Solution_):
        self._delegate = delegate
        self._java_solution = java_solution
        self._python_solution = python_solution

    def _replace_solution_in_callable(self, callable: Callable):
        if isinstance(callable, FunctionType):
            if callable.__closure__ is not None:
                for cell in callable.__closure__:
                    if cell.cell_contents is self._python_solution:
                        cell.cell_contents = self._java_solution
        return callable

    def add_entity(self, entity: Entity, modifier: Callable[[Entity], None]) -> None:
        """
        Add a new ``planning_entity`` instance into the ``working solution``.

        Parameters
        ----------
        entity : Entity
            The ``planning_entity`` instance
        modifier : Callable[[Entity], None]
            A callable that adds the entity to the working solution.
        """
        from java.util.function import Consumer
        converted_modifier = translate_python_bytecode_to_java_bytecode(self._replace_solution_in_callable(modifier),
                                                                        Consumer)
        self._delegate.addEntity(convert_to_java_python_like_object(entity), converted_modifier)
        update_python_object_from_java(self._java_solution)

    def add_problem_fact(self, fact: ProblemFact, modifier: Callable[[ProblemFact], None]) -> None:
        """
        Add a new problem fact instance into the ``working solution``.

        Parameters
        ----------
        fact : ProblemFact
            The problem fact instance
        modifier : Callable[[ProblemFact], None]
            A callable that adds the fact to the working solution.
        """
        from java.util.function import Consumer
        converted_modifier = translate_python_bytecode_to_java_bytecode(self._replace_solution_in_callable(modifier),
                                                                        Consumer)
        self._delegate.addProblemFact(convert_to_java_python_like_object(fact), converted_modifier)
        update_python_object_from_java(self._java_solution)

    def change_problem_property(self, problem_fact_or_entity: EntityOrProblemFact,
                                modifier: Callable[[EntityOrProblemFact], None]) -> None:
        """
        Change a property of either a ``planning_entity`` or a problem fact.
        Translates the entity or the problem fact to its working solution counterpart
        by performing a lookup as defined by `lookup_working_object_or_fail`.

        Parameters
        ----------
        problem_fact_or_entity : EntityOrProblemFact
            The ``planning_entity`` or problem fact instance
        modifier : Callable[[EntityOrProblemFact], None]
            Updates the property of the ``planning_entity`` or the problem fact
        """
        from java.util.function import Consumer
        converted_modifier = translate_python_bytecode_to_java_bytecode(self._replace_solution_in_callable(modifier),
                                                                        Consumer)
        self._delegate.changeProblemProperty(convert_to_java_python_like_object(problem_fact_or_entity),
                                             converted_modifier)
        update_python_object_from_java(self._java_solution)

    def change_variable(self, entity: Entity, variable: str,
                        modifier: Callable[[Entity], None]) -> None:
        """
        Change a ``PlanningVariable`` value of a ``planning_entity``.
        Translates the entity to a working planning entity
        by performing a lookup as defined by `lookup_working_object_or_fail`.

        Parameters
        ----------
        entity : Entity
            The ``planning_entity`` instance
        variable : str
            Name of the ``PlanningVariable``
        modifier : Callable[[Entity], None]
            Updates the value of the ``PlanningVariable`` inside the ``planning_entity``
        """
        from java.util.function import Consumer
        converted_modifier = translate_python_bytecode_to_java_bytecode(self._replace_solution_in_callable(modifier),
                                                                        Consumer)
        self._delegate.changeVariable(convert_to_java_python_like_object(entity), variable, converted_modifier)
        update_python_object_from_java(self._java_solution)

    def lookup_working_object(self, external_object: EntityOrProblemFact) -> Optional[EntityOrProblemFact]:
        """
        As defined by `lookup_working_object_or_fail`,
        but doesn't fail fast if no working object was ever added for the `external_object`.
        It's recommended to use `lookup_working_object_or_fail` instead.

        Parameters
        ----------
        external_object : EntityOrProblemFact
            The entity or fact instance to lookup.
            Can be ``None``.

        Returns
        -------
        EntityOrProblemFact | None
            None if there is no working object for the `external_object`, the looked up object
            otherwise.

        Raises
        ------
        If it cannot be looked up or if the `external_object`'s class is not supported.
        """
        out = self._delegate.lookUpWorkingObject(convert_to_java_python_like_object(external_object)).orElse(None)
        if out is None:
            return None
        return unwrap_python_like_object(out)

    def lookup_working_object_or_fail(self, external_object: EntityOrProblemFact) -> EntityOrProblemFact:
        """
        Translate an entity or fact instance (often from another Thread)
        to this `ProblemChangeDirector`'s internal working instance.

        Matches entities by ``PlanningId``.

        Parameters
        ----------
        external_object : EntityOrProblemFact
            The entity or fact instance to lookup.
            Can be ``None``.

        Raises
        ------
        If there is no working object for `external_object`,
        if it cannot be looked up or if the `external_object`'s class is not supported.
        """
        return unwrap_python_like_object(self._delegate.lookUpWorkingObjectOrFail(external_object))

    def remove_entity(self, entity: Entity, modifier: Callable[[Entity], None]) -> None:
        """
        Remove an existing `planning_entity` instance from the ``working solution``.
        Translates the entity to its working solution counterpart
        by performing a lookup as defined by `lookup_working_object_or_fail`.

        Parameters
        ----------
        entity : Entity
            The ``planning_entity`` instance
        modifier : Callable[[Entity], None]
            Removes the working entity from the ``working solution``.
        """
        from java.util.function import Consumer
        converted_modifier = translate_python_bytecode_to_java_bytecode(self._replace_solution_in_callable(modifier),
                                                                        Consumer)
        self._delegate.removeEntity(convert_to_java_python_like_object(entity), converted_modifier)
        update_python_object_from_java(self._java_solution)

    def remove_problem_fact(self, fact: ProblemFact, modifier: Callable[[ProblemFact], None]) -> None:
        """
        Remove an existing problem fact instance from the ``working solution``.
        Translates the problem fact to its working solution counterpart
        by performing a lookup as defined by `lookup_working_object_or_fail`.

        Parameters
        ----------
        fact : ProblemFact
            The problem fact instance
        modifier : Callable[[ProblemFact], None]
            Removes the working problem fact from the ``working solution``.
        """
        from java.util.function import Consumer
        converted_modifier = translate_python_bytecode_to_java_bytecode(self._replace_solution_in_callable(modifier),
                                                                        Consumer)
        self._delegate.removeProblemFact(convert_to_java_python_like_object(fact), converted_modifier)
        update_python_object_from_java(self._java_solution)

    def update_shadow_variables(self) -> None:
        """
        Calls variable listeners on the external changes submitted so far.
        This happens automatically after the entire `ProblemChange` has been processed,
        but this method allows the user to specifically request it in the middle of the `ProblemChange`.
        """
        self._delegate.updateShadowVariables()
        update_python_object_from_java(self._java_solution)


class ProblemChange(Generic[Solution_], ABC):
    """
    A `ProblemChange` represents a change in one or more planning entities or problem facts of a `planning_solution`.

    The Solver checks the presence of waiting problem changes after every Move evaluation.
    If there are waiting problem changes, the Solver:

    1. clones the last best solution and sets the clone as the new working solution
    2. applies every problem change keeping the order in which problem changes have been submitted; after every problem change, variable listeners are triggered
    3. calculates the score and makes the updated working solution the new best solution; note that this solution is not published via the ai. timefold. solver. core. api. solver. event. BestSolutionChangedEvent, as it hasn't been initialized yet
    4. restarts solving to fill potential uninitialized planning entities

    Note that the Solver clones a `planning_solution` at will.
    Any change must be done on the problem facts and planning entities referenced by the `planning_solution`.

    Examples
    --------
    An example implementation, based on the Cloud balancing problem, looks as follows:
    >>> from timefold.solver import ProblemChange
    >>> from domain import CloudBalance, CloudComputer
    >>>
    >>> class DeleteComputerProblemChange(ProblemChange[CloudBalance]):
    ...     computer: CloudComputer
    ...
    ...     def __init__(self, computer: CloudComputer):
    ...         self.computer = computer
    ...
    ...     def do_change(self, cloud_balance: CloudBalance, problem_change_director: ProblemChangeDirector):
    ...         working_computer = problem_change_director.lookup_working_object_or_fail(self.computer)
    ...         # First remove the problem fact from all planning entities that use it
    ...         for process in cloud_balance.process_list:
    ...             if process.computer == working_computer:
    ...                 problem_change_director.change_variable(process, "computer",
    ...                                                         lambda working_process: setattr(working_process,
    ...                                                                                         'computer', None))
    ...         # A SolutionCloner does not clone problem fact lists (such as computer_list), only entity lists.
    ...         # Shallow clone the computer_list so only the working solution is affected.
    ...         computer_list = cloud_balance.computer_list.copy()
    ...         cloud_balance.computer_list = computer_list
    ...         # Remove the problem fact itself
    ...         problem_change_director.remove_problem_fact(working_computer, computer_list.remove)
    """
    @abstractmethod
    def do_change(self, working_solution: Solution_, problem_change_director: ProblemChangeDirector) -> None:
        """
        Do the change on the `planning_solution`.
        Every modification to the `planning_solution` must be done via the `ProblemChangeDirector`,
        otherwise the Score calculation will be corrupted.

        Parameters
        ----------
        working_solution : Solution_
            the working solution which contains the problem facts (and planning entities) to change
        problem_change_director : ProblemChangeDirector
            `ProblemChangeDirector` to perform the change through
        """
        ...


@JImplements('ai.timefold.solver.core.api.solver.change.ProblemChange', deferred=True)
class ProblemChangeWrapper:
    _delegate: ProblemChange

    def __init__(self, delegate: ProblemChange):
        self._delegate = delegate

    @JOverride
    def doChange(self, working_solution, problem_change_director: '_ProblemChangeDirector') -> None:
        wrapped_problem_change_director = ProblemChangeDirector(problem_change_director,
                                                                working_solution,
                                                                unwrap_python_like_object(working_solution))
        self._delegate.do_change(working_solution, wrapped_problem_change_director)


__all__ = ['ProblemChange', 'ProblemChangeDirector']
