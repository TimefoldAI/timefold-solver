from _jpyinterpreter import JavaAnnotation
from enum import Enum
from jpype import JImplements, JOverride, JClass
from typing import Union, List, Callable, Type, TypeVar

from ._variable_listener import VariableListener
from .._timefold_java_interop import ensure_init

Solution_ = TypeVar('Solution_')


class PlanningId(JavaAnnotation):
    """
    Specifies that an attribute is the id to match when locating
    an external object (often from another thread).
    Used during Move rebasing and in a ProblemChange.
    It is specified on an attribute of a `planning_entity` class,
    planning value class or any problem fact class.
    The return type can be any comparable type that overrides
    ``__eq__`` and ``__hash__``, and is usually ``int`` or ``str``.
    It must never return a ``None`` instance.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningId
    >>> from typing import Annotated
    >>>
    >>> class Room:
    ...     id: Annotated[str, PlanningId]

    See Also
    --------
    planning_entity
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.lookup import PlanningId as JavaPlanningId
        super().__init__(JavaPlanningId, {})


class PlanningPin:
    """
    Specifies that a boolean attribute of a `planning_entity` determines if the planning entity is pinned.
    A pinned planning entity is never changed during planning.
    For example,
    it allows the user to pin a shift to a specific employee before solving and the solver will not undo that,
    regardless of the constraints.

    The boolean is ``False`` if the planning entity is movable and ``True`` if the planning entity is pinned.
    It applies to all the planning variables of that planning entity.
    If set on an entity with PlanningListVariable, this will pin the entire list of planning values as well.
    This is syntactic sugar for ``@planning_entity(pinning_filter=...)``,
    which is a more flexible and verbose way to pin a planning entity.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningPin, planning_entity
    >>> from typing import Annotated
    >>>
    >>> @planning_entity
    ... class Lesson:
    ...     is_pinned: Annotated[bool, PlanningPin]
    """
    pass


class PlanningVariableGraphType(Enum):
    CHAINED = 'CHAINED'
    NONE = 'NONE'

    def _to_java_value(self):
        return getattr(JClass('ai.timefold.solver.core.api.domain.variable.PlanningVariableGraphType'),
                       self.name)


class PlanningVariable(JavaAnnotation):
    """
    Specifies that an attribute can be changed and should be optimized by the optimization algorithms.
    It is specified on an attribute of a `planning_entity` class.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import Room, Timeslot
    >>>
    >>> @planning_entity
    ... class Lesson:
    ...     teacher: str
    ...     room: Annotated[Room, PlanningVariable]
    ...     timeslot: Annotated[Timeslot, PlanningVariable]
    """
    def __init__(self, *,
                 value_range_provider_refs: List[str] = None,
                 allows_unassigned: bool = False,
                 graph_type: PlanningVariableGraphType = PlanningVariableGraphType.NONE):
        ensure_init()
        from ai.timefold.solver.core.api.domain.variable import PlanningVariable as JavaPlanningVariable
        super().__init__(JavaPlanningVariable,
                         {
                             'valueRangeProviderRefs': value_range_provider_refs,
                             'graphType': graph_type._to_java_value(),
                             'allowsUnassigned': allows_unassigned
                         })


class PlanningListVariable(JavaAnnotation):
    """
    Specifies that an attribute can be changed and should be optimized by the optimization algorithms.
    It is specified on an attribute of a `planning_entity` class.
    The type of the PlanningListVariable annotated attribute must be ``list[Value]``.

    List variable
    -------------
    A planning entity's attribute annotated with `PlanningListVariable` is referred to as a list variable.
    The way solver optimizes a list variable is by adding, removing,
    or changing order of elements in the `list` object held by the list variable.

    Disjoint lists
    --------------
    Furthermore,
    the current implementation works under the assumption
    that the list variables of all entity instances are "disjoint lists":

    - List means that the order of elements inside a list planning variable is significant.
    - Disjoint means that any given pair of entities have no common elements in their list variables.
    In other words, each element from the list variable's value range appears in exactly one entity's list variable.

    This makes sense for common use cases,
    for example the Vehicle Routing Problem or Task Assigning.
    In both cases the order in which customers are visited and tasks are being worked on matters.
    Also, each customer must be visited once and each task must be completed by exactly one employee.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningListVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import Visit
    >>>
    >>> @planning_entity
    ... class Vehicle:
    ...     visits: Annotated[list[Visit], PlanningListVariable]

    See Also
    --------
    PlanningPin
    PlanningPinToIndex
    """
    def __init__(self, *,
                 value_range_provider_refs: List[str] = None,
                 allows_unassigned_values: bool = False):
        ensure_init()
        from ai.timefold.solver.core.api.domain.variable import PlanningListVariable as JavaPlanningListVariable
        super().__init__(JavaPlanningListVariable,
                         {
                             'valueRangeProviderRefs': value_range_provider_refs,
                             'allowsUnassignedValues': allows_unassigned_values
                         })


class ShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute is a custom shadow variable of one or more source variables.
    The source variable may be a genuine `PlanningVariable`, `PlanningListVariable`, or another shadow variable.
    It is specified on an attribute of a `planning_entity` class.

    Examples
    --------
    >>> from timefold.solver.domain import ShadowVariable, PreviousElementShadowVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import ArrivalTimeVariableListener
    >>> from datetime import datetime
    >>>
    >>> @planning_entity
    >>> class Visit:
    ...     previous: Annotated['Visit', PreviousElementShadowVariable]
    ...     arrival_time: Annotated[datetime,
    ...                             ShadowVariable(
    ...                                 variable_listener_class=ArrivalTimeVariableListener,
    ...                                 source_variable_name='previous'
    ...                             )
    ...                            ]

    See Also
    --------
    VariableListener
    PiggybackShadowVariable
    """
    def __init__(self, *,
                 variable_listener_class: Type[VariableListener],
                 source_variable_name: str,
                 source_entity_class: Type = None):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import ShadowVariable as JavaShadowVariable

        super().__init__(JavaShadowVariable,
                         {
                             'variableListenerClass': variable_listener_class,
                             'sourceVariableName': PythonClassTranslator.getJavaFieldName(source_variable_name),
                             'sourceEntityClass': source_entity_class,
                         })


class PiggybackShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute is a custom shadow variable that is
    updated by another shadow variable's variable listener.
    It is specified on an attribute of a `planning_entity` class.

    Examples
    --------
    >>> from timefold.solver.domain import ShadowVariable, PreviousElementShadowVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import ArrivalTimeVariableListener
    >>> from datetime import datetime
    >>>
    >>> @planning_entity
    >>> class Visit:
    ...     previous: Annotated['Visit', PreviousElementShadowVariable]
    ...     arrival_time: Annotated[datetime,
    ...                             ShadowVariable(
    ...                                 variable_listener_class=ArrivalTimeVariableListener,
    ...                                 source_variable_name='previous'
    ...                             )
    ...     departure_time: Annotated[datetime,
    ...                               PiggybackShadowVariable(shadow_variable_name='arrival_time')

    See Also
    --------
    VariableListener
    """
    def __init__(self, *,
                 shadow_variable_name: str,
                 shadow_entity_class: Type = None):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import (
            PiggybackShadowVariable as JavaPiggybackShadowVariable)
        super().__init__(JavaPiggybackShadowVariable,
                         {
                             'shadowVariableName': PythonClassTranslator.getJavaFieldName(shadow_variable_name),
                             'shadowEntityClass': shadow_entity_class,
                         })


class CascadingUpdateShadowVariable(JavaAnnotation):
    """
    Specifies that field may be updated by the target method when a dependency changes.

    Automatically cascades change events to `NextElementShadowVariable` of a `PlanningListVariable`.

    Notes
    -----
    Important: it must only change the shadow variable(s) for which it's configured.
    It is only possible to define either `source_variable_name` or `source_variable_names`.
    It can be applied to multiple attributes to modify different shadow variables.
    It should never change a genuine variable or a problem fact.
    It can change its shadow variable(s) on multiple entity instances
    (for example: an arrival_time change affects all trailing entities too).

    Examples
    --------

    >>> from timefold.solver.domain import CascadingUpdateShadowVariable, PreviousElementShadowVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import ArrivalTimeVariableListener
    >>> from datetime import datetime, timedelta
    >>>
    >>> @planning_entity
    >>> class Visit:
    ...     previous: Annotated['Visit', PreviousElementShadowVariable]
    ...     arrival_time: Annotated[datetime,
    ...                             CascadingUpdateShadowVariable(
    ...                                 target_method_name='update_arrival_time'
    ...                             )
    ...                            ]
    ...
    ...     def update_arrival_time(self):
    ...         self.arrival_time = previous.arrival_time + timedelta(hours=1)
    """

    def __init__(self, *,
                 target_method_name: str):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import \
            CascadingUpdateShadowVariable as JavaCascadingUpdateShadowVariable

        super().__init__(JavaCascadingUpdateShadowVariable,
                         {
                             'targetMethodName': PythonClassTranslator.getJavaMethodName(target_method_name),
                         })


class IndexShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute is an index of this planning value in another entity's `PlanningListVariable`.
    It is specified on an attribute of a `planning_entity` class.
    The source variable must be a list variable.

    Examples
    --------
    >>> from timefold.solver.domain import IndexShadowVariable, planning_entity
    >>> from typing import Annotated
    >>>
    >>> @planning_entity
    ... class Visit:
    ...     visit_index: Annotated[int, IndexShadowVariable]
    """
    def __init__(self, *,
                 source_variable_name: str):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import (
            IndexShadowVariable as JavaIndexShadowVariable)
        super().__init__(JavaIndexShadowVariable,
                         {
                             'sourceVariableName': PythonClassTranslator.getJavaFieldName(source_variable_name)
                         })


class PreviousElementShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute references the previous element in the same `PlanningListVariable`.
    The previous element's index is one lower than this element's index.
    It is ``None`` if this element is the first element in the list variable.
    It is specified on an attribute of a `planning_entity` class.
    The source variable must be a list variable.

    Examples
    --------
    >>> from timefold.solver.domain import PreviousElementShadowVariable, planning_entity
    >>> from typing import Annotated
    >>>
    >>> @planning_entity
    >>> class Visit:
    ...     previous: Annotated['Visit', PreviousElementShadowVariable]
    """
    def __init__(self, *,
                 source_variable_name: str):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import (
            PreviousElementShadowVariable as JavaPreviousElementShadowVariable)
        super().__init__(JavaPreviousElementShadowVariable,
                         {
                             'sourceVariableName': PythonClassTranslator.getJavaFieldName(source_variable_name)
                         })


class NextElementShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute references the next element in the same `PlanningListVariable`.
    The next element's index is one higher than this element's index.
    It is ``None`` if this element is the last element in the list variable.
    It is specified on an attribute of a `planning_entity` class.
    The source variable must be a list variable.
    """
    def __init__(self, *,
                 source_variable_name: str):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import (
            NextElementShadowVariable as JavaNextElementShadowVariable)
        super().__init__(JavaNextElementShadowVariable,
                         {
                             'sourceVariableName': PythonClassTranslator.getJavaFieldName(source_variable_name)
                         })


class AnchorShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute is the anchor of a chained `PlanningVariable`,
    which implies it's a shadow variable.
    It is specified on an attribute of a `planning_entity` class.
    """
    def __init__(self, *,
                 source_variable_name: str):
        ensure_init()
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        from ai.timefold.solver.core.api.domain.variable import (
            AnchorShadowVariable as JavaAnchorShadowVariable)
        super().__init__(JavaAnchorShadowVariable,
                         {
                             'sourceVariableName': PythonClassTranslator.getJavaFieldName(source_variable_name)
                         })


class InverseRelationShadowVariable(JavaAnnotation):
    """
    Specifies that an attribute is the inverse of a `PlanningVariable`,
    which implies it's a shadow variable.
    It is specified on an attribute of a `planning_entity` class.

    Examples
    --------
    >>> from timefold.solver.domain import InverseRelationShadowVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import Vehicle
    >>>
    >>> @planning_entity
    >>> class Visit:
    ...     vehicle: Annotated[Vehicle, InverseRelationShadowVariable(source_variable_name='visits')]
    """
    def __init__(self, *,
                 source_variable_name: str):
        ensure_init()
        from ai.timefold.solver.core.api.domain.variable import (
            InverseRelationShadowVariable as JavaInverseRelationShadowVariable)
        from ai.timefold.jpyinterpreter import PythonClassTranslator
        super().__init__(JavaInverseRelationShadowVariable,
                         {
                             'sourceVariableName': PythonClassTranslator.getJavaFieldName(source_variable_name)
                         })


class ProblemFactProperty(JavaAnnotation):
    """
    Specifies that an attribute on a `planning_solution` class is a problem fact.
    A problem fact must not change during solving (except through a `ProblemChange` event).
    The constraints in a `timefold.solver.score.ConstraintProvider` rely on problem facts for
    `timefold.solver.score.ConstraintFactory.for_each`.
    Do not annotate a planning entity or a planning paramerization as a problem fact:
    they are automatically available as facts for `timefold.solver.score.ConstraintFactory.for_each`.

    Examples
    --------
    >>> from timefold.solver.domain import ProblemFactProperty, planning_solution
    >>> from typing import Annotated
    >>> from domain import School
    >>>
    >>> @planning_solution
    >>> class Timetable:
    ...     school: Annotated[School, ProblemFactProperty]
    ...     # ...

    See Also
    --------
    ProblemFactCollectionProperty
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.solution import (
            ProblemFactProperty as JavaProblemFactProperty)
        super().__init__(JavaProblemFactProperty, {})


class ProblemFactCollectionProperty(JavaAnnotation):
    """
    Specifies that an attribute on a `planning_solution` class is a collection of problem facts.
    A problem fact must not change during solving (except through a `ProblemChange` event).
    The constraints in a `timefold.solver.score.ConstraintProvider` rely on problem facts for
    `timefold.solver.score.ConstraintFactory.for_each`.
    Do not annotate a planning entity or a planning paramerization as a problem fact:
    they are automatically available as facts for `timefold.solver.score.ConstraintFactory.for_each`.

    Examples
    --------
    >>> from timefold.solver.domain import ProblemFactCollectionProperty, planning_solution
    >>> from typing import Annotated
    >>> from domain import School
    >>>
    >>> @planning_solution
    >>> class Timetable:
    ...     schools: Annotated[list[School], ProblemFactCollectionProperty]
    ...     # ...

    See Also
    --------
    ProblemFactProperty
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.solution import (
            ProblemFactCollectionProperty as JavaProblemFactCollectionProperty)
        super().__init__(JavaProblemFactCollectionProperty, {})


class PlanningEntityProperty(JavaAnnotation):
    """
    Specifies that an attribute on a `planning_solution` class is a planning entity.
    The planning entity class should be decorated by the `planning_entity` decorator.
    The planning entity will be added to the `ScoreDirector`.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningEntityProperty, planning_solution
    >>> from typing import Annotated
    >>> from domain import Lesson
    >>>
    >>> @planning_solution
    >>> class Timetable:
    ...     lesson: Annotated[Lesson, PlanningEntityProperty]
    ...     # ...
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.solution import (
            PlanningEntityProperty as JavaPlanningEntityProperty)
        super().__init__(JavaPlanningEntityProperty, {})


class PlanningEntityCollectionProperty(JavaAnnotation):
    """
    Specifies that an attribute on a `planning_solution` class is a collection of planning entities.
    The class of every element in the planning entity collection should be decorated by the `planning_entity` decorator.
    Every element in the planning entity collection will be added to the `ScoreDirector`.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningEntityCollectionProperty, planning_solution
    >>> from typing import Annotated
    >>> from domain import Lesson
    >>>
    >>> @planning_solution
    >>> class Timetable:
    ...     lessons: Annotated[list[Lesson], PlanningEntityCollectionProperty]
    ...     # ...
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.solution import (
            PlanningEntityCollectionProperty as JavaPlanningEntityCollectionProperty)
        super().__init__(JavaPlanningEntityCollectionProperty, {})


class ValueRangeProvider(JavaAnnotation):
    """
    Provides the planning values that can be used for a `PlanningVariable`.
    This is specified on an attribute which returns a collection (like `list` or `set`) or ValueRange.
    A collection is implicitly converted to a ValueRange.

    Examples
    --------
    >>> from timefold.solver.domain import ProblemFactCollectionProperty, ValueRangeProvider, planning_solution
    >>> from typing import Annotated
    >>> from domain import Room
    >>>
    >>> @planning_solution
    >>> class Timetable:
    ...     rooms: Annotated[list[Room], ProblemFactCollectionProperty, ValueRangeProvider]
    ...     # ...
    """
    def __init__(self, *, id: str = None):
        ensure_init()
        from ai.timefold.solver.core.api.domain.valuerange import (
            ValueRangeProvider as JavaValueRangeProvider)
        super().__init__(JavaValueRangeProvider, {
            'id': id
        })


class PlanningScore(JavaAnnotation):
    """
    Specifies that an attribute on a `planning_solution` class holds the `timefold.score.Score` of that solution.
    This attribute can be ``None`` if the planning solution is uninitialized.
    This attribute is modified by the Solver,
    every time when the `timefold.score.Score` of this planning solution has been calculated.

    Examples
    --------
    >>> from timefold.solver.domain import PlanningScore, planning_solution
    >>> from timefold.solver.score import HardSoftScore
    >>> from typing import Annotated
    >>>
    >>> @planning_solution
    >>> class Timetable:
    ...     score: Annotated[HardSoftScore, PlanningScore]
    ...     # ...
    """
    def __init__(self, *,
                 bendable_hard_levels_size: int = None,
                 bendable_soft_levels_size: int = None):
        ensure_init()
        from ai.timefold.solver.core.api.domain.solution import (
            PlanningScore as JavaPlanningScore)
        super().__init__(JavaPlanningScore,
                         {
                             'bendableHardLevelsSize': bendable_hard_levels_size,
                             'bendableSoftLevelsSize': bendable_soft_levels_size
                         })


class DeepPlanningClone(JavaAnnotation):
    """
    Marks a problem fact class as being required to be deep planning cloned.
    Not needed for a `planning_solution` or `planning_entity` because those are automatically deep cloned.
    It can also mark an attribute as being required to be deep planning cloned.
    This is especially useful for `list` (or `dict`) properties.
    Not needed for a `list` (or `dist`) attribute with a generic type of `planning_entity`,
    because those are automatically deep cloned.

    Notes
    -----
    If it annotates an attribute returning `list` (or `dict`),
    it clones the `list` (or `dict`),
    but its elements (or keys and values) are only cloned if they are of a type that needs to be planning cloned.

    Examples
    --------
    >>> from timefold.solver.domain import DeepPlanningClone, ShadowVariable, planning_entity
    >>> from datetime import date
    >>> from typing import Annotated
    >>>
    >>> @planning_entity
    ... class Employee:
    ...     work_day_to_hours: Annotated[dict[date, int], ShadowVariable(...), DeepPlanningClone]
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.solution.cloner import (
            DeepPlanningClone as JavaDeepPlanningClone)
        super().__init__(JavaDeepPlanningClone, {})


class ConstraintConfigurationProvider(JavaAnnotation):
    """
    Specifies that an attribute on a `planning_solution` class is a `constraint_configuration`.
    This attribute is automatically a ProblemFactProperty too, so no need to declare that explicitly.
    The type of this attribute must be decorated by the `constraint_configuration` decorator.

    Examples
    --------
    >>> from timefold.solver.domain import ConstraintConfigurationProvider, planning_solution
    >>> from typing import Annotated
    >>> from domain import MyConstraintConfiguration
    >>>
    >>> @planning_solution
    ... class Timetable:
    ...     configuration: Annotated[MyConstraintConfiguration, ConstraintConfigurationProvider]
    """
    def __init__(self):
        ensure_init()
        from ai.timefold.solver.core.api.domain.constraintweight import (
            ConstraintConfigurationProvider as JavaConstraintConfigurationProvider)
        super().__init__(JavaConstraintConfigurationProvider, {})


class ConstraintWeight(JavaAnnotation):
    """
    Specifies that an attribute set the constraint weight and score level of a constraint.
    For example, with a constraint weight of 2soft,
    a constraint match penalization with weight multiplier of 3 will result in a Score of -6soft.
    It is specified on an attribute of a `constraint_configuration` class.

    Examples
    --------
    >>> from timefold.solver.domain import ConstraintWeight, constraint_configuration
    >>> from timefold.solver.score import HardSoftScore
    >>> from typing import Annotated
    >>>
    >>> @constraint_configuration
    ... class ConstraintConfiguration:
    ...     maximize_value: Annotated[HardSoftScore, ConstraintWeight('Maximize value')]
    """
    def __init__(self, constraint_name: str, *, constraint_package: str = None):
        ensure_init()
        from ai.timefold.solver.core.api.domain.constraintweight import ConstraintWeight as JavaConstraintWeight
        super().__init__(JavaConstraintWeight, {
            'value': constraint_name,
            'constraintPackage': constraint_package
        })


@JImplements('ai.timefold.solver.core.api.domain.entity.PinningFilter', deferred=True)
class _PythonPinningFilter:
    def __init__(self, delegate):
        self.delegate = delegate

    @JOverride
    def accept(self, solution, entity):
        return self.delegate(solution, entity)


def planning_entity(entity_class: Type = None, /, *, pinning_filter: Callable = None) -> Union[Type,
                                                                                               Callable[[Type], Type]]:
    """
    Specifies that the class is a planning entity.
    There are two types of entities:

    - Genuine entity
        Must have at least one genuine planning variable, and zero or more shadow variables.

    - Shadow entity
        Must have at least one shadow variable, and no genuine variables.

    If a planning entity has neither a genuine nor a shadow variable,
    it is not a planning entity and the solver will fail fast.

    Parameters
    ----------
    pinning_filter : Callable[[Solution, Entity], bool], optional
        A pinned planning entity is never changed during planning,
        this is useful in repeated planning use cases (such as continuous planning and real-time planning).
        This applies to all the planning variables of this planning entity.

        The predicate should return ``False`` if the selection entity is pinned,
        and it should return ``True`` if the selection entity is movable

    Examples
    --------
    >>> from timefold.solver.domain import PlanningVariable, planning_entity
    >>> from typing import Annotated
    >>> from domain import Timeslot, Room
    >>>
    >>> @planning_entity
    ... class Lesson:
    ...     teacher: str
    ...     room: Annotated[Room, PlanningVariable]
    ...     timeslot: Annotated[Timeslot, PlanningVariable]
    """
    ensure_init()
    from ai.timefold.solver.core.api.domain.entity import PlanningEntity as JavaPlanningEntity

    def planning_entity_wrapper(entity_class_argument):
        from .._timefold_java_interop import _add_to_compilation_queue
        from _jpyinterpreter import add_class_annotation
        from typing import get_origin, Annotated

        planning_pin_field = None
        for name, type_hint in entity_class_argument.__annotations__.items():
            if get_origin(type_hint) == Annotated:
                for metadata in type_hint.__metadata__:
                    if metadata == PlanningPin or isinstance(metadata, PlanningPin):
                        if planning_pin_field is not None:
                            raise ValueError(f'Only one attribute can be annotated with PlanningPin, '
                                             f'but found multiple fields ({planning_pin_field} and {name}).')
                        planning_pin_field = name

        pinning_filter_function = None
        if pinning_filter is not None:
            if planning_pin_field is not None:
                pinning_filter_function = lambda solution, entity: (getattr(entity, planning_pin_field, False) or
                                                                    pinning_filter(solution, entity))
            else:
                pinning_filter_function = pinning_filter
        else:
            if planning_pin_field is not None:
                pinning_filter_function = lambda solution, entity: getattr(entity, planning_pin_field, False)

        out = add_class_annotation(JavaPlanningEntity,
                                   pinningFilter=pinning_filter_function)(entity_class_argument)
        _add_to_compilation_queue(out)
        return out

    if entity_class:  # Called as @planning_entity
        return planning_entity_wrapper(entity_class)
    else:  # Called as @planning_entity(pinning_filter=some_function)
        return planning_entity_wrapper


def planning_solution(planning_solution_class: Type[Solution_]) -> Type[Solution_]:
    """
    Specifies that the class is a planning solution.
    A solution represents a problem and a possible solution of that problem.
    A possible solution does not need to be optimal or even feasible.
    A solution's planning variables might not be initialized (especially when delivered as a problem).

    A solution is mutable.
    For scalability reasons (to facilitate incremental score calculation),
    the same solution instance (called the working solution per move thread) is continuously modified.
    It's cloned to recall the best solution.

    Each planning solution must have exactly one `PlanningScore` annotated attribute.
    Each planning solution must have at least one `PlanningEntityCollectionProperty` or `PlanningEntityProperty`
    annotated attribute.
    Each planning solution is recommended to have one `ConstraintConfigurationProvider` annotated attribute too.
    Each planning solution
    used with ConstraintStream score calculation must have at least one `ProblemFactCollectionProperty` or
    `ProblemFactProperty` annotated attribute.

    Examples
    --------
    >>> from timefold.solver.domain import (PlanningScore, PlanningEntityCollectionProperty,
    ...                                     ProblemFactCollectionProperty, ValueRangeProvider,
    ...                                     ConstraintConfigurationProvider, planning_solution)
    ... from timefold.solver.score import HardSoftScore
    >>> from typing import Annotated
    >>> from domain import Lesson, Room, Timeslot, TimetablingConstraintConfiguration
    >>>
    >>> @planning_solution
    ... class Timetable:
    ...     lessons: Annotated[list[Lesson], PlanningEntityCollectionProperty]
    ...     rooms: Annotated[list[Room], ProblemFactCollectionProperty, ValueRangeProvider]
    ...     timeslots: Annotated[list[Timeslot], ProblemFactCollectionProperty, ValueRangeProvider]
    ...     configuration: Annotated[TimetablingConstraintConfiguration, ConstraintConfigurationProvider]
    ...     score: Annotated[HardSoftScore, PlanningScore]
    """
    ensure_init()
    from _jpyinterpreter import add_class_annotation
    from .._timefold_java_interop import _add_to_compilation_queue
    from ai.timefold.solver.core.api.domain.solution import PlanningSolution as JavaPlanningSolution
    out = add_class_annotation(JavaPlanningSolution)(planning_solution_class)
    _add_to_compilation_queue(planning_solution_class)
    return out


def constraint_configuration(constraint_configuration_class: Type[Solution_]) -> Type[Solution_]:
    """
    Allows end users to change the constraint weights, by not hard coding them.
    This decorator specifies that the class holds a number of ConstraintWeight annotated attributes.
    That class must also have a weight for each of the constraints.

    A `planning_solution` has at most one attribute annotated with `ConstraintConfigurationProvider`
    with returns a type of the `ConstraintConfiguration` decorated class.

    Examples
    --------
    >>> from timefold.solver.domain import ConstraintWeight, constraint_configuration
    >>> from timefold.solver.score import HardSoftScore
    >>> from typing import Annotated
    >>>
    >>> @constraint_configuration
    ... class ConstraintConfiguration:
    ...     maximize_value: Annotated[HardSoftScore, ConstraintWeight('Maximize value')]
    """
    ensure_init()
    from _jpyinterpreter import add_class_annotation
    from ai.timefold.solver.core.api.domain.constraintweight import (
        ConstraintConfiguration as JavaConstraintConfiguration)
    out = add_class_annotation(JavaConstraintConfiguration)(constraint_configuration_class)
    return out


__all__ = ['PlanningId', 'PlanningScore', 'PlanningPin', 'PlanningVariable',
           'PlanningVariableGraphType', 'PlanningListVariable', 'ShadowVariable',
           'PiggybackShadowVariable', 'CascadingUpdateShadowVariable',
           'IndexShadowVariable', 'PreviousElementShadowVariable', 'NextElementShadowVariable',
           'AnchorShadowVariable', 'InverseRelationShadowVariable',
           'ProblemFactProperty', 'ProblemFactCollectionProperty',
           'PlanningEntityProperty', 'PlanningEntityCollectionProperty',
           'ValueRangeProvider', 'DeepPlanningClone', 'ConstraintConfigurationProvider',
           'ConstraintWeight',
           'planning_entity', 'planning_solution', 'constraint_configuration']
