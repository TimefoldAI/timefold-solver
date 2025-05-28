from __future__ import annotations

import datetime
from dataclasses import dataclass, field
from typing import List

from timefold.solver.domain import problem_fact, planning_entity, planning_variable


@dataclass
class Timeslot:
    id: str
    start_datetime: datetime.datetime
    end_datetime: datetime.datetime

    def overlaps(self, other_timeslot: Timeslot) -> bool:
        return max(self.start_datetime, other_timeslot.start_datetime) < min(self.end_datetime, other_timeslot.end_datetime)


@dataclass(frozen=True)
class Skill:
    name: str

    def __hash__(self):
        return hash(self.name)

    def __eq__(self, other):
        if not isinstance(other, Skill):
            return False
        return self.name == other.name


@problem_fact
@dataclass
class Employee:
    id: str
    name: str
    skills: List[Skill] = field(default_factory=list)
    unavailable_timeslots: List[Timeslot] = field(default_factory=list)
    cannot_work_with: List[str] = field(default_factory=list)
    classification: str = "unknown"
    is_senior: bool = False


@planning_entity
@dataclass
class Shift:
    id: str
    timeslot: Timeslot
    required_skill: Skill
    employee: Employee = field(default=None)

    @planning_variable(Employee)
    def get_employee(self) -> Employee | None:
        return self.employee

    def set_employee(self, employee: Employee | None) -> None:
        self.employee = employee
