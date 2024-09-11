from typing import TypeVar, Callable, TYPE_CHECKING

from ._constraint_builder import Constraint
from ._constraint_factory import ConstraintFactory
from .._timefold_java_interop import ensure_init, _generate_constraint_provider_class, register_java_class, wrap_errors

if TYPE_CHECKING:
    from ..score import Score

Solution_ = TypeVar('Solution_')


def constraint_provider(constraint_provider_function: Callable[[ConstraintFactory], list[Constraint]], /) \
        -> Callable[[ConstraintFactory], list[Constraint]]:
    """
    A decorator used to convert a function into a constraint provider.
    Used by Constraint Streams' Score calculation.
    An implementation must be stateless in order to facilitate
    building a single set of constraints independent of potentially changing constraint weights.

    The function must have the signature ``ConstraintFactory -> list[Constraint]``.

    Examples
    --------
    >>> from timefold.solver.score import ConstraintFactory, Constraint, Joiners, HardSoftScore, constraint_provider
    >>> from domain import Lesson
    >>>
    >>> @constraint_provider
    ... def timetabling_constraints(cf: ConstraintFactory) -> list[Constraint]:
    ...     return [
    ...         cf.for_each_unique_pair(Lesson,
    ...                                 Joiners.equal(lambda lesson: lesson.teacher),
    ...                                 Joiners.equal(lambda lesson: lesson.timeslot))
    ...           .penalize(HardSoftScore.ONE_HARD)
    ...           .as_constraint('Overlapping Timeslots')
    ...     ]

    See Also
    --------
    Joiners
    ConstraintCollectors
    ConstraintFactory
    UniConstraintStream
    """
    ensure_init()

    def constraint_provider_wrapper(function):
        def wrapped_constraint_provider(constraint_factory):
            from ..score import ConstraintFactory
            out = wrap_errors(function)(ConstraintFactory(constraint_factory))
            return out
        java_class = _generate_constraint_provider_class(function, wrapped_constraint_provider)
        return register_java_class(wrapped_constraint_provider, java_class)

    return constraint_provider_wrapper(constraint_provider_function)


def easy_score_calculator(easy_score_calculator_function: Callable[[Solution_], 'Score']) -> \
        Callable[[Solution_], 'Score']:
    """
    Used for easy Python `Score` calculation.
    This is non-incremental calculation, which is slow.
    An implementation must be stateless.

    The function must have the signature ``Solution_ -> Score``.

    Examples
    --------
    >>> from timefold.solver.score import SimpleScore, easy_score_calculator
    >>> from domain import Timetable
    >>>
    >>> @easy_score_calculator
    ... def timetabling_constraints(timetable: Timetable) -> SimpleScore:
    ...     total_score = 0
    ...
    ...     for lesson_1 in timetable.lessons:
    ...         for lesson_2 in timetable.lessons:
    ...             if lesson_1.teacher == lesson_2.teacher and lesson_1.timeslot == lesson_2.timeslot:
    ...                 total_score -= 1
    ...
    ...     return SimpleScore.of(total_score)

    """
    ensure_init()
    from _jpyinterpreter import translate_python_bytecode_to_java_bytecode, generate_proxy_class_for_translated_function
    from ai.timefold.solver.core.api.score.calculator import EasyScoreCalculator

    def wrapped_easy_score_calculator(solution):
        return easy_score_calculator_function(solution)._to_java_score()

    java_class = generate_proxy_class_for_translated_function(EasyScoreCalculator,
                                                              translate_python_bytecode_to_java_bytecode(
                                                                  wrapped_easy_score_calculator, EasyScoreCalculator))
    return register_java_class(easy_score_calculator_function, java_class)


__all__ = ['constraint_provider', 'easy_score_calculator']
