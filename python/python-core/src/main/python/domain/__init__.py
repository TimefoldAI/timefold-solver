"""
Annotations, classes and decorators used to
define the domain of a planning problem.
See `the modeling planning problems section in Timefold Solver documentation
<https://docs.timefold.ai/timefold-solver/latest/using-timefold-solver/modeling-planning-problems>`_.

Examples
--------
>>> from timefold.domain import PlanningVariable, PlanningId, planning_entity
>>> from typing import Annotated
>>> from datetime import datetime
>>>
>>> class Room:
...     id: Annotated[str, PlanningId]
...
>>> class Timeslot:
...     id: Annotated[str, PlanningId]
...     start: datetime
...     end: datetime
...
>>> @planning_entity
>>> class Lesson:
...     id: Annotated[str, PlanningId]
...     teacher: str
...     room: Annotated[Room, PlanningVariable]
...     timeslot: Annotated[Timeslot, PlanningVariable]
"""
from ._annotations import *
from ._types import *
from ._value_range import *
from ._variable_listener import *
from typing import TYPE_CHECKING as _TYPE_CHECKING

if _TYPE_CHECKING:
    class CountableValueRange:
        ...


def __getattr__(name: str):
    from ._value_range import lookup_value_range_class  # noqa
    return lookup_value_range_class(name)


if not _TYPE_CHECKING:
    exported = [name for name in globals().keys() if not name.startswith('_')]
    exported += ['CountableValueRange']
    __all__ = exported
