from datetime import datetime, timedelta

from timefold.solver import *
from timefold.solver.domain import *
from timefold.solver.config import *
from timefold.solver.score import *

from typing import Annotated, List, Optional
from dataclasses import dataclass, field


@dataclass
class Location:
    latitude: float
    longitude: float
    driving_time_seconds: dict[int, int] = field(default_factory=dict)

    def driving_time_to(self, other: 'Location') -> int:
        return self.driving_time_seconds[id(other)]


class ArrivalTimeUpdatingVariableListener(VariableListener):
    def after_variable_changed(self, score_director: ScoreDirector, visit: 'Visit') -> None:
        if visit.vehicle is None:
            if visit.arrival_time is not None:
                score_director.before_variable_changed(visit, 'arrival_time')
                visit.arrival_time = None
                score_director.after_variable_changed(visit, 'arrival_time')
            return
        previous_visit = visit.previous_visit
        departure_time = visit.vehicle.departure_time if previous_visit is None else previous_visit.departure_time()
        next_visit = visit
        arrival_time = ArrivalTimeUpdatingVariableListener.calculate_arrival_time(next_visit, departure_time)
        while next_visit is not None and next_visit.arrival_time != arrival_time:
            score_director.before_variable_changed(next_visit, 'arrival_time')
            next_visit.arrival_time = arrival_time
            score_director.after_variable_changed(next_visit, 'arrival_time')
            departure_time = next_visit.departure_time()
            next_visit = next_visit.next_visit
            arrival_time = ArrivalTimeUpdatingVariableListener.calculate_arrival_time(next_visit, departure_time)

    @staticmethod
    def calculate_arrival_time(visit: Optional['Visit'], previous_departure_time: Optional[datetime]) \
            -> datetime | None:
        if visit is None or previous_departure_time is None:
            return None
        return previous_departure_time + timedelta(seconds=visit.driving_time_seconds_from_previous_standstill())


@planning_entity
@dataclass
class Visit:
    id: Annotated[str, PlanningId]
    name: str
    location: Location
    demand: int
    min_start_time: datetime
    max_end_time: datetime
    service_duration: timedelta
    vehicle: Annotated[Optional['Vehicle'], InverseRelationShadowVariable(source_variable_name='visits')] = (
        field(default=None))
    previous_visit: Annotated[Optional['Visit'], PreviousElementShadowVariable(source_variable_name='visits')] = (
        field(default=None))
    next_visit: Annotated[Optional['Visit'],
                          NextElementShadowVariable(source_variable_name='visits')] = field(default=None)
    arrival_time: Annotated[Optional[datetime],
                            ShadowVariable(variable_listener_class=ArrivalTimeUpdatingVariableListener,
                                           source_variable_name='vehicle'),
                            ShadowVariable(variable_listener_class=ArrivalTimeUpdatingVariableListener,
                                           source_variable_name='previous_visit')] = field(default=None)

    def departure_time(self) -> Optional[datetime]:
        if self.arrival_time is None:
            return None

        return self.arrival_time + self.service_duration

    def start_service_time(self) -> Optional[datetime]:
        if self.arrival_time is None:
            return None
        return self.min_start_time if (self.min_start_time < self.arrival_time) else self.arrival_time

    def is_service_finished_after_max_end_time(self) -> bool:
        return self.arrival_time is not None and self.departure_time() > self.max_end_time

    def service_finished_delay_in_minutes(self) -> int:
        if self.arrival_time is None:
            return 0
        return (self.max_end_time - self.departure_time()).seconds // 60

    def driving_time_seconds_from_previous_standstill(self) -> int:
        if self.vehicle is None:
            raise ValueError("This method must not be called when the shadow variables are not initialized yet.")

        if self.previous_visit is None:
            return self.vehicle.home_location.driving_time_to(self.location)
        else:
            return self.previous_visit.location.driving_time_to(self.location)

    def driving_time_seconds_from_previous_standstill_or_none(self) -> Optional[int]:
        if self.vehicle is None:
            return None
        return self.driving_time_seconds_from_previous_standstill()

    def __str__(self):
        return self.id


@planning_entity
@dataclass
class Vehicle:
    id: Annotated[str, PlanningId]
    capacity: int
    home_location: Location
    departure_time: datetime
    visits: Annotated[list[Visit], PlanningListVariable] = field(default_factory=list)

    def total_demand(self) -> int:
        total_demand = 0
        for visit in self.visits:
            total_demand += visit.demand
        return total_demand

    def total_driving_time_seconds(self) -> int:
        if len(self.visits) == 0:
            return 0
        total_driving_time_seconds = 0
        previous_location = self.home_location

        for visit in self.visits:
            total_driving_time_seconds += previous_location.driving_time_to(visit.location)
            previous_location = visit.location

        total_driving_time_seconds += previous_location.driving_time_to(self.home_location)
        return total_driving_time_seconds

    def arrival_time(self):
        if len(self.visits) == 0:
            return self.departure_time

        last_visit = self.visits[-1]
        return (last_visit.departure_time() +
                timedelta(seconds=last_visit.location.driving_time_to(self.home_location)))


@planning_solution
@dataclass
class VehicleRoutePlan:
    vehicles: Annotated[list[Vehicle], PlanningEntityCollectionProperty]
    visits: Annotated[list[Visit], PlanningEntityCollectionProperty, ValueRangeProvider]
    score: Annotated[HardSoftScore, PlanningScore] = field(default=None)


@constraint_provider
def vehicle_routing_constraints(factory: ConstraintFactory):
    return [
        vehicle_capacity(factory),
        service_finished_after_max_end_time(factory),
        minimize_travel_time(factory)
    ]

##############################################
# Hard constraints
##############################################


def vehicle_capacity(factory: ConstraintFactory):
    return (factory.for_each(Vehicle)
            .filter(lambda vehicle: vehicle.total_demand() > vehicle.capacity)
            .penalize(HardSoftScore.ONE_HARD,
                      lambda vehicle: vehicle.total_demand() - vehicle.capacity)
            .as_constraint('VEHICLE_CAPACITY')
            )


def service_finished_after_max_end_time(factory: ConstraintFactory):
    return (factory.for_each(Visit)
            .filter(lambda visit: visit.is_service_finished_after_max_end_time())
            .penalize(HardSoftScore.ONE_HARD,
                      lambda visit: visit.service_finished_delay_in_minutes())
            .as_constraint('SERVICE_FINISHED_AFTER_MAX_END_TIME')
            )

##############################################
# Soft constraints
##############################################


def minimize_travel_time(factory: ConstraintFactory):
    return (
        factory.for_each(Vehicle)
        .penalize(HardSoftScore.ONE_SOFT,
                  lambda vehicle: vehicle.total_driving_time_seconds())
        .as_constraint('MINIMIZE_TRAVEL_TIME')
    )


def test_vrp():
    solver_config = SolverConfig(
        solution_class=VehicleRoutePlan,
        entity_class_list=[Vehicle, Visit],
        score_director_factory_config=ScoreDirectorFactoryConfig(
            constraint_provider_function=vehicle_routing_constraints
        ),
        termination_config=TerminationConfig(
            best_score_limit='0hard/-300soft'
        )
    )

    solver = SolverFactory.create(solver_config).build_solver()
    l1 = Location(1, 1)
    l2 = Location(2, 2)
    l3 = Location(3, 3)
    l4 = Location(4, 4)
    l5 = Location(5, 5)

    l1.driving_time_seconds = {
        id(l1): 0,
        id(l2): 60,
        id(l3): 60 * 60,
        id(l4): 60 * 60,
        id(l5): 60 * 60
    }

    l2.driving_time_seconds = {
        id(l1): 60 * 60,
        id(l2): 0,
        id(l3): 60,
        id(l4): 60 * 60,
        id(l5): 60 * 60
    }

    l3.driving_time_seconds = {
        id(l1): 60,
        id(l2): 60 * 60,
        id(l3): 0,
        id(l4): 60 * 60,
        id(l5): 60 * 60
    }

    l4.driving_time_seconds = {
        id(l1): 60 * 60,
        id(l2): 60 * 60,
        id(l3): 60 * 60,
        id(l4): 0,
        id(l5): 60
    }

    l5.driving_time_seconds = {
        id(l1): 60 * 60,
        id(l2): 60 * 60,
        id(l3): 60 * 60,
        id(l4): 60,
        id(l5): 0
    }

    problem = VehicleRoutePlan(
        vehicles=[
            Vehicle(
                id='A',
                capacity=3,
                home_location=l1,
                departure_time=datetime(2020, 1, 1),
            ),
            Vehicle(
                id='B',
                capacity=3,
                home_location=l4,
                departure_time=datetime(2020, 1, 1),
            ),
        ],
        visits=[
            Visit(
                id='1',
                name='1',
                location=l2,
                demand=1,
                min_start_time=datetime(2020, 1, 1),
                max_end_time=datetime(2020, 1, 1, hour=10),
                service_duration=timedelta(hours=1),
            ),
            Visit(
                id='2',
                name='2',
                location=l3,
                demand=1,
                min_start_time=datetime(2020, 1, 1),
                max_end_time=datetime(2020, 1, 1, hour=10),
                service_duration=timedelta(hours=1),
            ),
            Visit(
                id='3',
                name='3',
                location=l5,
                demand=1,
                min_start_time=datetime(2020, 1, 1),
                max_end_time=datetime(2020, 1, 1, hour=10),
                service_duration=timedelta(hours=1),
            ),
        ]
    )
    solution = solver.solve(problem)

    assert [visit.id for visit in solution.vehicles[0].visits] == ['1', '2']
    assert [visit.id for visit in solution.vehicles[1].visits] == ['3']
