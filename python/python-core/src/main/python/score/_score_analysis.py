from _jpyinterpreter import unwrap_python_like_object, add_java_interface
from dataclasses import dataclass
from typing import overload, TypeVar, Generic, Union, TYPE_CHECKING, Any, cast, Optional, Type

from .._jpype_type_conversions import to_python_score
from .._timefold_java_interop import get_class, _user_package_prefix

if TYPE_CHECKING:
    # These imports require a JVM to be running, so only import if type checking
    from ..score import Score
    from ai.timefold.solver.core.api.score import ScoreExplanation as _JavaScoreExplanation
    from ai.timefold.solver.core.api.score.analysis import (
        ConstraintAnalysis as _JavaConstraintAnalysis,
        MatchAnalysis as _JavaMatchAnalysis,
        ScoreAnalysis as _JavaScoreAnalysis)
    from ai.timefold.solver.core.api.score.constraint import Indictment as _JavaIndictment
    from ai.timefold.solver.core.api.score.constraint import ConstraintMatch as _JavaConstraintMatch

Solution_ = TypeVar('Solution_')
ProblemId_ = TypeVar('ProblemId_')
Score_ = TypeVar('Score_', bound='Score')
Justification_ = TypeVar('Justification_', bound='ConstraintJustification')


@dataclass(frozen=True, unsafe_hash=True)
class ConstraintRef:
    """
    Represents a unique identifier of a constraint.
    Users should have no need to create instances of this record.

    Attributes
    ----------
    package_name : str
        The constraint package is the namespace of the constraint.
        When using a `constraint_configuration`, it is equal to the
        `ConstraintWeight.constraint_package`.

    constraint_name : str
        The constraint name.
        It might not be unique, but `constraint_id` is unique.
        When using a `constraint_configuration`, it is equal to the `ConstraintWeight.constraint_name`.

    constraint_id : str
        Always derived from `packageName` and `constraintName`.
    """
    package_name: str
    constraint_name: str

    @property
    def constraint_id(self) -> str:
        return f'{self.package_name}/{self.constraint_name}'

    @staticmethod
    def parse_id(constraint_id: str):
        slash_index = constraint_id.rfind('/')
        if slash_index == -1:
            raise ValueError(
                f'The constraint_id {constraint_id} is invalid as it does not contain a package separator \'/\'.')
        package_name = constraint_id[:slash_index]
        constraint_name = constraint_id[slash_index + 1:]
        return ConstraintRef(package_name, constraint_name)

    @staticmethod
    def compose_constraint_id(solution_type_or_package: Union[type, str], constraint_name: str) -> str:
        """
        Returns the constraint id with the given constraint package and the given name

        Parameters
        ----------
        solution_type_or_package : type | str
            the constraint package, or a class decorated with @planning_solution
            (for when the constraint is in the default package)

        constraint_name : str
            the name of the constraint

        Returns
        -------
        str
            the constraint id with the given name in the default package
        """
        package = solution_type_or_package
        if not isinstance(solution_type_or_package, str):
            package = get_class(solution_type_or_package).getPackage().getName()[len(_user_package_prefix):]
        return ConstraintRef(package_name=package,
                             constraint_name=constraint_name).constraint_id

    def _to_java(self):
        from ai.timefold.solver.core.api.score.constraint import ConstraintRef as JavaConstraintRef
        return JavaConstraintRef.of(f'{_user_package_prefix}{self.package_name}', self.constraint_name)


def _safe_hash(obj: Any) -> int:
    try:
        return hash(obj)
    except TypeError:
        return id(obj)


@dataclass(frozen=True, eq=True)
class ConstraintMatch(Generic[Score_]):
    """
    Retrievable from `ConstraintMatchTotal.constraint_match_set` and
    `Indictment.constraint_match_set`.
    This class is comparable for consistent ordering of constraint matches in visualizations.
    The details of this ordering are unspecified and are subject to change.
    If possible, prefer using `SolutionManager.analyze` instead.
    """
    constraint_ref: ConstraintRef
    justification: Any
    indicted_objects: tuple[Any, ...]
    score: Score_

    @property
    def identification_string(self) -> str:
        return self.constraint_ref.constraint_id

    def __hash__(self) -> int:
        combined_hash = hash(self.constraint_ref)
        combined_hash ^= _safe_hash(self.justification)
        for item in self.indicted_objects:
            combined_hash ^= _safe_hash(item)
        combined_hash ^= self.score.__hash__()
        return combined_hash


@dataclass(eq=True)
class ConstraintMatchTotal(Generic[Score_]):
    """
    Explains the Score of a `planning_solution`,
    from the opposite side than `Indictment`.
    Retrievable from `ScoreExplanation.constraint_match_total_map`.
    If possible, prefer using `SolutionManager.analyze` instead.
    """
    constraint_ref: ConstraintRef
    constraint_match_count: int
    constraint_match_set: set[ConstraintMatch]
    constraint_weight: Optional[Score_]
    score: Score_

    def __hash__(self) -> int:
        combined_hash = hash(self.constraint_ref)
        combined_hash ^= hash(self.constraint_match_count)
        for constraint_match in self.constraint_match_set:
            combined_hash ^= hash(constraint_match)

        if self.constraint_weight is not None:
            combined_hash ^= self.constraint_weight.hashCode()

        combined_hash ^= self.score.__hash__()
        return combined_hash


@add_java_interface('ai.timefold.solver.core.api.score.stream.ConstraintJustification')
class ConstraintJustification:
    """
    Marker interface for constraint justifications.
    All classes used as constraint justifications must implement this interface.
    Implementing classes ("implementations")
    may decide to implement Comparable to preserve order of instances when displayed in user interfaces,
    logs etc. This is entirely optional.

    If two instances of this class are equal, they are considered to be the same justification.
    This matters in case of `SolutionManager.analyze` score analysis where such justifications are grouped together.
    This situation is likely to occur in case a ConstraintStream produces duplicate tuples,
    which can be avoided by using `UniConstraintStream.distinct()` or its bi, tri and quad counterparts.
    Alternatively, some unique ID (such as `uuid.uuid4()`) can be used to distinguish between instances.
    Score analysis does not diff contents of the implementations;
    instead it uses equality of the implementations (as defined above) to tell them apart from the outside.
    For this reason, it is recommended that:

    - The implementations must not use Score for equal and hash codes,
      as that would prevent diffing from working entirely.

    - The implementations should not store any Score instances,
      as they would not be diffed, leading to confusion with `MatchAnalysis.score`, which does get diffed.

    If the user wishes to use score analysis,
    they are required to ensure that the class(es)
    implementing this interface can be serialized into any format
    which is supported by the SolutionManager implementation,
    typically JSON.

    See Also
    --------
    ConstraintMatch.justification
    """
    pass


@dataclass(frozen=True, eq=True)
class DefaultConstraintJustification(ConstraintJustification):
    """
    Default implementation of `ConstraintJustification`, returned by
    `ConstraintMatch.justification` unless the user defined a custom justification mapping.
    """
    facts: tuple[Any, ...]
    impact: Score_

    def __hash__(self) -> int:
        combined_hash = self.impact.__hash__()
        for fact in self.facts:
            combined_hash ^= _safe_hash(fact)
        return combined_hash


def _map_constraint_match_set(constraint_match_set: set['_JavaConstraintMatch']) -> set[ConstraintMatch]:
    return {
        ConstraintMatch(constraint_ref=ConstraintRef(package_name=constraint_match
                                                     .getConstraintRef().packageName()[len(_user_package_prefix):],
                                                     constraint_name=constraint_match
                                                     .getConstraintRef().constraintName()),
                        justification=_unwrap_justification(constraint_match.getJustification()),
                        indicted_objects=tuple([unwrap_python_like_object(indicted)
                                                for indicted in cast(list, constraint_match.getIndictedObjectList())]),
                        score=to_python_score(constraint_match.getScore())
                        )
        for constraint_match in constraint_match_set
    }


def _unwrap_justification(justification: Any) -> ConstraintJustification:
    from ai.timefold.solver.core.api.score.stream import (
        DefaultConstraintJustification as _JavaDefaultConstraintJustification)
    if isinstance(justification, _JavaDefaultConstraintJustification):
        fact_list = justification.getFacts()
        return DefaultConstraintJustification(facts=tuple([unwrap_python_like_object(fact)
                                                           for fact in cast(list, fact_list)]),
                                              impact=to_python_score(justification.getImpact()))
    else:
        return unwrap_python_like_object(justification)


def _unwrap_justification_list(justification_list: list[Any]) -> list[ConstraintJustification]:
    return [_unwrap_justification(justification) for justification in justification_list]


class Indictment(Generic[Score_]):
    """
    Explains the `Score` of a `planning_solution`,
    from the opposite side than `ConstraintMatchTotal`.
    Retrievable from `ScoreExplanation.indictment_map`.

    Attributes
    ----------
    constraint_match_set: set[ConstraintMatch]

    score: Score_
        Sum of the constraint_match_set's `ConstraintMatch.score`.

    constraint_match_count: int

    indicted_object : Any
        The object that was involved in causing the constraints to match.
        It is part of `ConstraintMatch.indicted_objects` of every `ConstraintMatch`
        in `constraint_match_set`.

    """

    def __init__(self, delegate: '_JavaIndictment[Score_]'):
        self._delegate = delegate

    @property
    def score(self) -> Score_:
        return to_python_score(self._delegate.getScore())

    @property
    def constraint_match_count(self) -> int:
        return self._delegate.getConstraintMatchCount()

    @property
    def constraint_match_set(self) -> set[ConstraintMatch[Score_]]:
        return _map_constraint_match_set(self._delegate.getConstraintMatchSet())

    @property
    def indicted_object(self) -> Any:
        return unwrap_python_like_object(self._delegate.getIndictedObject())

    def get_justification_list(self, justification_type: Type[Justification_] = None) -> list[Justification_]:
        """
        Retrieve ConstraintJustification instances associated with ConstraintMatches in `constraint_match_set`.
        This is equivalent to retrieving `constraint_match_set` and collecting all `ConstraintMatch.justification`
        objects into a list.

        Parameters
        ----------
        justification_type : Type[Justification_], optional
            If present, only include justifications of the given type in the returned list.

        Returns
        -------
        list[Justification_]
            guaranteed to contain unique instances

        """
        if justification_type is None:
            justification_list = self._delegate.getJustificationList()
        else:
            justification_list = self._delegate.getJustificationList(get_class(justification_type))

        return _unwrap_justification_list(justification_list)


class ScoreExplanation(Generic[Solution_]):
    """
    Build by `SolutionManager.explain`
    to hold `ConstraintMatchTotal`s and `Indictment`s
    necessary to explain the quality of a particular `Score`.

    For a simplified, faster and JSON-friendly alternative, see `ScoreAnalysis`.

    Attributes
    ----------
    solution : Solution_
        Retrieve the `planning_solution` that the score being explained comes from.

    score : Score
        Return the `Score` being explained.
        If the specific Score type used by the `planning_solution`
        is required, retrieve it from the `solution` attribute.

    summary : str
        Returns a diagnostic text
        that explains the solution through the `ConstraintMatch` API
        to identify which constraints or planning entities cause that score quality.

        In case of an infeasible solution, this can help diagnose the cause of that.
        Do not parse the return value, its format may change without warning.
        Instead, to provide this information in a UI or a service,
        use `constraint_match_total_map` and `indictment_map` and convert those into a domain-specific API.

    constraint_match_total_map : dict[str, ConstraintMatchTotal]
        Explains the `Score` of the `score` attribute by splitting it up per `Constraint`.
        The sum of `ConstraintMatchTotal.score` equals the `score` attribute.

    indictment_map: dict[Any, Indictment]
        Explains the impact of each planning entity or problem fact on the `Score`.
        An `Indictment` is basically the inverse of a `ConstraintMatchTotal`:
        it is a Score total for any of the indicted objects.

        The sum of `ConstraintMatchTotal.score` accessible from this `dict`
        differs from `score` because each `ConstraintMatch.score` is counted for each of the indicted objects.
    """
    _delegate: '_JavaScoreExplanation'

    def __init__(self, delegate: '_JavaScoreExplanation'):
        self._delegate = delegate

    @property
    def constraint_match_total_map(self) -> dict[str, ConstraintMatchTotal]:
        return {
            e.getKey()[len(_user_package_prefix):]: ConstraintMatchTotal(
                constraint_ref=ConstraintRef(
                    package_name=e.getValue().getConstraintRef().packageName()[len(_user_package_prefix):],
                                             constraint_name=e.getValue().getConstraintRef().constraintName()),
                constraint_match_count=e.getValue().getConstraintMatchCount(),
                constraint_match_set=_map_constraint_match_set(e.getValue().getConstraintMatchSet()),
                constraint_weight=to_python_score(e.getValue().getConstraintWeight()),
                score=to_python_score(e.getValue().getScore())
            )
            for e in cast(set['_JavaMap.Entry[str, _JavaConstraintMatchTotal]'],
                          self._delegate.getConstraintMatchTotalMap().entrySet())
        }

    @property
    def indictment_map(self) -> dict[Any, Indictment]:
        return {
            unwrap_python_like_object(e.getKey()): Indictment(e.getValue())
            for e in cast(set['_JavaMap.Entry'], self._delegate.getIndictmentMap().entrySet())
        }

    @property
    def score(self) -> 'Score':
        return to_python_score(self._delegate.getScore())

    @property
    def solution(self) -> Solution_:
        from _jpyinterpreter import unwrap_python_like_object
        return unwrap_python_like_object(self._delegate.getSolution())

    @property
    def summary(self) -> str:
        return self._delegate.getSummary()

    def get_justification_list(self, justification_type: Type[Justification_] = None) -> list[Justification_]:
        """
        Explains the `Score` of the `score` attribute for all constraints.
        The return value of this method is determined by several factors:

        - With Constraint Streams,
          the user has an option to provide a custom justification mapping, implementing `ConstraintJustification`.
          If provided, every ConstraintMatch of such constraint will be associated with this custom justification class.
          Every constraint
          not associated with a custom justification class will be associated with `DefaultConstraintJustification`.

        - With ConstraintMatchAwareIncrementalScoreCalculator, every `ConstraintMatch`
          will be associated with the justification class that the user created it with.

        Parameters
        ----------
        justification_type : Type[Justification_], optional
            If present, only include justifications of the given type in the returned list.

        Returns
        -------
        list[Justification_]
             all constraint matches, optionally only those of a given class.
        """
        if justification_type is None:
            justification_list = self._delegate.getJustificationList()
        else:
            justification_list = self._delegate.getJustificationList(get_class(justification_type))

        return _unwrap_justification_list(justification_list)


class MatchAnalysis(Generic[Score_]):
    """
    Users should never create instances of this type directly.
    It is available transitively via `SolutionManager.analyze`.

    Attributes
    ----------
    constraint_ref : ConstraintRef
    score : Score_
    justification : ConstraintJustification
    """
    _delegate: '_JavaMatchAnalysis'

    def __init__(self, delegate: '_JavaMatchAnalysis'):
        self._delegate = delegate

    @property
    def constraint_ref(self) -> ConstraintRef:
        return ConstraintRef(package_name=self._delegate.constraintRef().packageName()[len(_user_package_prefix):],
                             constraint_name=self._delegate.constraintRef().constraintName())

    @property
    def score(self) -> Score_:
        return to_python_score(self._delegate.score())

    @property
    def justification(self) -> ConstraintJustification:
        return _unwrap_justification(self._delegate.justification())


class ConstraintAnalysis(Generic[Score_]):
    """
    Users should never create instances of this type directly.
    It is available transitively via `SolutionManager.analyze`.

    Attributes
    ----------
    constraint_ref : ConstraintRef
    weight : Score_
    score : Score_
    matches : list[MatchAnalysis]
         None if analysis not available;
         empty if constraint has no matches,
         but still non-zero constraint weight; non-empty if constraint has matches.
         This is a list to simplify access to individual elements,
         but it contains no duplicates just like `set` wouldn't.
    summary : str
        Returns a diagnostic text
        that explains part of the score quality through the ConstraintAnalysis API.
    match_count : int
        Return the match count of the constraint.
    """
    _delegate: '_JavaConstraintAnalysis[Score_]'

    def __init__(self, delegate: '_JavaConstraintAnalysis[Score_]'):
        self._delegate = delegate
        delegate.constraintRef()

    def __str__(self):
        return self.summary

    @property
    def constraint_ref(self) -> ConstraintRef:
        return ConstraintRef(package_name=self._delegate.constraintRef().packageName()[len(_user_package_prefix):],
                             constraint_name=self._delegate.constraintRef().constraintName())

    @property
    def constraint_package(self) -> str:
        return self._delegate.constraintPackage()[len(_user_package_prefix):]

    @property
    def constraint_name(self) -> str:
        return self._delegate.constraintName()

    @property
    def weight(self) -> Optional[Score_]:
        return to_python_score(self._delegate.weight())

    @property
    def matches(self) -> list[MatchAnalysis[Score_]]:
        return [MatchAnalysis(match_analysis)
                for match_analysis in cast(list['_JavaMatchAnalysis[Score_]'], self._delegate.matches())]

    @property
    def match_count(self) -> int:
        return self._delegate.matchCount()

    @property
    def score(self) -> Score_:
        return to_python_score(self._delegate.score())

    @property
    def summary(self) -> str:
        return self._delegate.summarize()


class ScoreAnalysis:
    """
    Represents the breakdown of a `Score` into individual `ConstraintAnalysis` instances,
    one for each constraint.
    Compared to `ScoreExplanation`, this is JSON-friendly and faster to generate.

    In order to be fully serializable to JSON,
    MatchAnalysis instances must be serializable to JSON
    and that requires any implementations of `ConstraintJustification` to be serializable to JSON.
    This is the responsibility of the user.

    For deserialization from JSON, the user needs to provide the deserializer themselves.
    This is due to the fact that, once the `ScoreAnalysis` is received over the wire,
    we no longer know which Score type or `ConstraintJustification` type was used.
    The user has all of that information in their domain model,
    and so they are the correct party to provide the deserializer.

    Attributes
    ----------
    constraint_map : dict[ConstraintRef, ConstraintAnalysis]
        for each constraint identified by its `constraint_ref`,
        the `ConstraintAnalysis` that describes the impact of that constraint on the overall score.
        Constraints are present even if they have no matches,
        unless their weight is zero; zero-weight constraints are not present.
        Entries in the map have a stable iteration order; items are ordered first by `ConstraintAnalysis.weight,
        then by `ConstraintAnalysis.constraint_ref`.

    constraint_analyses : list[ConstraintAnalysis]
        Individual ConstraintAnalysis instances that make up this ScoreAnalysis.

    summary : str
        Returns a diagnostic text that explains the solution through the `ConstraintAnalysis` API to identify which
        Constraints cause that score quality.
        The string is built fresh every time the method is called.

        In case of an infeasible solution, this can help diagnose the cause of that.

        Do not parse the return value, its format may change without warning.
        Instead, provide this information in a UI or a service,
        use `constraintAnalyses()`
        and convert those into a domain-specific API.

    is_solution_initialized : bool

    Notes
    -----
    the constructors of this record are off-limits.
    We ask users to use exclusively `SolutionManager.analyze` to obtain instances of this record.
    """
    _delegate: '_JavaScoreAnalysis'

    def __init__(self, delegate: '_JavaScoreAnalysis'):
        self._delegate = delegate

    def __str__(self):
        return self.summary

    def __sub__(self, other):
        return self.diff(other)

    @property
    def score(self) -> 'Score':
        return to_python_score(self._delegate.score())

    @property
    def constraint_map(self) -> dict[ConstraintRef, ConstraintAnalysis]:
        return {
            ConstraintRef(package_name=e.getKey().packageName()[len(_user_package_prefix):],
                          constraint_name=e.getKey().constraintName())
            : ConstraintAnalysis(e.getValue())
            for e in cast(set['_JavaMap.Entry[_JavaConstraintRef, _JavaConstraintAnalysis]'],
                          self._delegate.constraintMap().entrySet())
        }

    @property
    def constraint_analyses(self) -> list[ConstraintAnalysis]:
        return [
            ConstraintAnalysis(analysis) for analysis in cast(
                list['_JavaConstraintAnalysis[Score]'], self._delegate.constraintAnalyses())
        ]

    @overload
    def constraint_analysis(self, constraint_package: str, constraint_name: str) -> ConstraintAnalysis:
        ...

    @overload
    def constraint_analysis(self, constraint_ref: 'ConstraintRef') -> ConstraintAnalysis:
        ...

    def constraint_analysis(self, *args) -> ConstraintAnalysis:
        """
        Performs a lookup on `constraint_map`.

        Parameters
        ----------
        *args: *tuple[str, str] | *tuple[ConstraintRef]
            Either two strings or a single ConstraintRef can be passed as positional arguments.
            If two strings are passed, they are taken to be the constraint package and constraint name, respectively.
            If a ConstraintRef is passed, it is used to perform the lookup.

        Returns
        -------
        ConstraintAnalysis
            None if no constraint matches of such constraint are present
        """
        if len(args) == 1:
            return ConstraintAnalysis(self._delegate.getConstraintAnalysis(args[0]._to_java()))
        else:
            return ConstraintAnalysis(self._delegate.getConstraintAnalysis(f'{_user_package_prefix}{args[0]}', args[1]))

    @property
    def summary(self) -> str:
        return self._delegate.summarize()

    @property
    def is_solution_initialized(self) -> bool:
        return self._delegate.isSolutionInitialized()

    def diff(self, other: 'ScoreAnalysis') -> 'ScoreAnalysis':
        """
        Compare this `ScoreAnalysis to another `ScoreAnalysis`
        and retrieve the difference between them.
        The comparison is in the direction of `this - other`.

        Example: if `this` has a score of 100 and `other` has a score of 90,
        the returned score will be 10.
        If this and other were inverted, the score would have been -10.
        The same applies to all other properties of `ScoreAnalysis`.

        In order to properly diff `MatchAnalysis` against each other,
        we rely on the user implementing `ConstraintJustification` equality correctly.
        In other words, the diff will consider two justifications equal if the user says they are equal,
        and it expects the hash code to be consistent with equals.

        If one `ScoreAnalysis` provides `MatchAnalysis` and the other doesn't, exception is thrown.
        Such `ScoreAnalysis` instances are mutually incompatible.

        Parameters
        ----------
        other : ScoreAnalysis

        Returns
        -------
        ScoreExplanation
            The `ScoreAnalysis` corresponding to the diff.
        """
        return ScoreAnalysis(self._delegate.diff(other._delegate))


__all__ = ['ScoreExplanation',
           'ConstraintRef', 'ConstraintMatch', 'ConstraintMatchTotal',
           'ConstraintJustification', 'DefaultConstraintJustification', 'Indictment',
           'ScoreAnalysis', 'ConstraintAnalysis', 'MatchAnalysis']
