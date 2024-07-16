"""
Classes used to test constraints.
See `testing a constraint stream
<https://docs.timefold.ai/timefold-solver/latest/constraints-and-score/score-calculation#constraintStreamsTesting>`_.

Examples
--------
>>> from timefold.solver.test import ConstraintVerifier
>>> from domain import Lesson, Room, Timeslot, generate_solver_config
>>> from constraint import overlapping_timeslots
>>>
>>> verifier = ConstraintVerifier.create(generate_solver_config())
>>> timeslot = Timeslot(...)
>>> (verifier.verify_that(overlapping_timeslots)
...          .given(Lesson('Amy', Room('A'), timeslot),
...                 Lesson('Amy', Room('B'), timeslot))
...          .penalizes_by(1))
"""
from typing import Callable, Generic, List, Type, TypeVar, TYPE_CHECKING, overload, Union

from .._jpype_type_conversions import PythonBiFunction
from .._timefold_java_interop import get_class
from ..score import ConstraintFactory
from ..config import SolverConfig

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from ai.timefold.solver.core.api.score.stream import Constraint, ConstraintFactory, ConstraintJustification
    from ai.timefold.solver.core.config.solver import SolverConfig
    from ai.timefold.solver.core.api.score import Score


Solution_ = TypeVar('Solution_')


class ConstraintVerifier(Generic[Solution_]):
    """
    Entry point for the ConstraintVerifier API, which is used to test constraints defined by
    a @constraint_provider function.
    """
    def __init__(self, delegate):
        self.delegate = delegate

    @staticmethod
    def create(solver_config: SolverConfig):
        from ai.timefold.solver.test.api.score.stream import ConstraintVerifier as JavaConstraintVerifier  # noqa
        return ConstraintVerifier(JavaConstraintVerifier.create(solver_config._to_java_solver_config()))

    @staticmethod
    def build(constraint_provider: Callable[['ConstraintFactory'], List['Constraint']],
              planning_solution_class: Type[Solution_], *entity_classes: Type):
        from ai.timefold.solver.test.api.score.stream import ConstraintVerifier as JavaConstraintVerifier  # noqa
        constraint_provider_instance = get_class(constraint_provider).getConstructor().newInstance()
        planning_solution_java_class = get_class(planning_solution_class)
        entity_java_classes = list(map(get_class, entity_classes))
        return ConstraintVerifier(JavaConstraintVerifier.build(constraint_provider_instance,
                                                               planning_solution_java_class,
                                                               entity_java_classes))

    @overload
    def verify_that(self) -> 'MultiConstraintVerification[Solution_]':
        """
        Creates a constraint verifier for all constraints of the ConstraintProvider.
        """
        ...

    @overload
    def verify_that(self, constraint_function: Callable[['ConstraintFactory'], 'Constraint']) -> \
            'SingleConstraintVerification[Solution_]':
        ...

    def verify_that(self, constraint_function: Callable[['ConstraintFactory'], 'Constraint'] = None):
        """
        Creates a constraint verifier for a given Constraint of the ConstraintProvider.

        Parameters
        ----------
        constraint_function : Callable[['ConstraintFactory'], 'Constraint'], optional
            the constraint to verify.
            If not provided, all constraints will be tested
        """
        if constraint_function is None:
            return MultiConstraintVerification(self.delegate.verifyThat())
        else:
            return SingleConstraintVerification(self.delegate.verifyThat(
                PythonBiFunction(lambda _, constraint_factory:
                                 constraint_function(ConstraintFactory(constraint_factory)))))


class SingleConstraintVerification(Generic[Solution_]):
    def __init__(self, delegate):
        self.delegate = delegate

    def given(self, *facts) -> 'SingleConstraintAssertion':
        """
        Set the facts for this assertion

        Parameters
        ----------
        facts
            never ``None``, at least one
        """
        from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        from ai.timefold.jpyinterpreter.types import CPythonBackedPythonLikeObject  # noqa
        from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference  # noqa
        from java.util import HashMap
        from _jpyinterpreter import convert_to_java_python_like_object
        reference_map = HashMap()
        wrapped_facts = []

        for fact in facts:
            wrapped_fact = convert_to_java_python_like_object(fact, reference_map)
            wrapped_facts.append(wrapped_fact)

        return SingleConstraintAssertion(self.delegate.given(wrapped_facts))

    def given_solution(self, solution: 'Solution_') -> 'SingleConstraintAssertion':
        """
        Set the solution to be used for this assertion

        Parameters
        ----------
        solution
            never ``None``
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        wrapped_solution = convert_to_java_python_like_object(solution)
        return SingleConstraintAssertion(self.delegate.givenSolution(wrapped_solution))


class MultiConstraintVerification(Generic[Solution_]):
    def __init__(self, delegate):
        self.delegate = delegate

    def given(self, *facts) -> 'MultiConstraintAssertion':
        """
        Set the facts for this assertion

        Parameters
        ----------
        facts
            never ``None``, at least one
        """
        from ai.timefold.jpyinterpreter import CPythonBackedPythonInterpreter  # noqa
        from ai.timefold.jpyinterpreter.types import CPythonBackedPythonLikeObject  # noqa
        from ai.timefold.jpyinterpreter.types.wrappers import OpaquePythonReference  # noqa
        from _jpyinterpreter import convert_to_java_python_like_object
        from java.util import HashMap
        reference_map = HashMap()
        wrapped_facts = []

        for fact in facts:
            wrapped_fact = convert_to_java_python_like_object(fact, reference_map)
            wrapped_facts.append(wrapped_fact)

        return MultiConstraintAssertion(self.delegate.given(wrapped_facts))

    def given_solution(self, solution: 'Solution_') -> 'MultiConstraintAssertion':
        """
        Set the solution to be used for this assertion.

        Parameters
        ----------
        solution
            never ``None``
        """
        from _jpyinterpreter import convert_to_java_python_like_object
        wrapped_solution = convert_to_java_python_like_object(solution)
        return MultiConstraintAssertion(self.delegate.givenSolution(wrapped_solution))


class SingleConstraintAssertion:
    def __init__(self, delegate):
        self.delegate = delegate

    def justifies_with(self, *justifications: 'ConstraintJustification', message: str = None) \
            -> 'SingleConstraintAssertion':
        """
        Asserts that the constraint being tested, given a set of facts, results in given justifications.

        Parameters
        ----------
        justifications : ConstraintVerifier
            zero or more justification to check for

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected justifications are not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        from _jpyinterpreter import convert_to_java_python_like_object
        from java.util import HashMap
        reference_map = HashMap()
        wrapped_justifications = []
        for justification in justifications:
            wrapped_justification = convert_to_java_python_like_object(justification, reference_map)
            wrapped_justifications.append(wrapped_justification)
        try:
            if message is None:
                return SingleConstraintAssertion(self.delegate.justifiesWith(*wrapped_justifications))
            else:
                return SingleConstraintAssertion(self.delegate.justifiesWith(message, *wrapped_justifications))
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def justifies_with_exactly(self, *justifications: 'ConstraintJustification', message: str = None) \
            -> 'SingleConstraintAssertion':
        """
        Asserts that the constraint being tested, given a set of facts, results in given justifications an no others.

        Parameters
        ----------
        justifications : ConstraintVerifier
            zero or more justification to check for

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected justifications are not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        from _jpyinterpreter import convert_to_java_python_like_object
        from java.util import HashMap
        reference_map = HashMap()
        wrapped_justifications = []
        for justification in justifications:
            wrapped_justification = convert_to_java_python_like_object(justification, reference_map)
            wrapped_justifications.append(wrapped_justification)
        try:
            if message is None:
                return SingleConstraintAssertion(self.delegate.justifiesWithExactly(*wrapped_justifications))
            else:
                return SingleConstraintAssertion(self.delegate.justifiesWithExactly(message, *wrapped_justifications))
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def indicts_with(self, *indictments, message: str = None) -> 'SingleConstraintAssertion':
        """
        Asserts that the constraint being tested, given a set of facts, results in given indictments.

        Parameters
        ----------
        indictments : ConstraintVerifier
            zero or more indictments to check for

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected indictments are not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        from _jpyinterpreter import convert_to_java_python_like_object
        from java.util import HashMap
        reference_map = HashMap()
        wrapped_indictments = []
        for indictment in indictments:
            wrapped_indictment = convert_to_java_python_like_object(indictment, reference_map)
            wrapped_indictments.append(wrapped_indictment)
        try:
            if message is None:
                return SingleConstraintAssertion(self.delegate.indictsWith(*wrapped_indictments))
            else:
                return SingleConstraintAssertion(self.delegate.indictsWith(message, *wrapped_indictments))
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def indicts_with_exactly(self, *indictments, message: str = None) -> 'SingleConstraintAssertion':
        """
        Asserts that the constraint being tested, given a set of facts, results in given indictments an no others.

        Parameters
        ----------
        indictments : ConstraintVerifier
            zero or more justification to check for

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected indictments are not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        from _jpyinterpreter import convert_to_java_python_like_object
        from java.util import HashMap
        reference_map = HashMap()
        wrapped_indictments = []
        for indictment in indictments:
            wrapped_indictment = convert_to_java_python_like_object(indictment, reference_map)
            wrapped_indictments.append(wrapped_indictment)
        try:
            if message is None:
                return SingleConstraintAssertion(self.delegate.indictsWithExactly(*wrapped_indictments))
            else:
                return SingleConstraintAssertion(self.delegate.indictsWithExactly(message, *wrapped_indictments))
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def penalizes(self, times: int = None, message: str = None) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        Parameters
        ----------
        times : int, optional
            the expected number of penalties.
            If not provided, it raises an AssertionError when there are no penalties

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected penalty is not observed if `times` is provided, or
            when there are no penalties if `times` is not provided
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is None and message is None:
                self.delegate.penalizes()
            elif times is not None and message is None:
                self.delegate.penalizes(times)
            elif times is None and message is not None:
                self.delegate.penalizes(message)
            else:
                self.delegate.penalizes(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def penalizes_less_than(self, times: int, message: str = None) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts,
        results in less than a given number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        Parameters
        ----------
        times : int
            the expected number of penalties.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected penalty is not observed if `times` is provided
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is not None and message is None:
                self.delegate.penalizesLessThan(times)
            else:
                self.delegate.penalizesLessThan(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def penalizes_more_than(self, times: int, message: str = None) -> None:
        """
        Asserts that the Constraint being tested, given a set of facts,
        results in more than a given number of penalties.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        Parameters
        ----------
        times : int
            the expected number of penalties.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected penalty is not observed if `times` is provided
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is not None and message is None:
                self.delegate.penalizesMoreThan(times)
            else:
                self.delegate.penalizesMoreThan(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def penalizes_by(self, match_weight_total: int, message: str = None):
        """
        Asserts that the `Constraint` being tested, given a set of facts, results in a specific penalty.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard. In that case, this assertion checks for 10.

        Parameters
        ----------
        match_weight_total : int
            the expected penalty

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected penalty is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.penalizesBy(match_weight_total)
            else:
                self.delegate.penalizesBy(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def penalizes_by_less_than(self, match_weight_total: int, message: str = None):
        """
        Asserts that the `Constraint` being tested, given a set of facts, results in less than a specific penalty.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard.
        In that case, this assertion checks for 10.

        Parameters
        ----------
        match_weight_total : int
            the expected penalty

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected penalty is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.penalizesByLessThan(match_weight_total)
            else:
                self.delegate.penalizesByLessThan(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def penalizes_by_more_than(self, match_weight_total: int, message: str = None):
        """
        Asserts that the `Constraint` being tested, given a set of facts, results in more than a specific penalty.

        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of -2hard reduces the
        score by -20hard.
        In that case, this assertion checks for 10.

        Parameters
        ----------
        match_weight_total : int
            the expected penalty

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected penalty is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.penalizesByMoreThan(match_weight_total)
            else:
                self.delegate.penalizesByMoreThan(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def rewards(self, times: int = None, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts, results in a given number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        Parameters
        ----------
        times : int, optional
            the expected number of rewards.
            If not provided, it raises an AssertionError when there are no rewards

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected reward is not observed if times is provided, or
            when there are no rewards if times is not provided
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is None and message is None:
                self.delegate.rewards()
            elif times is not None and message is None:
                self.delegate.rewards(times)
            elif times is None and message is not None:
                self.delegate.rewards(message)
            else:
                self.delegate.rewards(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def rewards_less_than(self, times: int, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts,
        results in a less than a given number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        Parameters
        ----------
        times : int
            the expected number of rewards.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected reward is not observed if times is provided
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is not None and message is None:
                self.delegate.rewardsLessThan(times)
            else:
                self.delegate.rewardsLessThan(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def rewards_more_than(self, times: int, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts,
        results in more than a given number of rewards.

        Ignores the constraint and match weights: it only asserts the number of matches
        For example: if there are two matches with weight of 10 each, this assertion will check for 2 matches.

        Parameters
        ----------
        times : int
            the expected number of rewards.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected reward is not observed if times is provided
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if times is not None and message is None:
                self.delegate.rewardsMoreThan(times)
            else:
                self.delegate.rewardsMoreThan(times, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def rewards_with(self, match_weight_total: int, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts, results in a specific reward.
        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of
        -2hard reduces the score by -20hard.
        In that case, this assertion checks for 10.

        Parameters
        ----------
        match_weight_total : int
            at least 0, expected sum of match weights of matches of the constraint.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected reward is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.rewardsWith(match_weight_total)
            else:
                self.delegate.rewardsWith(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def rewards_with_less_than(self, match_weight_total: int, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts, results in less than a specific reward.
        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of
        -2hard reduces the score by -20hard.
        In that case, this assertion checks for 10.

        Parameters
        ----------
        match_weight_total : int
            at least 0, expected sum of match weights of matches of the constraint.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected reward is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.rewardsWithLessThan(match_weight_total)
            else:
                self.delegate.rewardsWithLessThan(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())

    def rewards_with_more_than(self, match_weight_total: int, message: str = None):
        """
        Asserts that the Constraint being tested, given a set of facts, results in more than a specific reward.
        Ignores the constraint weight: it only asserts the match weights.
        For example: a match with a match weight of 10 on a constraint with a constraint weight of
        -2hard reduces the score by -20hard.
        In that case, this assertion checks for 10.

        Parameters
        ----------
        match_weight_total : int
            at least 0, expected sum of match weights of matches of the constraint.

        message : str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected reward is not observed
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.rewardsWithMoreThan(match_weight_total)
            else:
                self.delegate.rewardsWithMoreThan(match_weight_total, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())


class MultiConstraintAssertion:
    def __init__(self, delegate):
        self.delegate = delegate

    def scores(self, score: 'Score', message: str = None):
        """
        Asserts that the `constraint_provider` under test, given a set of facts, results in a specific `Score`.

        Parameters
        ----------
        score : Score
            total score calculated for the given set of facts

        message: str, optional
            description of the scenario being asserted

        Raises
        ------
        AssertionError
            when the expected score does not match the calculated score
        """
        from java.lang import AssertionError as JavaAssertionError  # noqa
        try:
            if message is None:
                self.delegate.scores(score)
            else:
                self.delegate.scores(score, message)
        except JavaAssertionError as e:
            raise AssertionError(e.getMessage())


__all__ = [
    'ConstraintVerifier',
    'SingleConstraintVerification', 'SingleConstraintAssertion',
    'MultiConstraintVerification', 'MultiConstraintAssertion'
]
