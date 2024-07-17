from ._function_translator import *
import dataclasses
import jpype.imports  # noqa
from typing import TYPE_CHECKING, Callable, TypeVar, overload

if TYPE_CHECKING:
    from ai.timefold.solver.core.api.score.stream.bi import BiJoiner
    from ai.timefold.solver.core.api.score.stream.tri import TriJoiner
    from ai.timefold.solver.core.api.score.stream.quad import QuadJoiner
    from ai.timefold.solver.core.api.score.stream.penta import PentaJoiner


@dataclasses.dataclass
class SamePropertyUniJoiner:
    joiner_creator: Callable
    join_function: Callable


@dataclasses.dataclass
class PropertyJoiner:
    joiner_creator: Callable
    left_join_function: Callable
    right_join_function: Callable


@dataclasses.dataclass
class SameOverlappingPropertyUniJoiner:
    joiner_creator: Callable
    start_function: Callable
    end_function: Callable


@dataclasses.dataclass
class OverlappingPropertyJoiner:
    joiner_creator: Callable
    left_start_function: Callable
    left_end_function: Callable
    right_start_function: Callable
    right_end_function: Callable


@dataclasses.dataclass
class FilteringJoiner:
    joiner_creator: Callable
    filter_function: Callable


def extract_joiners(joiner_tuple, *stream_types):
    from ai.timefold.solver.core.api.score.stream.bi import BiJoiner
    from ai.timefold.solver.core.api.score.stream.tri import TriJoiner
    from ai.timefold.solver.core.api.score.stream.quad import QuadJoiner
    from ai.timefold.solver.core.api.score.stream.penta import PentaJoiner

    if len(joiner_tuple) == 1 and (isinstance(joiner_tuple[0], list) or isinstance(joiner_tuple[0], tuple)):
        joiner_tuple = joiner_tuple[0]  # Joiners was passed as a list of Joiners instead of varargs
    array_size = len(joiner_tuple)
    output_array = None
    array_type = None
    if len(stream_types) == 2:
        array_type = BiJoiner
        output_array = BiJoiner[array_size]
    elif len(stream_types) == 3:
        array_type = TriJoiner
        output_array = TriJoiner[array_size]
    elif len(stream_types) == 4:
        array_type = QuadJoiner
        output_array = QuadJoiner[array_size]
    elif len(stream_types) == 5:
        array_type = PentaJoiner
        output_array = PentaJoiner[array_size]
    else:
        raise ValueError

    for i in range(array_size):
        joiner_info = joiner_tuple[i]
        created_joiner = None
        if isinstance(joiner_info, SamePropertyUniJoiner):
            property_function = function_cast(joiner_info.join_function, stream_types[0])
            created_joiner = joiner_info.joiner_creator(property_function)
        elif isinstance(joiner_info, PropertyJoiner):
            left_property_function = function_cast(joiner_info.left_join_function, *stream_types[:-1])
            right_property_function = function_cast(joiner_info.right_join_function, stream_types[-1])
            created_joiner = joiner_info.joiner_creator(left_property_function, right_property_function)
        elif isinstance(joiner_info, SameOverlappingPropertyUniJoiner):
            start_function = function_cast(joiner_info.start_function, stream_types[0])
            end_function = function_cast(joiner_info.end_function, stream_types[0])
            created_joiner = joiner_info.joiner_creator(start_function, end_function)
        elif isinstance(joiner_info, OverlappingPropertyJoiner):
            left_start_function = function_cast(joiner_info.left_start_function, *stream_types[:-1])
            left_end_function = function_cast(joiner_info.left_end_function, *stream_types[:-1])
            right_start_function = function_cast(joiner_info.right_start_function, stream_types[-1])
            right_end_function = function_cast(joiner_info.right_end_function, stream_types[-1])
            created_joiner = joiner_info.joiner_creator(left_start_function, left_end_function,
                                                        right_start_function, right_end_function)
        elif isinstance(joiner_info, FilteringJoiner):
            filter_function = predicate_cast(joiner_info.filter_function, *stream_types)
            created_joiner = joiner_info.joiner_creator(filter_function)
        else:
            raise ValueError(f'Invalid Joiner: {joiner_info}. Create Joiners via timefold.solver.constraint.Joiners.')

        output_array[i] = array_type @ created_joiner

    return output_array


class Joiners:
    #  Method parameter type variables
    A = TypeVar('A')
    B = TypeVar('B')
    C = TypeVar('C')
    D = TypeVar('D')
    E = TypeVar('E')

    #  Method return type variables
    A_ = TypeVar('A_')
    B_ = TypeVar('B_')
    C_ = TypeVar('C_')
    D_ = TypeVar('D_')
    E_ = TypeVar('E_')

    @staticmethod
    def _delegate():
        from .._timefold_java_interop import ensure_init
        ensure_init()
        from ai.timefold.solver.core.api.score.stream import Joiners
        return Joiners

    @staticmethod
    def _call_comparison_java_joiner(java_joiner, mapping_or_left_mapping, right_mapping):
        if mapping_or_left_mapping is None and right_mapping is None:
            raise ValueError
        elif mapping_or_left_mapping is not None and right_mapping is None:
            return SamePropertyUniJoiner(java_joiner, mapping_or_left_mapping)
        elif mapping_or_left_mapping is not None and right_mapping is not None:
            return PropertyJoiner(java_joiner, mapping_or_left_mapping, right_mapping)
        else:
            raise ValueError

    @overload  # noqa
    @staticmethod
    def equal() -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def equal(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) -> 'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def equal(mapping_or_left_mapping=None, right_mapping=None):
        """
        Joins every A and B that share a property.
        """
        if mapping_or_left_mapping is None and right_mapping is None:
            return SamePropertyUniJoiner(Joiners._delegate().equal, lambda a: a)
        return Joiners._call_comparison_java_joiner(Joiners._delegate().equal, mapping_or_left_mapping, right_mapping)

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B], bool]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B, C], bool]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B, C, D], bool]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def filtering(predicate: Callable[[A, B, C, D, E], bool]) -> 'QuadJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def filtering(predicate):
        """
        Applies a filter to the joined tuple.
        """
        return FilteringJoiner(Joiners._delegate().filtering, predicate)

    @overload  # noqa
    @staticmethod
    def greater_than(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) -> \
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def greater_than(mapping_or_left_mapping, right_mapping=None):
        """
        Joins every A and B where a value of property on A is greater than the value of a property on B.
        """
        return Joiners._call_comparison_java_joiner(Joiners._delegate().greaterThan, mapping_or_left_mapping,
                                                    right_mapping)

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> \
            'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> \
            'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def greater_than_or_equal(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) -> \
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def greater_than_or_equal(mapping_or_left_mapping, right_mapping=None):
        """
        Joins every A and B where a value of property on A is greater than or equal to the value of a property on B.
        """
        return Joiners._call_comparison_java_joiner(Joiners._delegate().greaterThanOrEqual, mapping_or_left_mapping,
                                                    right_mapping)

    @overload  # noqa
    @staticmethod
    def less_than(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> 'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) -> \
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def less_than(mapping_or_left_mapping, right_mapping=None):
        """
        Joins every A and B where a value of property on A is less than the value of a property on B.
        """
        return Joiners._call_comparison_java_joiner(Joiners._delegate().lessThan, mapping_or_left_mapping,
                                                    right_mapping)

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(property_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A], A_], right_mapping: Callable[[B], B_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A, B], A_], right_mapping: Callable[[C], B_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A, B, C], A_], right_mapping: Callable[[D], B_]) -> \
            'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def less_than_or_equal(left_mapping: Callable[[A, B, C, D], A_], right_mapping: Callable[[E], B_]) -> \
            'PentaJoiner[A,B,C,D,E]':
        ...

    @staticmethod
    def less_than_or_equal(mapping_or_left_mapping, right_mapping=None):
        """
        Joins every A and B where a value of property on A is less than or equal to the value of a property on B.
        """
        return Joiners._call_comparison_java_joiner(Joiners._delegate().lessThanOrEqual, mapping_or_left_mapping,
                                                    right_mapping)

    @overload  # noqa
    @staticmethod
    def overlapping(start_mapping: Callable[[A], A_], end_mapping: Callable[[A], A_]) -> 'BiJoiner[A,A]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A], A_], left_end_mapping: Callable[[A], A_],
                    right_start_mapping: Callable[[B], A_], right_end_mapping: Callable[[B], A_]) -> 'BiJoiner[A,B]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A, B], A_], left_end_mapping: Callable[[A, B], A_],
                    right_start_mapping: Callable[[C], A_], right_end_mapping: Callable[[C], A_]) -> 'TriJoiner[A,B,C]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A, B, C], A_], left_end_mapping: Callable[[A, B, C], A_],
                    right_start_mapping: Callable[[D], A_], right_end_mapping: Callable[[D], A_]) -> \
            'QuadJoiner[A,B,C,D]':
        ...

    @overload  # noqa
    @staticmethod
    def overlapping(left_start_mapping: Callable[[A, B, C, D], A_], left_end_mapping: Callable[[A, B, C, D], A_],
                    right_start_mapping: Callable[[E], A_], right_end_mapping: Callable[[E], A_]) -> \
            'PentaJoiner[A,B,C,D]':
        ...

    @staticmethod
    def overlapping(start_mapping_or_left_start_mapping, end_mapping_or_left_end_mapping,
                    right_start_mapping=None, right_end_mapping=None):
        """
        Joins every A and B that overlap for an interval which is specified by a start and end property on both A and
        B.
        """
        if start_mapping_or_left_start_mapping is None or end_mapping_or_left_end_mapping is None:
            raise ValueError
        if right_start_mapping is None and right_end_mapping is None:
            return SameOverlappingPropertyUniJoiner(Joiners._delegate().overlapping,
                                                    start_mapping_or_left_start_mapping,
                                                    end_mapping_or_left_end_mapping)
        elif right_start_mapping is not None and right_end_mapping is not None:
            return OverlappingPropertyJoiner(Joiners._delegate().overlapping,
                                             start_mapping_or_left_start_mapping,
                                             end_mapping_or_left_end_mapping,
                                             right_start_mapping,
                                             right_end_mapping)
        else:
            raise ValueError


__all__ = ['Joiners',
           'SamePropertyUniJoiner',
           'PropertyJoiner',
           'OverlappingPropertyJoiner',
           'FilteringJoiner',
           'extract_joiners']
