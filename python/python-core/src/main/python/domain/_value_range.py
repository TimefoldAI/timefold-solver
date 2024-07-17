from .._timefold_java_interop import ensure_init
from typing import TYPE_CHECKING
from decimal import Decimal
if TYPE_CHECKING:
    class CountableValueRange:
        """
        A set of a values for a PlanningVariable.
        These values might be stored in memory as a collection (usually a `list` or `set`),
        but if the values are numbers,
        they can also be stored in memory by their bounds to use less memory and provide more opportunities.
        It always has a discrete (as in non-continuous) range.
        """
        ...


class ValueRangeFactory:
    """
    Factory for `CountableValueRange`.
    """
    # Return cannot be typed, since CountableValueRange does not exist in the globals dict
    # since it is loaded lazily (to not start the JVM prematurely)
    @staticmethod
    def create_int_value_range(start: int, end: int, step: int = None):
        """
        Build a `CountableValueRange` of all `int` values between two bounds.

        Parameters
        ----------
        start : int
            The inclusive lower bound of the range.
        end : int
            The exclusive upper bound of the range.
        step : int, optional
            The step of the range, defaults to ``1``.

        Examples
        --------
        >>> ValueRangeFactory.create_int_value_range(1, 10)
        CountableValueRange([1, 2, 3, 4, 5, 6, 7, 8, 9])
        >>> ValueRangeFactory.create_int_value_range(1, 10, 2)
        CountableValueRange([1, 3, 5, 7, 9])
        """
        ensure_init()
        import jpype.imports
        from ai.timefold.solver.python import PythonValueRangeFactory
        from java.math import BigInteger
        if step is None:
            return PythonValueRangeFactory.createIntValueRange(BigInteger(str(start)), BigInteger(str(end)))
        else:
            return PythonValueRangeFactory.createIntValueRange(BigInteger(str(start)), BigInteger(str(end)),
                                                               BigInteger(str(step)))

    @staticmethod
    def create_float_value_range(start: Decimal, end: Decimal, step: Decimal = None):
        """
        Build a `CountableValueRange` of all `Decimal` values (of a specific scale) between two bounds.

        Parameters
        ----------
        start : Decimal
            The inclusive lower bound of the range.
        end : Decimal
            The exclusive upper bound of the range.
        step : Decimal, optional
            The step of the range, defaults to the lowest positive number
            with the same scale as start.

        Examples
        --------
        >>> ValueRangeFactory.create_float_value_range(Decimal('1.0'), Decimal('2.0'))
        CountableValueRange([1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9])
        >>> ValueRangeFactory.create_float_value_range(Decimal('1.0'), Decimal('2.0'), Decimal('0.2'))
        CountableValueRange([1.0, 1.2, 1.4, 1.6, 1.8])
        """
        ensure_init()
        import jpype.imports
        from ai.timefold.solver.python import PythonValueRangeFactory
        from java.math import BigDecimal
        if step is None:
            return PythonValueRangeFactory.createFloatValueRange(BigDecimal(str(start)), BigDecimal(str(end)))
        else:
            return PythonValueRangeFactory.createFloatValueRange(BigDecimal(str(start)), BigDecimal(str(end)),
                                                                 BigDecimal(str(step)))

    @staticmethod
    def create_bool_value_range():
        """
        Build a CountableValueRange of both boolean values.

        Examples
        --------
        >>> ValueRangeFactory.create_bool_value_range()
        CountableValueRange([True, False])
        """
        ensure_init()
        import jpype.imports
        from ai.timefold.solver.python import PythonValueRangeFactory
        return PythonValueRangeFactory.createBooleanValueRange()


def lookup_value_range_class(name: str):
    ensure_init()
    import jpype.imports
    from ai.timefold.solver.core.api.domain.valuerange import CountableValueRange
    match name:
        case 'CountableValueRange':
            return CountableValueRange

        case _:
            raise AttributeError(f"module '{__name__}' has no attribute '{name}'")


__all__ = ['ValueRangeFactory']
