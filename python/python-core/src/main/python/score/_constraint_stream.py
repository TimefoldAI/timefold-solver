from .._timefold_java_interop import get_class
import jpype.imports  # noqa
from jpype import JClass
from typing import TYPE_CHECKING, Type, Callable, overload, TypeVar, Generic, Any, Union, cast
from decimal import Decimal

if TYPE_CHECKING:
    from ai.timefold.solver.core.api.score.stream.uni import (UniConstraintCollector,
                                                              UniConstraintStream as _JavaUniConstraintStream)
    from ai.timefold.solver.core.api.score.stream.bi import (BiJoiner, BiConstraintCollector,
                                                             BiConstraintStream as _JavaBiConstraintStream)
    from ai.timefold.solver.core.api.score.stream.tri import (TriJoiner, TriConstraintCollector,
                                                              TriConstraintStream as _JavaTriConstraintStream)
    from ai.timefold.solver.core.api.score.stream.quad import (QuadJoiner, QuadConstraintCollector,
                                                               QuadConstraintStream as _JavaQuadConstraintStream)
    from ai.timefold.solver.core.api.score.stream.penta import PentaJoiner

#  Class type variables
A = TypeVar('A')
B = TypeVar('B')
C = TypeVar('C')
D = TypeVar('D')
ScoreType = TypeVar('ScoreType', bound='Score')


class UniConstraintStream(Generic[A]):
    """
    A ConstraintStream that matches one fact.
    """
    delegate: '_JavaUniConstraintStream[A]'
    package: str
    a_type: Type[A]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: '_JavaUniConstraintStream[A]', package: str,
                 a_type: Type[A]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type

    def get_constraint_factory(self):
        """
        The ConstraintFactory that build this.
        """
        return ConstraintFactory(self.delegate.getConstraintFactory())

    def filter(self, predicate: Callable[[A], bool]) -> 'UniConstraintStream[A]':
        """
        Exhaustively test each fact against the predicate and match if the predicate returns ``True``.
        """
        translated_predicate = predicate_cast(predicate, self.a_type)
        return UniConstraintStream(self.delegate.filter(translated_predicate), self.package,
                                   self.a_type)

    def join(self, unistream_or_type: Union['UniConstraintStream[B_]', Type[B_]], *joiners: 'BiJoiner[A, B_]') -> \
            'BiConstraintStream[A,B_]':
        """
        Create a new `BiConstraintStream` for every combination of A and B that satisfies all specified joiners.
        """
        b_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            b_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            b_type = get_class(unistream_or_type)
            unistream_or_type = b_type

        join_result = self.delegate.join(unistream_or_type, extract_joiners(joiners, self.a_type, b_type))
        return BiConstraintStream(join_result, self.package,
                                  self.a_type, b_type)

    @overload
    def if_exists(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> 'UniConstraintStream[A]':
        ...

    @overload
    def if_exists(self, other_stream: 'UniConstraintStream[B_]', *joiners: 'BiJoiner[A, B_]') \
            -> 'UniConstraintStream[A]':
        ...

    def if_exists(self, unistream_or_type: Union['UniConstraintStream[B_]', Type[B_]],
                  *joiners: 'BiJoiner[A, B_]') -> 'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A where B exists that satisfies all specified joiners.
        """
        b_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            b_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            b_type = get_class(unistream_or_type)
            unistream_or_type = b_type
        return UniConstraintStream(self.delegate.ifExists(unistream_or_type,
                                                          extract_joiners(joiners,
                                                                          self.a_type, b_type)),
                                   self.package, self.a_type)

    def if_exists_including_unassigned(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> \
            'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A where B exists that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return UniConstraintStream(self.delegate.ifExistsIncludingUnassigned(item_type,
                                                                             extract_joiners(joiners,
                                                                                             self.a_type, item_type)),
                                   self.package, self.a_type)

    def if_exists_other(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> 'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A, if another A exists that does not equal the first,
        and for which all specified joiners are satisfied.
        """
        item_type = get_class(item_type)
        return UniConstraintStream(self.delegate.ifExistsOther(cast(Type['A_'], item_type),
                                                               extract_joiners(joiners,
                                                                               self.a_type, item_type)),
                                   self.package, self.a_type)

    def if_exists_other_including_unassigned(self, item_type: Type, *joiners: 'BiJoiner') -> \
            'UniConstraintStream':
        """
        Create a new UniConstraintStream for every A, if another A exists that does not equal the first.
        For classes decorated with `planning_entity`, this method also includes entities with ``None`` variables,
        or entities that are not assigned to any list variable.
        """
        item_type = get_class(item_type)
        return UniConstraintStream(self.delegate.ifExistsOtherIncludingUnassigned(cast(Type['A_'], item_type),
                                                                                  extract_joiners(joiners,
                                                                                                  self.a_type, item_type)),
                                   self.package, self.a_type)

    @overload
    def if_not_exists(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> 'UniConstraintStream[A]':
        ...

    @overload
    def if_not_exists(self, other_stream: 'UniConstraintStream[B_]', *joiners: 'BiJoiner[A, B_]') \
            -> 'UniConstraintStream[A]':
        ...

    def if_not_exists(self, unistream_or_type: Union['UniConstraintStream[B_]', Type[B_]],
                      *joiners: 'BiJoiner[A, B_]') -> 'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A where B does not exist that satisfies all specified joiners.
        """
        b_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            b_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            b_type = get_class(unistream_or_type)
            unistream_or_type = b_type
        return UniConstraintStream(self.delegate.ifNotExists(unistream_or_type,
                                                             extract_joiners(joiners,
                                                                             self.a_type, b_type)),
                                   self.package, self.a_type)

    def if_not_exists_including_unassigned(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> \
            'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A where B does not exist that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return UniConstraintStream(self.delegate.ifNotExistsIncludingUnassigned(item_type,
                                                                                extract_joiners(joiners,
                                                                                                self.a_type, item_type)),
                                   self.package, self.a_type)

    def if_not_exists_other(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> \
            'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A where B does not exist that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return UniConstraintStream(self.delegate.ifNotExistsOther(cast(Type['A_'], item_type),
                                                                  extract_joiners(joiners,
                                                                                  self.a_type,
                                                                                  item_type)),
                                   self.package, self.a_type)

    def if_not_exists_other_including_unassigned(self, item_type: Type[B_], *joiners: 'BiJoiner[A, B_]') -> \
            'UniConstraintStream[A]':
        """
        Create a new `UniConstraintStream` for every A where a different A does not exist
        that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return UniConstraintStream(self.delegate.ifNotExistsOtherIncludingUnassigned(cast(Type['A_'], item_type),
                                                                                     extract_joiners(joiners,
                                                                                                     self.a_type, item_type)),
                                   self.package, self.a_type)

    @overload
    def group_by(self, key_mapping: Callable[[A], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'UniConstraintCollector[A, Any, A_]') -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_],
                 second_key_mapping: Callable[[A], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A], A_],
                 collector: 'UniConstraintCollector[A, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'UniConstraintCollector[A, Any, A_]',
                 second_collector: 'UniConstraintCollector[A, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 third_key_mapping: Callable[[A], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 collector: 'UniConstraintCollector[A, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A], A_], first_collector: 'UniConstraintCollector[A, Any, B_]',
                 second_collector: 'UniConstraintCollector[A, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'UniConstraintCollector[A, Any, A_]',
                 second_collector: 'UniConstraintCollector[A, Any, B_]',
                 third_collector: 'UniConstraintCollector[A, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 third_key_mapping: Callable[[A], C_],
                 fourth_key_mapping: Callable[[A], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 third_key_mapping: Callable[[A], C_],
                 collector: 'UniConstraintCollector[A, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A], A_], second_key_mapping: Callable[[A], B_],
                 first_collector: 'UniConstraintCollector[A, Any, C_]',
                 second_collector: 'UniConstraintCollector[A, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A], A_], first_collector: 'UniConstraintCollector[A, Any, B_]',
                 second_collector: 'UniConstraintCollector[A, Any, C_]',
                 third_collector: 'UniConstraintCollector[A, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'UniConstraintCollector[A, Any, A_]',
                 second_collector: 'UniConstraintCollector[A, Any, B_]',
                 third_collector: 'UniConstraintCollector[A, Any, C_]',
                 fourth_collector: 'UniConstraintCollector[A, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """
        Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Returns
        -------
        UniConstraintStream | BiConstraintStream | TriConstraintStream | QuadConstraintStream
            The type of stream returned depends on the number of arguments passed:

            - 1 -> UniConstraintStream

            - 2 -> BiConstraintStream

            - 3 -> TriConstraintStream

            - 4 -> QuadConstraintStream

        Examples
        --------

        Count the items in this stream; returns Uni[int]

        >>> group_by(ConstraintCollectors.count())

        Count the number of shifts each employee has; returns Bi[Employee]

        >>> group_by(lambda shift: shift.employee, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date, datetime.date]

        >>> group_by(lambda shift: shift.employee,
        ...          ConstraintCollectors.min(lambda shift: shift.date)
        ...          ConstraintCollectors.max(lambda shift: shift.date))
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type)

    @overload
    def map(self, mapping_function: Callable[[A], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A], A_],
            mapping_function2: Callable[[A], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A], A_], mapping_function2: Callable[[A], B_],
            mapping_function3: Callable[[A], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A], A_], mapping_function2: Callable[[A], B_],
            mapping_function3: Callable[[A], C_],
            mapping_function4: Callable[[A], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def map(self, *mapping_functions):
        """
        Transforms the stream in such a way that tuples are remapped using the given function.
        """
        if len(mapping_functions) == 0:
            raise ValueError(f'At least one mapping function is required for map.')
        if len(mapping_functions) > 4:
            raise ValueError(f'At most four mapping functions can be passed to map (got {len(mapping_functions)}).')
        translated_functions = tuple(map(lambda mapping_function: function_cast(mapping_function, self.a_type),
                                         mapping_functions))
        if len(mapping_functions) == 1:
            return UniConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 2:
            return BiConstraintStream(self.delegate.map(*translated_functions), self.package,

                                      JClass('java.lang.Object'), JClass('java.lang.Object'))
        if len(mapping_functions) == 3:
            return TriConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'), JClass('java.lang.Object'),
                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 4:
            return QuadConstraintStream(self.delegate.map(*translated_functions), self.package,

                                        JClass('java.lang.Object'), JClass('java.lang.Object'),
                                        JClass('java.lang.Object'), JClass('java.lang.Object'))
        raise RuntimeError(f'Impossible state: missing case for {len(mapping_functions)}.')

    @overload
    def expand(self, mapping_function: Callable[[A], B_]) -> 'BiConstraintStream[A, B_]':
        ...

    @overload
    def expand(self, mapping_function: Callable[[A], B_],
               mapping_function2: Callable[[A], C_]) -> 'TriConstraintStream[A, B_, C_]':
        ...

    @overload
    def expand(self, mapping_function: Callable[[A], B_], mapping_function2: Callable[[A], C_],
               mapping_function3: Callable[[A], D_]) -> 'TriConstraintStream[A, B_, C_, D_]':
        ...

    def expand(self, *mapping_functions):
        """
        Tuple expansion is a special case of tuple mapping
        which only increases stream cardinality and can not introduce duplicate tuples.
        It enables you to add extra facts to each tuple in a constraint stream by applying a mapping function to it.
        This is useful in situations where an expensive computations needs to be cached for use later in the stream.
        """
        if len(mapping_functions) == 0:
            raise ValueError(f'At least one mapping function is required for expand.')
        if len(mapping_functions) > 3:
            raise ValueError(
                f'At most three mapping functions can be passed to expand on a UniStream '
                f'(got {len(mapping_functions)}).')
        translated_functions = tuple(map(lambda mapping_function: function_cast(mapping_function, self.a_type),
                                         mapping_functions))
        if len(mapping_functions) == 1:
            return BiConstraintStream(self.delegate.expand(*translated_functions), self.package,

                                      self.a_type, JClass('java.lang.Object'))
        if len(mapping_functions) == 2:
            return TriConstraintStream(self.delegate.expand(*translated_functions), self.package,

                                       self.a_type, JClass('java.lang.Object'), JClass('java.lang.Object'))
        if len(mapping_functions) == 3:
            return QuadConstraintStream(self.delegate.expand(*translated_functions), self.package,

                                        self.a_type, JClass('java.lang.Object'), JClass('java.lang.Object'),
                                        JClass('java.lang.Object'))
        raise RuntimeError(f'Impossible state: missing case for {len(mapping_functions)}.')

    def flatten_last(self, flattening_function: Callable[[A], A_]) -> 'UniConstraintStream[A_]':
        """
        Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.
        """
        translated_function = function_cast(flattening_function, self.a_type)
        return UniConstraintStream(self.delegate.flattenLast(translated_function), self.package,

                                   JClass('java.lang.Object'))

    def distinct(self) -> 'UniConstraintStream[A]':
        """
        Transforms the stream in such a way that all the tuples going through it are distinct.
        """
        return UniConstraintStream(self.delegate.distinct(), self.package, self.a_type)

    @overload
    def concat(self, other: 'UniConstraintStream[A]') -> 'UniConstraintStream[A]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B_]') -> 'BiConstraintStream[A, B_]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B_]', padding_b: Callable[[A], B_]) -> 'BiConstraintStream[A, B_]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B_, C_]') -> 'TriConstraintStream[A, B_, C_]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B_, C_]', padding_b: Callable[[A], B_],
               padding_c: Callable[[A], C_]) -> 'TriConstraintStream[A, B_, C_]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B_, C_, D_]') -> 'QuadConstraintStream[A, B_, C_, D_]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B_, C_, D_]', padding_b: Callable[[A], B_],
               padding_c: Callable[[A], C_], padding_d: Callable[[A], D_]) -> 'QuadConstraintStream[A, B_, C_, D_]':
        ...

    def concat(self, other, padding_b=None, padding_c=None, padding_d=None):
        """
        The concat building block allows you
        to create a constraint stream containing tuples of two other constraint streams.
        If join acts like a cartesian product of two lists, concat acts like a concatenation of two lists.
        Unlike union of sets, concatenation of lists repeats duplicated elements.
        If the two constraint concatenating streams share tuples, which happens e.g.
        when they come from the same source of data, the tuples will be repeated downstream.
        If this is undesired, use the distinct building block.
        """
        specified_count = sum(x is not None for x in [padding_b, padding_c, padding_d])
        if isinstance(other, UniConstraintStream):
            if specified_count == 0:
                return UniConstraintStream(self.delegate.concat(other.delegate), self.package,
                                           self.a_type)
            else:
                raise ValueError(f'Concatenating UniConstraintStreams requires no padding functions, '
                                 f'got {specified_count} instead.')
        elif isinstance(other, BiConstraintStream):
            if specified_count == 0:
                return BiConstraintStream(self.delegate.concat(other.delegate), self.package,
                                          self.a_type, other.b_type)
            elif specified_count > 1:
                raise ValueError(f'Concatenating Uni and BiConstraintStream requires 1 padding function, '
                                 f'got {specified_count} instead.')
            elif padding_b is None:
                raise ValueError(f'Concatenating Uni and BiConstraintStream requires padding_b to be provided.')
            return BiConstraintStream(self.delegate.concat(other.delegate, padding_b), self.package,
                                      self.a_type, other.b_type)
        elif isinstance(other, TriConstraintStream):
            if specified_count == 0:
                return TriConstraintStream(self.delegate.concat(other.delegate), self.package,
                                           self.a_type, other.b_type, other.c_type)
            elif specified_count != 2:
                raise ValueError(f'Concatenating Uni and TriConstraintStream requires 2 padding functions, '
                                 f'got {specified_count} instead.')
            elif padding_d is not None:
                raise ValueError(f'Concatenating Uni and TriConstraintStream requires '
                                 f'padding_b and padding_c to be provided.')
            return TriConstraintStream(self.delegate.concat(other.delegate, padding_b, padding_c), self.package,
                                       self.a_type, other.b_type, other.c_type)
        elif isinstance(other, QuadConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate),
                                            self.package, self.a_type, other.b_type, other.c_type, other.d_type)
            elif specified_count != 3:
                raise ValueError(f'Concatenating Uni and QuadConstraintStream requires 3 padding functions, '
                                 f'got {specified_count} instead.')
            return QuadConstraintStream(self.delegate.concat(other.delegate, padding_b, padding_c, padding_d),
                                        self.package, self.a_type, other.b_type, other.c_type, other.d_type)
        else:
            raise RuntimeError(f'Unhandled constraint stream type {type(other)}.')

    def complement(self, cls: type[A]) -> 'UniConstraintStream[A]':
        """
        Adds to the stream all instances of a given class which are not yet present in it.
        These instances must be present in the solution,
        which means the class needs to be either a planning entity or a problem fact.

        Parameters
        ----------
        cls : Type[A]
            the type of the instances to add to the stream.
        """
        result = self.delegate.complement(get_class(cls))
        return UniConstraintStream(result, self.package, self.a_type)

    def penalize(self, constraint_weight: ScoreType, match_weigher: Callable[[A], int] = None) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A], int], optional
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        UniConstraintBuilder
            a `UniConstraintBuilder`
        """
        if match_weigher is None:
            return UniConstraintBuilder(self.delegate.penalize(constraint_weight), self.a_type)
        else:
            return UniConstraintBuilder(self.delegate.penalizeLong(constraint_weight,
                                                                   to_long_function_cast(match_weigher, self.a_type)),
                                        self.a_type)

    def penalize_decimal(self, constraint_weight: ScoreType, match_weigher: Callable[[A], Decimal] = None) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A], Decimal], optional
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        UniConstraintBuilder
            a `UniConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return UniConstraintBuilder(self.delegate.penalizeBigDecimal(constraint_weight), self.a_type)
        else:
            return UniConstraintBuilder(self.delegate.penalizeBigDecimal(constraint_weight,
                                                                         function_cast(match_weigher,
                                                                                       self.a_type,
                                                                                       return_type=BigDecimal)),
                                        self.a_type)

    def reward(self, constraint_weight: ScoreType, match_weigher: Callable[[A], int] = None) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        UniConstraintBuilder
            a `UniConstraintBuilder`
        """
        if match_weigher is None:
            return UniConstraintBuilder(self.delegate.reward(constraint_weight), self.a_type)
        else:
            return UniConstraintBuilder(self.delegate.rewardLong(constraint_weight,
                                                                 to_long_function_cast(match_weigher, self.a_type)),
                                        self.a_type)

    def reward_decimal(self, constraint_weight: ScoreType, match_weigher: Callable[[A], Decimal] = None) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A], Decimal], optional
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        UniConstraintBuilder
            a `UniConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return UniConstraintBuilder(self.delegate.reward(constraint_weight), self.a_type)
        else:
            return UniConstraintBuilder(self.delegate.rewardBigDecimal(constraint_weight,
                                                                       function_cast(match_weigher,
                                                                                     self.a_type,
                                                                                     return_type=BigDecimal)),
                                        self.a_type)

    def impact(self, constraint_weight: ScoreType, match_weigher: Callable[[A], int] = None) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        UniConstraintBuilder
            a `UniConstraintBuilder`
        """
        if match_weigher is None:
            return UniConstraintBuilder(self.delegate.impact(constraint_weight), self.a_type)
        else:
            return UniConstraintBuilder(self.delegate.impactLong(constraint_weight,
                                                                 to_long_function_cast(match_weigher,
                                                                                       self.a_type)),
                                        self.a_type)

    def impact_decimal(self, constraint_weight: ScoreType, match_weigher: Callable[[A], Decimal] = None) -> \
            'UniConstraintBuilder[A, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A], Decimal], optional
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        UniConstraintBuilder
            a `UniConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return UniConstraintBuilder(self.delegate.impact(constraint_weight), self.a_type)
        else:
            return UniConstraintBuilder(self.delegate.impactBigDecimal(constraint_weight,
                                                                       function_cast(match_weigher,
                                                                                     self.a_type,
                                                                                     return_type=BigDecimal)),
                                        self.a_type)


class BiConstraintStream(Generic[A, B]):
    """
    A ConstraintStream that matches two facts.
    """
    delegate: '_JavaBiConstraintStream[A,B]'
    package: str
    a_type: Type[A]
    b_type: Type[B]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: '_JavaBiConstraintStream[A,B]', package: str,
                 a_type: Type[A], b_type: Type[B]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type
        self.b_type = b_type

    def get_constraint_factory(self):
        """
        The ConstraintFactory that build this.
        """
        return ConstraintFactory(self.delegate.getConstraintFactory())

    def filter(self, predicate: Callable[[A, B], bool]) -> 'BiConstraintStream[A,B]':
        """
        Exhaustively test each fact against the predicate and match if the predicate returns ``True``.
        """
        translated_predicate = predicate_cast(predicate, self.a_type, self.b_type)
        return BiConstraintStream(self.delegate.filter(translated_predicate), self.package,

                                  self.a_type,
                                  self.b_type)

    def join(self, unistream_or_type: Union[UniConstraintStream[C_], Type[C_]],
             *joiners: 'TriJoiner[A,B,C_]') -> 'TriConstraintStream[A,B,C_]':
        """
        Create a new `TriConstraintStream` for every combination of A, B and C that satisfies all specified joiners.
        """
        c_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            c_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            c_type = get_class(unistream_or_type)
            unistream_or_type = c_type

        join_result = self.delegate.join(unistream_or_type, extract_joiners(joiners,
                                                                            self.a_type, self.b_type, c_type))
        return TriConstraintStream(join_result, self.package,
                                   self.a_type, self.b_type, c_type)

    @overload
    def if_exists(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') -> 'BiConstraintStream[A,B]':
        ...

    @overload
    def if_exists(self, other_stream: 'UniConstraintStream[C_]', *joiners: 'TriJoiner[A, B, C_]') \
            -> 'BiConstraintStream[A,B]':
        ...

    def if_exists(self, unistream_or_type: Union['UniConstraintStream[C_]', Type[C_]],
                  *joiners: 'TriJoiner[A, B, C_]') -> 'BiConstraintStream[A,B]':
        """
        Create a new `BiConstraintStream` for every A, B where C exists that satisfies all specified joiners.
        """
        c_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            c_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            c_type = get_class(unistream_or_type)
            unistream_or_type = c_type
        return BiConstraintStream(self.delegate.ifExists(unistream_or_type,
                                                         extract_joiners(joiners,
                                                                         self.a_type, self.b_type, c_type)),
                                  self.package, self.a_type, self.b_type)

    def if_exists_including_unassigned(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') -> \
            'BiConstraintStream[A,B]':
        """
        Create a new `BiConstraintStream` for every A, B where C exists that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return BiConstraintStream(self.delegate.ifExistsIncludingUnassigned(item_type, extract_joiners(joiners,
                                                                                                       self.a_type,
                                                                                                       self.b_type,
                                                                                                       item_type)),
                                  self.package, self.a_type, self.b_type)

    @overload
    def if_not_exists(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') -> 'BiConstraintStream[A,B]':
        ...

    @overload
    def if_not_exists(self, other_stream: 'UniConstraintStream[C_]', *joiners: 'TriJoiner[A, B, C_]')\
            -> 'BiConstraintStream[A,B]':
        ...

    def if_not_exists(self, unistream_or_type: Union['UniConstraintStream[C_]', Type[C_]],
                      *joiners: 'TriJoiner[A, B, C_]') -> 'BiConstraintStream[A,B]':
        """
        Create a new `BiConstraintStream` for every A, B where C does not exist that satisfies all specified joiners.
        """
        c_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            c_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            c_type = get_class(unistream_or_type)
            unistream_or_type = c_type
        return BiConstraintStream(self.delegate.ifNotExists(unistream_or_type,
                                                            extract_joiners(joiners,
                                                                            self.a_type, self.b_type, c_type)),
                                  self.package, self.a_type, self.b_type)

    def if_not_exists_including_unassigned(self, item_type: Type[C_], *joiners: 'TriJoiner[A, B, C_]') -> \
            'BiConstraintStream[A,B]':
        """
        Create a new `BiConstraintStream` for every A, B where C does not exist that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return BiConstraintStream(self.delegate.ifNotExistsIncludingUnassigned(item_type,
                                                                               extract_joiners(joiners,
                                                                                               self.a_type, self.b_type,
                                                                                               item_type)),
                                  self.package, self.a_type, self.b_type)

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'BiConstraintCollector[A, B, Any, A_]') -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_],
                 second_key_mapping: Callable[[A, B], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_],
                 collector: 'BiConstraintCollector[A, B, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'BiConstraintCollector[A, B, Any, A_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 third_key_mapping: Callable[[A, B], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 collector: 'BiConstraintCollector[A, B, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_], first_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'BiConstraintCollector[A, B, Any, A_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 third_collector: 'BiConstraintCollector[A, B, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 third_key_mapping: Callable[[A, B], C_],
                 fourth_key_mapping: Callable[[A, B], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 third_key_mapping: Callable[[A, B], C_],
                 collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B], A_], second_key_mapping: Callable[[A, B], B_],
                 first_collector: 'BiConstraintCollector[A, B, Any, C_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B], A_], first_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, C_]',
                 third_collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'BiConstraintCollector[A, B, Any, A_]',
                 second_collector: 'BiConstraintCollector[A, B, Any, B_]',
                 third_collector: 'BiConstraintCollector[A, B, Any, C_]',
                 fourth_collector: 'BiConstraintCollector[A, B, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """
        Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Returns
        -------
        UniConstraintStream | BiConstraintStream | TriConstraintStream | QuadConstraintStream
            The type of stream returned depends on the number of arguments passed:

            - 1 -> UniConstraintStream

            - 2 -> BiConstraintStream

            - 3 -> TriConstraintStream

            - 4 -> QuadConstraintStream

        Examples
        --------

        Count the items in this stream; returns Uni[int]

        >>> group_by(ConstraintCollectors.count())

        Count the number of shifts each employee has; returns Bi[Employee]

        >>> group_by(lambda shift: shift.employee, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date, datetime.date]

        >>> group_by(lambda shift: shift.employee,
        ...          ConstraintCollectors.min(lambda shift: shift.date)
        ...          ConstraintCollectors.max(lambda shift: shift.date))
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type, self.b_type)

    @overload
    def map(self, mapping_function: Callable[[A, B], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B], A_],
            mapping_function2: Callable[[A, B], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B], A_], mapping_function2: Callable[[A, B], B_],
            mapping_function3: Callable[[A, B], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B], A_], mapping_function2: Callable[[A, B], B_],
            mapping_function3: Callable[[A, B], C_],
            mapping_function4: Callable[[A, B], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def map(self, *mapping_functions):
        """
        Transforms the stream in such a way that tuples are remapped using the given function.
        """
        if len(mapping_functions) == 0:
            raise ValueError(f'At least one mapping function is required for map.')
        if len(mapping_functions) > 4:
            raise ValueError(f'At most four mapping functions can be passed to map (got {len(mapping_functions)}).')
        translated_functions = tuple(map(lambda mapping_function: function_cast(mapping_function, self.a_type,
                                                                                self.b_type),
                                         mapping_functions))
        if len(mapping_functions) == 1:
            return UniConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 2:
            return BiConstraintStream(self.delegate.map(*translated_functions), self.package,

                                      JClass('java.lang.Object'), JClass('java.lang.Object'))
        if len(mapping_functions) == 3:
            return TriConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'), JClass('java.lang.Object'),
                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 4:
            return QuadConstraintStream(self.delegate.map(*translated_functions), self.package,

                                        JClass('java.lang.Object'), JClass('java.lang.Object'),
                                        JClass('java.lang.Object'), JClass('java.lang.Object'))
        raise RuntimeError(f'Impossible state: missing case for {len(mapping_functions)}.')

    @overload
    def expand(self, mapping_function: Callable[[A, B], C_]) -> 'TriConstraintStream[A, B, C_]':
        ...

    @overload
    def expand(self, mapping_function: Callable[[A, B], C_],
               mapping_function2: Callable[[A, B], D_]) -> 'QuadConstraintStream[A, B, C_, D_]':
        ...

    def expand(self, *mapping_functions):
        """
        Tuple expansion is a special case of tuple mapping
        which only increases stream cardinality and can not introduce duplicate tuples.
        It enables you to add extra facts to each tuple in a constraint stream by applying a mapping function to it.
        This is useful in situations where an expensive computations needs to be cached for use later in the stream.
        """
        if len(mapping_functions) == 0:
            raise ValueError(f'At least one mapping function is required for expand.')
        if len(mapping_functions) > 2:
            raise ValueError(
                f'At most two mapping functions can be passed to expand on a BiStream (got {len(mapping_functions)}).')
        translated_functions = tuple(
            map(lambda mapping_function: function_cast(mapping_function, self.a_type, self.b_type),
                mapping_functions))
        if len(mapping_functions) == 1:
            return TriConstraintStream(self.delegate.expand(*translated_functions), self.package,

                                       self.a_type, self.b_type, JClass('java.lang.Object'))
        if len(mapping_functions) == 2:
            return QuadConstraintStream(self.delegate.expand(*translated_functions), self.package,

                                        self.a_type, self.b_type, JClass('java.lang.Object'),
                                        JClass('java.lang.Object'))
        raise RuntimeError(f'Impossible state: missing case for {len(mapping_functions)}.')

    def flatten_last(self, flattening_function: Callable[[B], B_]) -> 'BiConstraintStream[A,B_]':
        """
        Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.
        """
        translated_function = function_cast(flattening_function, self.b_type)
        return BiConstraintStream(self.delegate.flattenLast(translated_function), self.package,

                                  self.a_type, JClass('java.lang.Object'))

    def distinct(self) -> 'BiConstraintStream[A,B]':
        """
        Transforms the stream in such a way that all the tuples going through it are distinct.
        """
        return BiConstraintStream(self.delegate.distinct(), self.package,
                                  self.a_type, self.b_type)

    @overload
    def concat(self, other: 'UniConstraintStream[A]') -> 'BiConstraintStream[A, B]':
        ...

    @overload
    def concat(self, other: 'UniConstraintStream[A]', padding_b: Callable[[A], B]) -> 'BiConstraintStream[A, B]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B]') -> 'BiConstraintStream[A, B]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B, C_]') -> 'TriConstraintStream[A, B, C_]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B, C_]', padding_c: Callable[[A, B], C_]) \
            -> 'TriConstraintStream[A, B, C_]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B, C_, D_]') -> 'QuadConstraintStream[A, B, C_, D_]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B, C_, D_]', padding_c: Callable[[A, B], C_],
               padding_d: Callable[[A, B], D_]) -> 'QuadConstraintStream[A, B, C_, D_]':
        ...

    def concat(self, other, padding_b=None, padding_c=None, padding_d=None):
        """
        The concat building block allows you
        to create a constraint stream containing tuples of two other constraint streams.
        If join acts like a cartesian product of two lists, concat acts like a concatenation of two lists.
        Unlike union of sets, concatenation of lists repeats duplicated elements.
        If the two constraint concatenating streams share tuples, which happens e.g.
        when they come from the same source of data, the tuples will be repeated downstream.
        If this is undesired, use the distinct building block.
        """
        specified_count = sum(x is not None for x in [padding_b, padding_c, padding_d])
        if isinstance(other, UniConstraintStream):
            if specified_count == 0:
                return BiConstraintStream(self.delegate.concat(other.delegate), self.package,
                                          self.a_type, self.b_type)
            elif specified_count != 1:
                raise ValueError(f'Concatenating Bi and UniConstraintStream requires one padding function, '
                                 f'got {specified_count} instead.')
            elif padding_b is None:
                raise ValueError(f'Concatenating Bi and UniConstraintStream requires padding_b to be provided.')
            return BiConstraintStream(self.delegate.concat(other.delegate, padding_b), self.package,
                                      self.a_type, self.b_type)
        elif isinstance(other, BiConstraintStream):
            if specified_count == 0:
                return BiConstraintStream(self.delegate.concat(other.delegate), self.package,
                                          self.a_type, self.b_type)
            else:
                raise ValueError(f'Concatenating BiConstraintStreams requires no padding function, '
                                 f'got {specified_count} instead.')
        elif isinstance(other, TriConstraintStream):
            if specified_count == 0:
                return TriConstraintStream(self.delegate.concat(other.delegate), self.package,
                                           self.a_type, self.b_type, other.c_type)
            elif specified_count != 1:
                raise ValueError(f'Concatenating Bi and TriConstraintStream requires one padding function, '
                                 f'got {specified_count} instead.')
            elif padding_c is None:
                raise ValueError(f'Concatenating Bi and TriConstraintStream requires padding_c to be provided.')
            return TriConstraintStream(self.delegate.concat(other.delegate, padding_c), self.package,
                                       self.a_type, self.b_type, other.c_type)
        elif isinstance(other, QuadConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate), self.package,
                                            self.a_type, self.b_type, other.c_type, other.d_type)
            elif specified_count != 2:
                raise ValueError(f'Concatenating Bi and QuadConstraintStream requires two padding functions, '
                                 f'got {specified_count} instead.')
            elif padding_b is not None:
                raise ValueError(f'Concatenating Bi and QuadConstraintStream requires '
                                 f'padding_c and padding_d to be provided.')
            return QuadConstraintStream(self.delegate.concat(other.delegate, padding_c, padding_d), self.package,
                                        self.a_type, self.b_type, other.c_type, other.d_type)
        else:
            raise RuntimeError(f'Unhandled constraint stream type {type(other)}.')

    @overload
    def complement(self, cls: type[A]) -> 'BiConstraintStream[A, B]':
        ...

    @overload
    def complement(self, cls: type[A], padding: Callable[[A], B]) -> 'BiConstraintStream[A, B]':
        ...

    def complement(self, cls: type[A], padding=None):
        """
        Adds to the stream all instances of a given class which are not yet present in it.
        These instances must be present in the solution,
        which means the class needs to be either a planning entity or a problem fact.

        The instances will be read from the first element of the input tuple.
        When an output tuple needs to be created for the newly inserted instances,
        the first element will be the new instance.
        The rest of the tuple will be padded with the result of the padding function.

        Parameters
        ----------
        cls : Type[A]
            the type of the instances to add to the stream.

        padding : Callable[[A], B]
            a function that computes the padding value for the second fact in the new tuple.
        """
        if None is padding:
            result = self.delegate.complement(get_class(cls))
            return BiConstraintStream(result, self.package, self.a_type, self.b_type)
        java_padding = function_cast(padding, self.a_type)
        result = self.delegate.complement(get_class(cls), java_padding)
        return BiConstraintStream(result, self.package, self.a_type, self.b_type)

    def penalize(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B], int] = None) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        BiConstraintBuilder
            a `BiConstraintBuilder`
        """
        if match_weigher is None:
            return BiConstraintBuilder(self.delegate.penalize(constraint_weight), self.a_type, self.b_type)
        else:
            return BiConstraintBuilder(self.delegate.penalizeLong(constraint_weight,
                                                                  to_long_function_cast(match_weigher,
                                                                                        self.a_type,
                                                                                        self.b_type)),
                                       self.a_type, self.b_type)


    def penalize_decimal(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B], Decimal] = None) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        BiConstraintBuilder
            a `BiConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return BiConstraintBuilder(self.delegate.penalize(constraint_weight), self.a_type, self.b_type)
        else:
            return BiConstraintBuilder(self.delegate.penalizeBigDecimal(constraint_weight,
                                                                        function_cast(match_weigher,
                                                                                      self.a_type,
                                                                                      self.b_type,
                                                                                      return_type=BigDecimal)),
                                       self.a_type, self.b_type)

    def reward(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B], int] = None) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        BiConstraintBuilder
            a `BiConstraintBuilder`
        """
        if match_weigher is None:
            return BiConstraintBuilder(self.delegate.reward(constraint_weight), self.a_type, self.b_type)
        else:
            return BiConstraintBuilder(self.delegate.rewardLong(constraint_weight,
                                                                to_long_function_cast(match_weigher,
                                                                                      self.a_type,
                                                                                      self.b_type)),
                                       self.a_type, self.b_type)

    def reward_decimal(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B], Decimal] = None) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        BiConstraintBuilder
            a `BiConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return BiConstraintBuilder(self.delegate.reward(constraint_weight), self.a_type, self.b_type)
        else:
            return BiConstraintBuilder(self.delegate.rewardBigDecimal(constraint_weight,
                                                                      function_cast(match_weigher,
                                                                                    self.a_type,
                                                                                    self.b_type,
                                                                                    return_type=BigDecimal)),
                                       self.a_type, self.b_type)

    def impact(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B], int] = None) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        BiConstraintBuilder
            a `BiConstraintBuilder`
        """
        if match_weigher is None:
            return BiConstraintBuilder(self.delegate.impact(constraint_weight), self.a_type, self.b_type)
        else:
            return BiConstraintBuilder(self.delegate.impactLong(constraint_weight,
                                                                to_long_function_cast(match_weigher,
                                                                                      self.a_type,
                                                                                      self.b_type)),
                                       self.a_type, self.b_type)


    def impact_decimal(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B], Decimal] = None) -> \
            'BiConstraintBuilder[A, B, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        BiConstraintBuilder
            a `BiConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return BiConstraintBuilder(self.delegate.impact(constraint_weight), self.a_type, self.b_type)
        else:
            return BiConstraintBuilder(self.delegate.impactBigDecimal(constraint_weight,
                                                                      function_cast(match_weigher,
                                                                                    self.a_type,
                                                                                    self.b_type,
                                                                                    return_type=BigDecimal)),
                                       self.a_type, self.b_type)


class TriConstraintStream(Generic[A, B, C]):
    """
    A ConstraintStream that matches three facts.
    """
    delegate: '_JavaTriConstraintStream[A,B,C]'
    package: str
    a_type: Type[A]
    b_type: Type[B]
    c_type: Type[C]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: '_JavaTriConstraintStream[A,B,C]', package: str,
                 a_type: Type[A], b_type: Type[B],
                 c_type: Type[C]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type
        self.b_type = b_type
        self.c_type = c_type

    def get_constraint_factory(self):
        """
        The ConstraintFactory that build this.
        """
        return ConstraintFactory(self.delegate.getConstraintFactory())

    def filter(self, predicate: Callable[[A, B, C], bool]) -> 'TriConstraintStream[A,B,C]':
        """
        Exhaustively test each fact against the predicate and match if the predicate returns ``True``.
        """
        translated_predicate = predicate_cast(predicate, self.a_type, self.b_type, self.c_type)
        return TriConstraintStream(self.delegate.filter(translated_predicate), self.package,
                                   self.a_type,
                                   self.b_type, self.c_type)

    def join(self, unistream_or_type: Union[UniConstraintStream[D_], Type[D_]],
             *joiners: 'QuadJoiner[A, B, C, D_]') -> 'QuadConstraintStream[A,B,C,D_]':
        """
        Create a new `QuadConstraintStream` for every combination of A, B and C that satisfies all specified joiners.
        """
        d_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            d_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            d_type = get_class(unistream_or_type)
            unistream_or_type = d_type

        join_result = self.delegate.join(unistream_or_type, extract_joiners(joiners,
                                                                            self.a_type, self.b_type, self.c_type,
                                                                            d_type))
        return QuadConstraintStream(join_result, self.package,
                                    self.a_type, self.b_type, self.c_type, d_type)

    @overload
    def if_exists(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'TriConstraintStream[A,B,C]':
        ...

    @overload
    def if_exists(self, other_stream: 'UniConstraintStream[D_]', *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'TriConstraintStream[A,B,C]':
        ...

    def if_exists(self, unistream_or_type: Union['UniConstraintStream[D_]', Type[D_]],
                  *joiners: 'QuadJoiner[A, B, C, D_]') -> 'TriConstraintStream[A,B,C]':
        """
        Create a new `TriConstraintStream` for every A, B, C where D exists that satisfies all specified joiners.
        """
        d_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            d_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            d_type = get_class(unistream_or_type)
            unistream_or_type = d_type
        return TriConstraintStream(self.delegate.ifExists(unistream_or_type,
                                                          extract_joiners(joiners,
                                                                          self.a_type, self.b_type, self.c_type,
                                                                          d_type)),
                                   self.package, self.a_type, self.b_type, self.c_type)

    def if_exists_including_unassigned(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'TriConstraintStream[A,B,C]':
        """
        Create a new `TriConstraintStream` for every A, B where D exists that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return TriConstraintStream(self.delegate.ifExistsIncludingUnassigned(item_type,
                                                                             extract_joiners(joiners,
                                                                                             self.a_type, self.b_type,
                                                                                             self.c_type, item_type)),
                                   self.package, self.a_type, self.b_type, self.c_type)

    @overload
    def if_not_exists(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'TriConstraintStream[A,B,C]':
        ...

    @overload
    def if_not_exists(self, other_stream: 'UniConstraintStream[D_]', *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'TriConstraintStream[A,B,C]':
        ...

    def if_not_exists(self, unistream_or_type: Union['UniConstraintStream[D_]', Type[D_]],
                      *joiners: 'QuadJoiner[A, B, C, D_]') -> 'TriConstraintStream[A,B,C]':
        """
        Create a new `TriConstraintStream` for every A, B, C where D does not exist
        that satisfies all specified joiners.
        """
        d_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            d_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            d_type = get_class(unistream_or_type)
            unistream_or_type = d_type
        return TriConstraintStream(self.delegate.ifNotExists(unistream_or_type,
                                                             extract_joiners(joiners,
                                                                             self.a_type, self.b_type, self.c_type,
                                                                             d_type)),
                                   self.package, self.a_type, self.b_type, self.c_type)

    def if_not_exists_including_unassigned(self, item_type: Type[D_], *joiners: 'QuadJoiner[A, B, C, D_]') -> \
            'TriConstraintStream[A,B,C]':
        """
        Create a new `TriConstraintStream` for every A, B, C where D does not exist that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return TriConstraintStream(self.delegate.ifNotExistsIncludingUnassigned(item_type,
                                                                                extract_joiners(joiners,
                                                                                                self.a_type,
                                                                                                self.b_type,
                                                                                                self.c_type,
                                                                                                item_type)),
                                   self.package, self.a_type, self.b_type, self.c_type)

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'TriConstraintCollector[A, B, C, Any, A_]') -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_],
                 second_key_mapping: Callable[[A, B, C], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_],
                 collector: 'TriConstraintCollector[A, B, C, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'TriConstraintCollector[A, B, C, Any, A_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 third_key_mapping: Callable[[A, B, C], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 collector: 'TriConstraintCollector[A, B, C, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_],
                 first_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'TriConstraintCollector[A, B, C, Any, A_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 third_collector: 'TriConstraintCollector[A, B, C, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 third_key_mapping: Callable[[A, B, C], C_],
                 fourth_key_mapping: Callable[[A, B, C], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 third_key_mapping: Callable[[A, B, C], C_],
                 collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C], A_], second_key_mapping: Callable[[A, B, C], B_],
                 first_collector: 'TriConstraintCollector[A, B, C, Any, C_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> \
            'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C], A_],
                 first_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, C_]',
                 third_collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'TriConstraintCollector[A, B, C, Any, A_]',
                 second_collector: 'TriConstraintCollector[A, B, C, Any, B_]',
                 third_collector: 'TriConstraintCollector[A, B, C, Any, C_]',
                 fourth_collector: 'TriConstraintCollector[A, B, C, Any, D_]') -> \
            'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """
        Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Returns
        -------
        UniConstraintStream | BiConstraintStream | TriConstraintStream | QuadConstraintStream
            The type of stream returned depends on the number of arguments passed:

            - 1 -> UniConstraintStream

            - 2 -> BiConstraintStream

            - 3 -> TriConstraintStream

            - 4 -> QuadConstraintStream

        Examples
        --------

        Count the items in this stream; returns Uni[int]

        >>> group_by(ConstraintCollectors.count())

        Count the number of shifts each employee has; returns Bi[Employee]

        >>> group_by(lambda shift: shift.employee, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date, datetime.date]

        >>> group_by(lambda shift: shift.employee,
        ...          ConstraintCollectors.min(lambda shift: shift.date)
        ...          ConstraintCollectors.max(lambda shift: shift.date))
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type, self.b_type, self.c_type)

    @overload
    def map(self, mapping_function: Callable[[A, B, C], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B, C], A_],
            mapping_function2: Callable[[A, B, C], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B, C], A_], mapping_function2: Callable[[A, B, C], B_],
            mapping_function3: Callable[[A, B, C], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B, C], A_], mapping_function2: Callable[[A, B, C], B_],
            mapping_function3: Callable[[A, B, C], C_],
            mapping_function4: Callable[[A, B, C], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def map(self, *mapping_functions):
        """
        Transforms the stream in such a way that tuples are remapped using the given function.
        """
        if len(mapping_functions) == 0:
            raise ValueError(f'At least one mapping function is required for map.')
        if len(mapping_functions) > 4:
            raise ValueError(f'At most four mapping functions can be passed to map (got {len(mapping_functions)}).')
        translated_functions = tuple(map(lambda mapping_function: function_cast(mapping_function,
                                                                                self.a_type, self.b_type, self.c_type),
                                         mapping_functions))
        if len(mapping_functions) == 1:
            return UniConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 2:
            return BiConstraintStream(self.delegate.map(*translated_functions), self.package,

                                      JClass('java.lang.Object'), JClass('java.lang.Object'))
        if len(mapping_functions) == 3:
            return TriConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'), JClass('java.lang.Object'),
                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 4:
            return QuadConstraintStream(self.delegate.map(*translated_functions), self.package,

                                        JClass('java.lang.Object'), JClass('java.lang.Object'),
                                        JClass('java.lang.Object'), JClass('java.lang.Object'))
        raise RuntimeError(f'Impossible state: missing case for {len(mapping_functions)}.')

    def expand(self, mapping_function: Callable[[A, B, C], D_]) -> 'QuadConstraintStream[A, B, C, D_]':
        """
        Tuple expansion is a special case of tuple mapping
        which only increases stream cardinality and can not introduce duplicate tuples.
        It enables you to add extra facts to each tuple in a constraint stream by applying a mapping function to it.
        This is useful in situations where an expensive computations needs to be cached for use later in the stream.
        """
        translated_function = function_cast(mapping_function, self.a_type, self.b_type, self.c_type)
        return QuadConstraintStream(self.delegate.expand(translated_function), self.package,

                                    self.a_type, self.b_type, self.c_type, JClass('java.lang.Object'))

    def flatten_last(self, flattening_function: Callable[[C], C_]) -> 'TriConstraintStream[A,B,C_]':
        """
        Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.
        """
        translated_function = function_cast(flattening_function, self.c_type)
        return TriConstraintStream(self.delegate.flattenLast(translated_function), self.package,

                                   self.a_type, self.b_type, JClass('java.lang.Object'))

    def distinct(self) -> 'TriConstraintStream[A, B, C]':
        """
        Transforms the stream in such a way that all the tuples going through it are distinct.
        """
        return TriConstraintStream(self.delegate.distinct(), self.package,
                                   self.a_type,
                                   self.b_type, self.c_type)

    @overload
    def concat(self, other: 'UniConstraintStream[A]') -> 'TriConstraintStream[A, B, C]':
        ...

    @overload
    def concat(self, other: 'UniConstraintStream[A]', padding_b: Callable[[A], B], padding_c: Callable[[A], C]) \
            -> 'TriConstraintStream[A, B, C]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B]') -> 'TriConstraintStream[A, B, C]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B]', padding_c: Callable[[A, B], C]) \
            -> 'TriConstraintStream[A, B, C]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B, C]') -> 'TriConstraintStream[A, B, C]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B, C, D_]') -> 'QuadConstraintStream[A, B, C, D_]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B, C, D_]', padding_d: Callable[[A, B, C], D_]) \
            -> 'QuadConstraintStream[A, B, C, D_]':
        ...

    def concat(self, other, padding_b=None, padding_c=None, padding_d=None):
        """
        The concat building block allows you
        to create a constraint stream containing tuples of two other constraint streams.
        If join acts like a cartesian product of two lists, concat acts like a concatenation of two lists.
        Unlike union of sets, concatenation of lists repeats duplicated elements.
        If the two constraint concatenating streams share tuples, which happens e.g.
        when they come from the same source of data, the tuples will be repeated downstream.
        If this is undesired, use the distinct building block.
        """
        specified_count = sum(x is not None for x in [padding_b, padding_c, padding_d])
        if isinstance(other, UniConstraintStream):
            if specified_count == 0:
                return TriConstraintStream(self.delegate.concat(other.delegate), self.package,
                                           self.a_type, self.b_type, self.c_type)
            elif specified_count != 2:
                raise ValueError(f'Concatenating Tri and UniConstraintStream requires 2 padding functions, '
                                 f'got {specified_count} instead.')
            elif padding_d is not None:
                raise ValueError(f'Concatenating Tri and UniConstraintStream requires '
                                 f'padding_b and padding_c to be provided.')
            return TriConstraintStream(self.delegate.concat(other.delegate, padding_b, padding_c), self.package,
                                       self.a_type, self.b_type, self.c_type)
        elif isinstance(other, BiConstraintStream):
            if specified_count == 0:
                return TriConstraintStream(self.delegate.concat(other.delegate), self.package,
                                           self.a_type, self.b_type, self.c_type)
            elif specified_count != 1:
                raise ValueError(f'Concatenating Tri and BiConstraintStream requires 1 padding function, '
                                 f'got {specified_count} instead.')
            elif padding_c is None:
                raise ValueError(f'Concatenating Tri and BiConstraintStream requires padding_c to be provided.')
            return TriConstraintStream(self.delegate.concat(other.delegate, padding_c), self.package,
                                       self.a_type, self.b_type, self.c_type)
        elif isinstance(other, TriConstraintStream):
            if specified_count == 0:
                return TriConstraintStream(self.delegate.concat(other.delegate), self.package,
                                           self.a_type, self.b_type, self.c_type)
            else:
                raise ValueError(f'Concatenating TriConstraintStreams requires no padding functions, '
                                 f'got {specified_count} instead.')
        elif isinstance(other, QuadConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate), self.package,
                                            self.a_type, self.b_type, self.c_type, other.d_type)
            elif specified_count != 1:
                raise ValueError(f'Concatenating Tri and QuadConstraintStream requires 1 padding function, '
                                 f'got {specified_count} instead.')
            elif padding_d is None:
                raise ValueError(f'Concatenating Tri and QuadConstraintStream requires padding_d to be provided.')
            return QuadConstraintStream(self.delegate.concat(other.delegate, padding_d), self.package,
                                        self.a_type, self.b_type, self.c_type, other.d_type)
        else:
            raise RuntimeError(f'Unhandled constraint stream type {type(other)}.')

    @overload
    def complement(self, cls: type[A]) -> 'TriConstraintStream[A, B, C]':
        ...

    @overload
    def complement(self, cls: type[A], padding_b: Callable[[A], B], padding_c: Callable[[A], C]) \
            -> 'TriConstraintStream[A, B, C]':
        ...

    def complement(self, cls: type[A], padding_b=None, padding_c=None):
        """
        Adds to the stream all instances of a given class which are not yet present in it.
        These instances must be present in the solution,
        which means the class needs to be either a planning entity or a problem fact.

        The instances will be read from the first element of the input tuple.
        When an output tuple needs to be created for the newly inserted instances,
        the first element will be the new instance.
        The rest of the tuple will be padded with the result of the padding function,
        applied on the new instance.

        Padding functions are optional, but if one is provided, then both must-be provided.

        Parameters
        ----------
        cls : Type[A]
            the type of the instances to add to the stream.

        padding_b : Callable[[A], B]
            a function that computes the padding value for the second fact in the new tuple.

        padding_c : Callable[[A], C]
            a function that computes the padding value for the third fact in the new tuple.
        """
        if None == padding_b == padding_c:
            result = self.delegate.complement(get_class(cls))
            return TriConstraintStream(result, self.package, self.a_type, self.b_type, self.c_type)
        specified_count = sum(x is not None for x in [padding_b, padding_c])
        if specified_count != 0:
            raise ValueError(f'If a padding function is provided, both are expected, got {specified_count} instead.')
        java_padding_b = function_cast(padding_b, self.a_type)
        java_padding_c = function_cast(padding_c, self.a_type)
        result = self.delegate.complement(get_class(cls), java_padding_b, java_padding_c)
        return TriConstraintStream(result, self.package, self.a_type, self.b_type, self.c_type)

    def penalize(self, constraint_weight: ScoreType,
                 match_weigher: Callable[[A, B, C], int] = None) -> 'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        TriConstraintBuilder
            a `TriConstraintBuilder`
        """
        if match_weigher is None:
            return TriConstraintBuilder(self.delegate.penalize(constraint_weight),
                                        self.a_type, self.b_type, self.c_type)
        else:
            return TriConstraintBuilder(self.delegate.penalizeLong(constraint_weight,
                                                                   to_long_function_cast(match_weigher,
                                                                                         self.a_type,
                                                                                         self.b_type,
                                                                                         self.c_type)),
                                        self.a_type, self.b_type, self.c_type)

    def penalize_decimal(self, constraint_weight: ScoreType,
                         match_weigher: Callable[[A, B, C], Decimal] = None) -> 'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        TriConstraintBuilder
            a `TriConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return TriConstraintBuilder(self.delegate.penalize(constraint_weight),
                                        self.a_type, self.b_type, self.c_type)
        else:
            return TriConstraintBuilder(self.delegate.penalizeBigDecimal(constraint_weight,
                                                                         function_cast(match_weigher,
                                                                                       self.a_type,
                                                                                       self.b_type,
                                                                                       self.c_type,
                                                                                       return_type=BigDecimal)),
                                        self.a_type, self.b_type, self.c_type)

    def reward(self, constraint_weight: ScoreType, match_weigher: Callable[[A, B, C], int] = None) -> \
            'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        TriConstraintBuilder
            a `TriConstraintBuilder`
        """
        if match_weigher is None:
            return TriConstraintBuilder(self.delegate.reward(constraint_weight), self.a_type, self.b_type,
                                        self.c_type)
        else:
            return TriConstraintBuilder(self.delegate.rewardLong(constraint_weight,
                                                                 to_long_function_cast(match_weigher,
                                                                                       self.a_type,
                                                                                       self.b_type,
                                                                                       self.c_type)),
                                        self.a_type, self.b_type, self.c_type)

    def reward_decimal(self, constraint_weight: ScoreType,
                         match_weigher: Callable[[A, B, C], Decimal] = None) -> 'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        TriConstraintBuilder
            a `TriConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return TriConstraintBuilder(self.delegate.reward(constraint_weight),
                                        self.a_type, self.b_type, self.c_type)
        else:
            return TriConstraintBuilder(self.delegate.rewardBigDecimal(constraint_weight,
                                                                       function_cast(match_weigher,
                                                                                     self.a_type,
                                                                                     self.b_type,
                                                                                     self.c_type,
                                                                                     return_type=BigDecimal)),
                                        self.a_type, self.b_type, self.c_type)

    def impact(self, constraint_weight: ScoreType,
               match_weigher: Callable[[A, B, C], int] = None) -> 'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        TriConstraintBuilder
            a `TriConstraintBuilder`
        """
        if match_weigher is None:
            return TriConstraintBuilder(self.delegate.impact(constraint_weight),
                                        self.a_type, self.b_type, self.c_type)
        else:
            return TriConstraintBuilder(self.delegate.impactLong(constraint_weight,
                                                                 to_long_function_cast(match_weigher,
                                                                                       self.a_type,
                                                                                       self.b_type,
                                                                                       self.c_type)),
                                        self.a_type, self.b_type, self.c_type)

    def impact_decimal(self, constraint_weight: ScoreType,
                       match_weigher: Callable[[A, B, C], Decimal] = None) -> 'TriConstraintBuilder[A, B, C, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        TriConstraintBuilder
            a `TriConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return TriConstraintBuilder(self.delegate.impact(constraint_weight),
                                        self.a_type, self.b_type, self.c_type)
        else:
            return TriConstraintBuilder(self.delegate.impactBigDecimal(constraint_weight,
                                                                       function_cast(match_weigher,
                                                                                     self.a_type,
                                                                                     self.b_type,
                                                                                     self.c_type,
                                                                                     return_type=BigDecimal)),
                                        self.a_type, self.b_type, self.c_type)


class QuadConstraintStream(Generic[A, B, C, D]):
    """
    A ConstraintStream that matches four facts.
    """
    delegate: '_JavaQuadConstraintStream[A,B,C,D]'
    package: str
    a_type: Type[A]
    b_type: Type[B]
    c_type: Type[C]
    d_type: Type[D]
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    def __init__(self, delegate: '_JavaQuadConstraintStream[A,B,C,D]', package: str,
                 a_type: Type[A], b_type: Type[B],
                 c_type: Type[C], d_type: Type[D]):
        self.delegate = delegate
        self.package = package
        self.a_type = a_type
        self.b_type = b_type
        self.c_type = c_type
        self.d_type = d_type

    def get_constraint_factory(self):
        """
        The ConstraintFactory that build this.
        """
        return ConstraintFactory(self.delegate.getConstraintFactory())

    def filter(self, predicate: Callable[[A, B, C, D], bool]) -> 'QuadConstraintStream[A,B,C,D]':
        """
        Exhaustively test each fact against the predicate and match if the predicate returns ``True``.
        """
        translated_predicate = predicate_cast(predicate, self.a_type, self.b_type, self.c_type, self.d_type)
        return QuadConstraintStream(self.delegate.filter(translated_predicate), self.package,
                                    self.a_type,
                                    self.b_type, self.c_type, self.d_type)

    @overload
    def if_exists(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'QuadConstraintStream[A,B,C,D]':
        ...

    @overload
    def if_exists(self, other_stream: 'UniConstraintCollector[E_]', *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'QuadConstraintStream[A,B,C,D]':
        ...

    def if_exists(self, unistream_or_type: Union['UniConstraintStream[E_]', Type[E_]],
                  *joiners: 'PentaJoiner[A, B, C, D, E_]') -> 'QuadConstraintStream[A,B,C,D]':
        """
        Create a new `QuadConstraintStream` for every A, B, C, D where E exists that satisfies all specified joiners.
        """
        e_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            e_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            e_type = get_class(unistream_or_type)
            unistream_or_type = e_type
        return QuadConstraintStream(self.delegate.ifExists(unistream_or_type,
                                                           extract_joiners(joiners,
                                                                           self.a_type, self.b_type, self.c_type,
                                                                           self.d_type, e_type)),
                                    self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    def if_exists_including_unassigned(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'QuadConstraintStream[A,B,C,D]':
        """
        Create a new `QuadConstraintStream` for every A, B, C, D where E exists that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return QuadConstraintStream(self.delegate.ifExistsIncludingUnassigned(item_type,
                                                                              extract_joiners(joiners,
                                                                                              self.a_type,
                                                                                              self.b_type,
                                                                                              self.c_type,
                                                                                              self.d_type,
                                                                                              item_type)),
                                    self.package,
                                    self.a_type, self.b_type, self.c_type, self.d_type)

    @overload
    def if_not_exists(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'QuadConstraintStream[A,B,C,D]':
        ...

    @overload
    def if_not_exists(self, other_stream: 'UniConstraintCollector[E_]', *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'QuadConstraintStream[A,B,C,D]':
        ...

    def if_not_exists(self, unistream_or_type: Union['UniConstraintStream[E_]', Type[E_]],
                      *joiners: 'PentaJoiner[A, B, C, D, E_]') -> 'QuadConstraintStream[A,B,C,D]':
        """
        Create a new `QuadConstraintStream` for every A, B, C, D where E does not exist
        that satisfies all specified joiners.
        """
        e_type = None
        if isinstance(unistream_or_type, UniConstraintStream):
            e_type = unistream_or_type.a_type
            unistream_or_type = unistream_or_type.delegate
        else:
            e_type = get_class(unistream_or_type)
            unistream_or_type = e_type
        return QuadConstraintStream(self.delegate.ifNotExists(unistream_or_type,
                                                              extract_joiners(joiners,
                                                                              self.a_type, self.b_type, self.c_type,
                                                                              self.d_type, e_type)),
                                    self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    def if_not_exists_including_unassigned(self, item_type: Type[E_], *joiners: 'PentaJoiner[A, B, C, D, E_]') -> \
            'QuadConstraintStream[A,B,C,D]':
        """
        Create a new `QuadConstraintStream` for every A, B, C,
        D where E does not exist that satisfies all specified joiners.
        """
        item_type = get_class(item_type)
        return QuadConstraintStream(self.delegate.ifNotExistsIncludingUnassigned(item_type,
                                                                                 extract_joiners(joiners,
                                                                                                 self.a_type,
                                                                                                 self.b_type,
                                                                                                 self.c_type,
                                                                                                 self.d_type,
                                                                                                 item_type)),
                                    self.package,
                                    self.a_type, self.b_type, self.c_type, self.d_type)

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]') -> 'UniConstraintStream[A_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_],
                 second_key_mapping: Callable[[A, B, C, D], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_],
                 collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]') -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 third_key_mapping: Callable[[A, B, C, D], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_],
                 first_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 third_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]') -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 third_key_mapping: Callable[[A, B, C, D], C_],
                 fourth_key_mapping: Callable[[A, B, C, D], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 third_key_mapping: Callable[[A, B, C, D], C_],
                 collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_key_mapping: Callable[[A, B, C, D], A_], second_key_mapping: Callable[[A, B, C, D], B_],
                 first_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> \
            'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, key_mapping: Callable[[A, B, C, D], A_],
                 first_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                 third_collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> \
            'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    @overload
    def group_by(self, first_collector: 'QuadConstraintCollector[A, B, C, D, Any, A_]',
                 second_collector: 'QuadConstraintCollector[A, B, C, D, Any, B_]',
                 third_collector: 'QuadConstraintCollector[A, B, C, D, Any, C_]',
                 fourth_collector: 'QuadConstraintCollector[A, B, C, D, Any, D_]') -> \
            'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def group_by(self, *args):
        """
        Collect items into groups using the group_key_function(s) and optionally aggregate the group's items into a
        result.

        The syntax of group_by is zero to four group_key functions, followed by zero to four collectors. At most
        four arguments can be passed to group_by.

        If no group_key function is passed to group_by, all items in the stream are aggregated into a single result
        by the passed constraint collectors.

        Returns
        -------
        UniConstraintStream | BiConstraintStream | TriConstraintStream | QuadConstraintStream
            The type of stream returned depends on the number of arguments passed:

            - 1 -> UniConstraintStream

            - 2 -> BiConstraintStream

            - 3 -> TriConstraintStream

            - 4 -> QuadConstraintStream

        Examples
        --------

        Count the items in this stream; returns Uni[int]

        >>> group_by(ConstraintCollectors.count())

        Count the number of shifts each employee has; returns Bi[Employee]

        >>> group_by(lambda shift: shift.employee, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Count the number of shifts each employee has on a date; returns Tri[Employee, datetime.date, int]

        >>> group_by(lambda shift: shift.employee, lambda shift: shift.date, ConstraintCollectors.count())

        Get the dates of the first and last shift of each employee; returns Tri[Employee, datetime.date, datetime.date]

        >>> group_by(lambda shift: shift.employee,
        ...          ConstraintCollectors.min(lambda shift: shift.date)
        ...          ConstraintCollectors.max(lambda shift: shift.date))
        """
        return perform_group_by(self.delegate, self.package, args, self.a_type, self.b_type, self.c_type, self.d_type)

    @overload
    def map(self, mapping_function: Callable[[A, B, C, D], A_]) -> 'UniConstraintStream[A_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B, C, D], A_],
            mapping_function2: Callable[[A, B, C, D], B_]) -> 'BiConstraintStream[A_, B_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B, C, D], A_], mapping_function2: Callable[[A, B, C, D], B_],
            mapping_function3: Callable[[A, B, C, D], C_]) -> 'TriConstraintStream[A_, B_, C_]':
        ...

    @overload
    def map(self, mapping_function: Callable[[A, B, C, D], A_], mapping_function2: Callable[[A, B, C, D], B_],
            mapping_function3: Callable[[A, B, C, D], C_],
            mapping_function4: Callable[[A, B, C, D], D_]) -> 'QuadConstraintStream[A_, B_, C_, D_]':
        ...

    def map(self, *mapping_functions):
        """
        Transforms the stream in such a way that tuples are remapped using the given function.
        """
        if len(mapping_functions) == 0:
            raise ValueError(f'At least one mapping function is required for map.')
        if len(mapping_functions) > 4:
            raise ValueError(f'At most four mapping functions can be passed to map (got {len(mapping_functions)}).')
        translated_functions = tuple(map(lambda mapping_function: function_cast(mapping_function, self.a_type,
                                                                                self.b_type, self.c_type, self.d_type),
                                         mapping_functions))
        if len(mapping_functions) == 1:
            return UniConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 2:
            return BiConstraintStream(self.delegate.map(*translated_functions), self.package,

                                      JClass('java.lang.Object'), JClass('java.lang.Object'))
        if len(mapping_functions) == 3:
            return TriConstraintStream(self.delegate.map(*translated_functions), self.package,

                                       JClass('java.lang.Object'), JClass('java.lang.Object'),
                                       JClass('java.lang.Object'))
        if len(mapping_functions) == 4:
            return QuadConstraintStream(self.delegate.map(*translated_functions), self.package,
                                        JClass('java.lang.Object'), JClass('java.lang.Object'),
                                        JClass('java.lang.Object'), JClass('java.lang.Object'))
        raise RuntimeError(f'Impossible state: missing case for {len(mapping_functions)}.')

    def flatten_last(self, flattening_function) -> 'QuadConstraintStream[A,B,C,D]':
        """
        Takes each tuple and applies a mapping on it, which turns the tuple into an Iterable.
        """
        translated_function = function_cast(flattening_function, self.d_type)
        return QuadConstraintStream(self.delegate.flattenLast(translated_function), self.package,
                                    self.a_type, self.b_type, self.c_type, JClass('java.lang.Object'))

    def distinct(self) -> 'QuadConstraintStream[A,B,C,D]':
        """
        Transforms the stream in such a way that all the tuples going through it are distinct.
        """
        return QuadConstraintStream(self.delegate.distinct(), self.package,
                                    self.a_type,
                                    self.b_type, self.c_type, self.d_type)

    @overload
    def concat(self, other: 'UniConstraintStream[A]') -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'UniConstraintStream[A]', padding_b: Callable[[A], B], padding_c: Callable[[A], C],
               padding_d: Callable[[A], D]) -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B]') -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B]', padding_c: Callable[[A, B], C],
               padding_d: Callable[[A, B], D]) -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'BiConstraintStream[A, B]') -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B, C]') -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'TriConstraintStream[A, B, C]', padding_d: Callable[[A, B, C], D]) \
            -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def concat(self, other: 'QuadConstraintStream[A, B, C, D]') -> 'QuadConstraintStream[A, B, C, D]':
        ...

    def concat(self, other, padding_b=None, padding_c=None, padding_d=None):
        """
        The concat building block allows you
        to create a constraint stream containing tuples of two other constraint streams.
        If join acts like a cartesian product of two lists, concat acts like a concatenation of two lists.
        Unlike union of sets, concatenation of lists repeats duplicated elements.
        If the two constraint concatenating streams share tuples, which happens e.g.
        when they come from the same source of data, the tuples will be repeated downstream.
        If this is undesired, use the distinct building block.
        """
        specified_count = sum(x is not None for x in [padding_b, padding_c, padding_d])
        if isinstance(other, UniConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate), self.package,
                                            self.a_type, self.b_type, self.c_type, self.d_type)
            elif specified_count != 3:
                raise ValueError(f'Concatenating Uni and QuadConstraintStream requires 3 padding functions, '
                                 f'got {specified_count} instead.')
            return QuadConstraintStream(self.delegate.concat(other.delegate, padding_b, padding_c, padding_d),
                                        self.package,
                                        self.a_type, self.b_type, self.c_type, self.d_type)
        elif isinstance(other, BiConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate), self.package,
                                            self.a_type, self.b_type, self.c_type, self.d_type)
            elif specified_count != 2:
                raise ValueError(f'Concatenating Bi and QuadConstraintStream requires 2 padding functions, '
                                 f'got {specified_count} instead.')
            elif padding_b is not None:
                raise ValueError(f'Concatenating Bi and QuadConstraintStream requires '
                                 f'padding_c and padding_d to be provided.')
            return QuadConstraintStream(self.delegate.concat(other.delegate, padding_c, padding_d), self.package,
                                        self.a_type, self.b_type, self.c_type, self.d_type)
        elif isinstance(other, TriConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate), self.package,
                                            self.a_type, self.b_type, self.c_type, self.d_type)
            elif specified_count != 1:
                raise ValueError(f'Concatenating Tri and QuadConstraintStream requires 1 padding function, '
                                 f'got {specified_count} instead.')
            elif padding_d is None:
                raise ValueError(f'Concatenating Bi and QuadConstraintStream requires padding_d to be provided.')
            return QuadConstraintStream(self.delegate.concat(other.delegate, padding_d), self.package,
                                        self.a_type, self.b_type, self.c_type, self.d_type)
        elif isinstance(other, QuadConstraintStream):
            if specified_count == 0:
                return QuadConstraintStream(self.delegate.concat(other.delegate), self.package,
                                            self.a_type, self.b_type, self.c_type, self.d_type)
            else:
                raise ValueError(f'Concatenating QuadConstraintStreams requires no padding functions, '
                                 f'got {specified_count} instead.')
        else:
            raise RuntimeError(f'Unhandled constraint stream type {type(other)}.')

    @overload
    def complement(self, cls: type[A]) -> 'QuadConstraintStream[A, B, C, D]':
        ...

    @overload
    def complement(self, cls: type[A], padding_b: Callable[[A], B], padding_c: Callable[[A], C],
                   padding_d: Callable[[A], D]) -> 'QuadConstraintStream[A, B, C, D]':
        ...

    def complement(self, cls: type[A], padding_b=None, padding_c=None, padding_d=None):
        """
        Adds to the stream all instances of a given class which are not yet present in it.
        These instances must be present in the solution,
        which means the class needs to be either a planning entity or a problem fact.

        The instances will be read from the first element of the input tuple.
        When an output tuple needs to be created for the newly inserted instances,
        the first element will be the new instance.
        The rest of the tuple will be padded with the result of the padding function,
        applied on the new instance.

        Padding functions are optional, but if one is provided, then all three must-be provided.

        Parameters
        ----------
        cls : Type[A]
            the type of the instances to add to the stream.

        padding_b : Callable[[A], B]
            a function that computes the padding value for the second fact in the new tuple.

        padding_c : Callable[[A], C]
            a function that computes the padding value for the third fact in the new tuple.

        padding_d : Callable[[A], D]
            a function that computes the padding value for the fourth fact in the new tuple.
        """
        if None == padding_b == padding_c == padding_d:
            result = self.delegate.complement(get_class(cls))
            return QuadConstraintStream(result, self.package, self.a_type, self.b_type, self.c_type, self.d_type)
        specified_count = sum(x is not None for x in [padding_b, padding_c, padding_d])
        if specified_count != 0:
            raise ValueError(f'If a padding function is provided, all 3 are expected, got {specified_count} instead.')
        java_padding_b = function_cast(padding_b, self.a_type)
        java_padding_c = function_cast(padding_c, self.a_type)
        java_padding_d = function_cast(padding_d, self.a_type)
        result = self.delegate.complement(get_class(cls), java_padding_b, java_padding_c, java_padding_d)
        return QuadConstraintStream(result, self.package, self.a_type, self.b_type, self.c_type, self.d_type)

    def penalize(self, constraint_weight: ScoreType,
                 match_weigher: Callable[[A, B, C, D], int] = None) -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C, D], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        QuadConstraintBuilder
            a `QuadConstraintBuilder`
        """
        if match_weigher is None:
            return QuadConstraintBuilder(self.delegate.penalize(constraint_weight),
                                         self.a_type, self.b_type, self.c_type, self.d_type)
        else:
            return QuadConstraintBuilder(self.delegate.penalizeLong(constraint_weight,
                                                                    to_long_function_cast(match_weigher,
                                                                                          self.a_type,
                                                                                          self.b_type,
                                                                                          self.c_type,
                                                                                          self.d_type)),
                                         self.a_type, self.b_type, self.c_type, self.d_type)

    def penalize_decimal(self, constraint_weight: ScoreType,
                 match_weigher: Callable[[A, B, C, D], Decimal] = None) -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Applies a negative Score impact, subtracting the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C, D], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        QuadConstraintBuilder
            a `QuadConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return QuadConstraintBuilder(self.delegate.penalize(constraint_weight),
                                         self.a_type, self.b_type, self.c_type, self.d_type)
        else:
            return QuadConstraintBuilder(self.delegate.penalizeBigDecimal(constraint_weight,
                                                                          function_cast(match_weigher,
                                                                                          self.a_type,
                                                                                          self.b_type,
                                                                                          self.c_type,
                                                                                          self.d_type,
                                                                                          return_type=BigDecimal)),
                                         self.a_type, self.b_type, self.c_type, self.d_type)

    def reward(self, constraint_weight: ScoreType,
               match_weigher: Callable[[A, B, C, D], int] = None) -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C, D], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        QuadConstraintBuilder
            a `QuadConstraintBuilder`
        """
        if match_weigher is None:
            return QuadConstraintBuilder(self.delegate.reward(constraint_weight),
                                         self.a_type, self.b_type, self.c_type, self.d_type)
        else:
            return QuadConstraintBuilder(self.delegate.rewardLong(constraint_weight,
                                                                  to_long_function_cast(match_weigher,
                                                                                        self.a_type,
                                                                                        self.b_type,
                                                                                        self.c_type,
                                                                                        self.d_type)),
                                         self.a_type, self.b_type, self.c_type, self.d_type)

    def reward_decimal(self, constraint_weight: ScoreType,
                       match_weigher: Callable[[A, B, C, D], Decimal] = None) -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Applies a positive Score impact, adding the constraint_weight multiplied by the match weight,
        and returns a builder to apply optional constraint properties.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C, D], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        QuadConstraintBuilder
            a `QuadConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return QuadConstraintBuilder(self.delegate.reward(constraint_weight),
                                         self.a_type, self.b_type, self.c_type, self.d_type)
        else:
            return QuadConstraintBuilder(self.delegate.rewardBigDecimal(constraint_weight,
                                                                        function_cast(match_weigher,
                                                                                      self.a_type,
                                                                                      self.b_type,
                                                                                      self.c_type,
                                                                                      self.d_type,
                                                                                      return_type=BigDecimal)),
                                         self.a_type, self.b_type, self.c_type, self.d_type)

    def impact(self, constraint_weight: ScoreType,
               match_weigher: Callable[[A, B, C, D], int] = None) -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C, D], int]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        QuadConstraintBuilder
            a `QuadConstraintBuilder`
        """
        if match_weigher is None:
            return QuadConstraintBuilder(self.delegate.impact(constraint_weight),
                                         self.a_type, self.b_type, self.c_type, self.d_type)
        else:
            return QuadConstraintBuilder(self.delegate.impactLong(constraint_weight,
                                                                  to_long_function_cast(match_weigher,
                                                                                        self.a_type,
                                                                                        self.b_type,
                                                                                        self.c_type,
                                                                                        self.d_type)),
                                         self.a_type, self.b_type, self.c_type, self.d_type)

    def impact_decimal(self, constraint_weight: ScoreType,
                       match_weigher: Callable[[A, B, C, D], Decimal] = None) -> 'QuadConstraintBuilder[A, B, C, D, ScoreType]':
        """
        Positively or negatively impacts the `Score` by `constraint_weight` multiplied by match weight for each match
        and returns a builder to apply optional constraint properties.
        Use `penalize` or `reward` instead, unless this constraint can both have positive and negative weights.

        Parameters
        ----------
        constraint_weight : Score
            the weight of the constraint.

        match_weigher : Callable[[A, B, C, D], Decimal]
            a function that computes the weight of a match.
            If absent, each match has weight ``1``.

        Returns
        -------
        QuadConstraintBuilder
            a `QuadConstraintBuilder`
        """
        from java.math import BigDecimal
        if match_weigher is None:
            return QuadConstraintBuilder(self.delegate.impact(constraint_weight),
                                         self.a_type, self.b_type, self.c_type, self.d_type)
        else:
            return QuadConstraintBuilder(self.delegate.impactBigDecimal(constraint_weight,
                                                                        function_cast(match_weigher,
                                                                                      self.a_type,
                                                                                      self.b_type,
                                                                                      self.c_type,
                                                                                      self.d_type,
                                                                                      return_type=BigDecimal)),
                                         self.a_type, self.b_type, self.c_type, self.d_type)


# Must be on the bottom, .group_by depends on this module
from ._constraint_factory import *
from ._joiners import *
from ._group_by import *
from ._constraint_builder import *
from ._function_translator import *

__all__ = [
    'UniConstraintStream',
    'BiConstraintStream',
    'TriConstraintStream',
    'QuadConstraintStream'
]
