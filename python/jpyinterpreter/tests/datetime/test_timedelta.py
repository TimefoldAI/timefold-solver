from datetime import timedelta

from ..conftest import verifier_for


# Constructor
def test_constructor():
    def positional_constructor(days: int, seconds: int, microseconds: int,
                 milliseconds: int, minutes: int, hours: int, weeks: int) -> timedelta:
        return timedelta(days, seconds, microseconds,
                         milliseconds, minutes, hours, weeks)

    def keyword_constructor(days: int, seconds: int, microseconds: int,
                               milliseconds: int, minutes: int, hours: int, weeks: int) -> timedelta:
        return timedelta(days=days, seconds=seconds, microseconds=microseconds,
                         milliseconds=milliseconds, minutes=minutes, hours=hours, weeks=weeks)

    def partial_constructor(seconds: int) -> timedelta:
        return timedelta(seconds=seconds)

    positional_verifier = verifier_for(positional_constructor)
    keyword_verifier = verifier_for(keyword_constructor)
    partial_verifier = verifier_for(partial_constructor)

    positional_verifier.verify(50, 27, 10, 29000, 5, 8, 2,
                               expected_result=timedelta(days=64, seconds=29156, microseconds=10))
    keyword_verifier.verify(50, 27, 10, 29000, 5, 8, 2,
                            expected_result=timedelta(days=64, seconds=29156, microseconds=10))
    partial_verifier.verify(29156, expected_result=timedelta(seconds=29156))


# Instance attributes
def test_instance_attributes():
    def function(a: timedelta) -> tuple:
        return a.days, a.seconds, a.microseconds

    verifier = verifier_for(function)

    verifier.verify(timedelta(days=50, seconds=27, microseconds=10, milliseconds=29000, minutes=5, hours=8, weeks=2),
                    expected_result=(64, 29156, 10))


# Operations
def test_add():
    def function(a: timedelta, b: timedelta) -> timedelta:
        return a + b

    verifier = verifier_for(function)

    verifier.verify(timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                    timedelta(days=2, seconds=3, microseconds=4, milliseconds=5, minutes=6, hours=7, weeks=8),
                    expected_result=
                    timedelta(days=3, seconds=5, microseconds=7, milliseconds=9, minutes=11, hours=13, weeks=15))


def test_subtract():
    def function(a: timedelta, b: timedelta) -> timedelta:
        return a - b

    verifier = verifier_for(function)

    verifier.verify(timedelta(days=2, seconds=3, microseconds=4, milliseconds=5, minutes=6, hours=7, weeks=8),
                    timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                    expected_result=
                    timedelta(days=1, seconds=1, microseconds=1, milliseconds=1, minutes=1, hours=1, weeks=1))

    verifier.verify(timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                    timedelta(days=2, seconds=3, microseconds=4, milliseconds=5, minutes=6, hours=7, weeks=8),
                    expected_result=
                    timedelta(days=-1, seconds=-1, microseconds=-1, milliseconds=-1, minutes=-1, hours=-1, weeks=-1))

    verifier.verify(timedelta(days=3, seconds=5, microseconds=7, milliseconds=9, minutes=11, hours=13, weeks=15),
                    timedelta(days=2, seconds=3, microseconds=4, milliseconds=5, minutes=6, hours=7, weeks=8),
                    expected_result=
                    timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7))


def test_multiply():
    def left_int_function(a: timedelta, b: int) -> timedelta:
        return a * b

    def right_int_function(a: int, b: timedelta) -> timedelta:
        return a * b

    def left_float_function(a: timedelta, b: float) -> timedelta:
        return a * b

    def right_float_function(a: float, b: timedelta) -> timedelta:
        return a * b

    left_int_verifier = verifier_for(left_int_function)
    right_int_verifier = verifier_for(right_int_function)
    left_float_verifier = verifier_for(left_float_function)
    right_float_verifier = verifier_for(right_float_function)

    left_int_verifier.verify(timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7), 3,
                             expected_result=
                             timedelta(days=3, seconds=6, microseconds=9, milliseconds=12, minutes=15, hours=18, weeks=21))
    left_int_verifier.verify(timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7), 0,
                             expected_result=
                             timedelta(0, 0, 0))
    left_int_verifier.verify(timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),-3,
                             expected_result=
                             timedelta(days=-3, seconds=-6, microseconds=-9, milliseconds=-12, minutes=-15, hours=-18, weeks=-21))

    right_int_verifier.verify(3, timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                              expected_result=
                              timedelta(days=3, seconds=6, microseconds=9, milliseconds=12, minutes=15, hours=18, weeks=21))
    right_int_verifier.verify(0, timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                              expected_result=
                              timedelta(0, 0, 0))
    right_int_verifier.verify(-3, timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                              expected_result=
                              timedelta(days=-3, seconds=-6, microseconds=-9, milliseconds=-12, minutes=-15, hours=-18, weeks=-21))

    left_float_verifier.verify(timedelta(days=2, seconds=4, microseconds=6, milliseconds=8, minutes=10, hours=12, weeks=14), 1.5,
                               expected_result=
                               timedelta(days=3, seconds=6, microseconds=9, milliseconds=12, minutes=15, hours=18, weeks=21))
    left_float_verifier.verify(timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7), 0.0,
                               expected_result=
                               timedelta(0, 0, 0))
    left_float_verifier.verify(timedelta(days=2, seconds=4, microseconds=6, milliseconds=8, minutes=10, hours=12, weeks=14), -1.5,
                               expected_result=
                               timedelta(days=-3, seconds=-6, microseconds=-9, milliseconds=-12, minutes=-15, hours=-18, weeks=-21))

    right_float_verifier.verify(1.5, timedelta(days=2, seconds=4, microseconds=6, milliseconds=8, minutes=10, hours=12, weeks=14),
                                expected_result=
                                timedelta(days=3, seconds=6, microseconds=9, milliseconds=12, minutes=15, hours=18, weeks=21))
    right_float_verifier.verify(0.0, timedelta(days=1, seconds=2, microseconds=3, milliseconds=4, minutes=5, hours=6, weeks=7),
                                expected_result=
                                timedelta(0, 0, 0))
    right_float_verifier.verify(-1.5, timedelta(days=2, seconds=4, microseconds=6, milliseconds=8, minutes=10, hours=12, weeks=14),
                                expected_result=
                                timedelta(days=-3, seconds=-6, microseconds=-9, milliseconds=-12, minutes=-15, hours=-18, weeks=-21))


def test_timedelta_division():
    def function(a: timedelta, b: timedelta) -> float:
        return a / b

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), timedelta(seconds=0), expected_error=ZeroDivisionError)
    verifier.verify(timedelta(seconds=10), timedelta(seconds=5), expected_result=2.0)
    verifier.verify(timedelta(seconds=5), timedelta(seconds=10), expected_result=0.5)

    verifier.verify(timedelta(seconds=-10), timedelta(seconds=5), expected_result=-2.0)
    verifier.verify(timedelta(seconds=10), timedelta(seconds=-5), expected_result=-2.0)
    verifier.verify(timedelta(seconds=-10), timedelta(seconds=-5), expected_result=2.0)


def test_number_division():
    def int_function(a: timedelta, b: int) -> timedelta:
        return a / b

    def float_function(a: timedelta, b: float) -> timedelta:
        return a / b

    int_verifier = verifier_for(int_function)
    float_verifier = verifier_for(float_function)

    int_verifier.verify(timedelta(seconds=10), 0, expected_error=ZeroDivisionError)
    int_verifier.verify(timedelta(seconds=10), 2, expected_result=timedelta(seconds=5))
    int_verifier.verify(timedelta(seconds=5), 10, expected_result=timedelta(seconds=0.5))

    int_verifier.verify(timedelta(seconds=-10), 2, expected_result=timedelta(seconds=-5))
    int_verifier.verify(timedelta(seconds=10), -2, expected_result=timedelta(seconds=-5))
    int_verifier.verify(timedelta(seconds=-10), -2, expected_result=timedelta(seconds=5))

    float_verifier.verify(timedelta(seconds=10), 0.0, expected_error=ZeroDivisionError)
    float_verifier.verify(timedelta(seconds=7.5), 1.5, expected_result=timedelta(seconds=5))
    float_verifier.verify(timedelta(seconds=5), 10.0, expected_result=timedelta(seconds=0.5))

    float_verifier.verify(timedelta(seconds=-10), 2.0, expected_result=timedelta(seconds=-5))
    float_verifier.verify(timedelta(seconds=10), -2.0, expected_result=timedelta(seconds=-5))
    float_verifier.verify(timedelta(seconds=-10), -2.0, expected_result=timedelta(seconds=5))


def test_timedelta_floor_division():
    def function(a: timedelta, b: timedelta) -> int:
        return a // b

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), timedelta(seconds=0), expected_error=ZeroDivisionError)
    verifier.verify(timedelta(seconds=10), timedelta(seconds=5), expected_result=2)
    verifier.verify(timedelta(seconds=5), timedelta(seconds=10), expected_result=0)

    verifier.verify(timedelta(seconds=-10), timedelta(seconds=5), expected_result=-2)
    verifier.verify(timedelta(seconds=10), timedelta(seconds=-5), expected_result=-2)
    verifier.verify(timedelta(seconds=-10), timedelta(seconds=-5), expected_result=2)


def test_number_floor_division():
    def int_function(a: timedelta, b: int) -> timedelta:
        return a // b

    int_verifier = verifier_for(int_function)

    int_verifier.verify(timedelta(seconds=10), 0, expected_error=ZeroDivisionError)
    int_verifier.verify(timedelta(seconds=10), 2, expected_result=timedelta(seconds=5))
    int_verifier.verify(timedelta(seconds=5), 10, expected_result=timedelta(seconds=0.5))

    int_verifier.verify(timedelta(seconds=-10), 2, expected_result=timedelta(seconds=-5))
    int_verifier.verify(timedelta(seconds=10), -2, expected_result=timedelta(seconds=-5))
    int_verifier.verify(timedelta(seconds=-10), -2, expected_result=timedelta(seconds=5))


def test_mod():
    def function(a: timedelta, b: timedelta) -> timedelta:
        return a % b

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), timedelta(seconds=0), expected_error=ZeroDivisionError)
    verifier.verify(timedelta(seconds=10), timedelta(seconds=5), expected_result=timedelta(seconds=0))
    verifier.verify(timedelta(seconds=5), timedelta(seconds=10), expected_result=timedelta(seconds=5))
    verifier.verify(timedelta(seconds=15), timedelta(seconds=10), expected_result=timedelta(seconds=5))
    verifier.verify(timedelta(seconds=-15), timedelta(seconds=10), expected_result=timedelta(seconds=5))
    verifier.verify(timedelta(seconds=15), timedelta(seconds=-10), expected_result=timedelta(seconds=-5))
    verifier.verify(timedelta(seconds=-15), timedelta(seconds=-10), expected_result=timedelta(seconds=-5))


def test_divmod():
    def function(a: timedelta, b: timedelta) -> tuple:
        return divmod(a, b)

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), timedelta(seconds=0), expected_error=ZeroDivisionError)
    verifier.verify(timedelta(seconds=10), timedelta(seconds=5), expected_result=(2, timedelta(seconds=0)))
    verifier.verify(timedelta(seconds=5), timedelta(seconds=10), expected_result=(0, timedelta(seconds=5)))
    verifier.verify(timedelta(seconds=15), timedelta(seconds=10), expected_result=(1, timedelta(seconds=5)))
    verifier.verify(timedelta(seconds=-15), timedelta(seconds=10), expected_result=(-2, timedelta(seconds=5)))
    verifier.verify(timedelta(seconds=-15), timedelta(seconds=-10), expected_result=(1, timedelta(seconds=-5)))
    verifier.verify(timedelta(seconds=15), timedelta(seconds=-10), expected_result=(-2, timedelta(seconds=-5)))


def test_pos():
    def function(a: timedelta) -> timedelta:
        return +a

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), expected_result=timedelta(seconds=10))
    verifier.verify(timedelta(seconds=0), expected_result=timedelta(seconds=0))
    verifier.verify(timedelta(seconds=-10), expected_result=timedelta(seconds=-10))


def test_negate():
    def function(a: timedelta) -> timedelta:
        return -a

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), expected_result=timedelta(seconds=-10))
    verifier.verify(timedelta(seconds=0), expected_result=timedelta(seconds=0))
    verifier.verify(timedelta(seconds=-10), expected_result=timedelta(seconds=10))


def test_abs():
    def function(a: timedelta) -> timedelta:
        return abs(a)

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), expected_result=timedelta(seconds=10))
    verifier.verify(timedelta(seconds=0), expected_result=timedelta(seconds=0))
    verifier.verify(timedelta(seconds=-10), expected_result=timedelta(seconds=10))


def test_str():
    def function(a: timedelta) -> str:
        return str(a)

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), expected_result='0:00:10')
    verifier.verify(timedelta(seconds=0), expected_result='0:00:00')
    verifier.verify(timedelta(seconds=-10), expected_result='-1 day, 23:59:50')
    verifier.verify(timedelta(seconds=10, microseconds=123456), expected_result='0:00:10.123456')
    verifier.verify(timedelta(weeks=1, hours=16, minutes=30), expected_result='7 days, 16:30:00')


def test_repr():
    def function(a: timedelta) -> str:
        return repr(a)

    verifier = verifier_for(function)

    verifier.verify(timedelta(seconds=10), expected_result='datetime.timedelta(seconds=10)')
    verifier.verify(timedelta(seconds=0), expected_result='datetime.timedelta(0)')
    verifier.verify(timedelta(seconds=-10), expected_result='datetime.timedelta(days=-1, seconds=86390)')
    verifier.verify(timedelta(seconds=10, microseconds=123456),
                    expected_result='datetime.timedelta(seconds=10, microseconds=123456)')
    verifier.verify(timedelta(weeks=1, hours=16, minutes=30), expected_result='datetime.timedelta(days=7, seconds=59400)')


def test_between_0_and_30_minutes():
    def function(a: timedelta) -> bool:
        return timedelta(minutes=0) <= a <= timedelta(minutes=30)

    verifier = verifier_for(function)

    verifier.verify(timedelta(minutes=0), expected_result=True)
    verifier.verify(timedelta(minutes=15), expected_result=True)
    verifier.verify(timedelta(minutes=30), expected_result=True)

    verifier.verify(timedelta(minutes=-15), expected_result=False)
    verifier.verify(timedelta(minutes=45), expected_result=False)
