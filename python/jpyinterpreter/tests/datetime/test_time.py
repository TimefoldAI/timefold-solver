from datetime import time

from ..conftest import verifier_for


# Constructor
def test_constructor():
    def function(hour: int, minute: int, second: int, microsecond: int, tzinfo, fold: int) -> time:
        return time(hour=hour, minute=minute, second=second, microsecond=microsecond, tzinfo=tzinfo, fold=fold)

    verifier = verifier_for(function)

    verifier.verify(1, 2, 3, 4, None, 0, expected_result=time(hour=1, minute=2, second=3, microsecond=4,
                                                              tzinfo=None, fold=0))
    verifier.verify(10, 20, 30, 40, None, 1, expected_result=time(hour=10, minute=20, second=30, microsecond=40,
                                                                  tzinfo=None, fold=1))

    verifier.verify(-1, 2, 3, 4, None, 0, expected_error=ValueError)
    verifier.verify(24, 2, 3, 4, None, 0, expected_error=ValueError)

    verifier.verify(1, -1, 3, 4, None, 0, expected_error=ValueError)
    verifier.verify(1, 60, 3, 4, None, 0, expected_error=ValueError)

    verifier.verify(1, 2, -1, 4, None, 0, expected_error=ValueError)
    verifier.verify(1, 2, 60, 4, None, 0, expected_error=ValueError)

    verifier.verify(1, 2, 3, -1, None, 0, expected_error=ValueError)
    verifier.verify(1, 2, 3, 1_000_000, None, 0, expected_error=ValueError)

    verifier.verify(1, 2, 3, 4, None, -1, expected_error=ValueError)
    verifier.verify(1, 2, 3, 4, None, 2, expected_error=ValueError)


# Instance attributes
def test_instance_attributes():
    def function(a: time) -> tuple:
        return a.hour, a.minute, a.second, a.microsecond, a.tzinfo, a.fold

    verifier = verifier_for(function)

    verifier.verify(time(hour=1, minute=2, second=3, microsecond=4,
                         tzinfo=None, fold=0),
                    expected_result=(1, 2, 3, 4, None, 0))

    verifier.verify(time(hour=10, minute=20, second=30, microsecond=40,
                         tzinfo=None, fold=1),
                    expected_result=(10, 20, 30, 40, None, 1))


# Operations
def test_comparisons():
    def equal(a: time, b: time) -> bool:
        return a == b

    def not_equal(a: time, b: time) -> bool:
        return a != b

    equal_verifier = verifier_for(equal)
    not_equal_verifier = verifier_for(not_equal)

    sorted_list_of_times = [
        time(hour=1, minute=2, second=3, microsecond=4,
             tzinfo=None, fold=0),
        time(hour=10, minute=20, second=30, microsecond=40,
             tzinfo=None, fold=1)
    ]

    for first_index in range(len(sorted_list_of_times)):
        for second_index in range(first_index, len(sorted_list_of_times)):
            a = sorted_list_of_times[first_index]
            b = sorted_list_of_times[second_index]

            if first_index == second_index:
                equal_verifier.verify(a, b, expected_result=True)
                not_equal_verifier.verify(a, b, expected_result=False)
            else:
                equal_verifier.verify(a, b, expected_result=False)
                not_equal_verifier.verify(a, b, expected_result=True)

                equal_verifier.verify(b, a, expected_result=False)
                not_equal_verifier.verify(b, a, expected_result=True)


def test_fromisoformat():
    def function(time_string: str) -> time:
        return time.fromisoformat(time_string)

    verifier = verifier_for(function)

    verifier.verify('04:23:01', expected_result=time(4, 23, 1))
    verifier.verify('04:23:01.000384', expected_result=time(4, 23, 1, 384))
    # TODO: Timezone support
    # verifier.verify('04:23:01+04:00', expected_result=time(4, 23, 1,
    #                                                        tzinfo=timezone(timedelta(seconds=14400))))


def test_replace():
    def replace_hour(x: time, hour: int) -> time:
        return x.replace(hour=hour)

    def replace_minute(x: time, minute: int) -> time:
        return x.replace(minute=minute)

    def replace_second(x: time, second: int) -> time:
        return x.replace(second=second)

    def replace_micro(x: time, micro: int) -> time:
        return x.replace(microsecond=micro)

    def replace_fold(x: time, fold: int) -> time:
        return x.replace(fold=fold)

    def replace_all(x: time, hour: int, minute: int, second: int, microsecond: int, tzinfo, fold: int) -> time:
        return x.replace(hour=hour, minute=minute, second=second, microsecond=microsecond, tzinfo=tzinfo, fold=fold)

    replace_hour_verifier = verifier_for(replace_hour)
    replace_minute_verifier = verifier_for(replace_minute)
    replace_second_verifier = verifier_for(replace_second)
    replace_micro_verifier = verifier_for(replace_micro)
    replace_fold_verifier = verifier_for(replace_fold)
    replace_all_verifier = verifier_for(replace_all)

    replace_hour_verifier.verify(time(1, 2, 3, 4, None, fold=0), 10,
                                 expected_result=time(10, 2, 3, 4, None, fold=0))
    replace_minute_verifier.verify(time(1, 2, 3, 4, None, fold=0), 20,
                                   expected_result=time(1, 20, 3, 4, None, fold=0))
    replace_second_verifier.verify(time(1, 2, 3, 4, None, fold=0), 30,
                                   expected_result=time(1, 2, 30, 4, None, fold=0))
    replace_micro_verifier.verify(time(1, 2, 3, 4, None, fold=0), 40,
                                  expected_result=time(1, 2, 3, 40, None, fold=0))
    replace_fold_verifier.verify(time(1, 2, 3, 4, None, fold=0), 1,
                                 expected_result=time(1, 2, 3, 4, None, fold=1))
    replace_all_verifier.verify(time(1, 2, 3, 4, None, fold=0),
                                10, 20, 30, 40, None, 1,
                                expected_result=time(10, 20, 30, 40, None, fold=1))


def test_isoformat():
    def function(x: time) -> str:
        return x.isoformat()

    def function_with_timespec(x: time, timespec: str) -> str:
        return x.isoformat(timespec)

    verifier = verifier_for(function)
    timespec_verifier = verifier_for(function_with_timespec)

    verifier.verify(time(1, 2, 3, 4, None, fold=0), expected_result='01:02:03.000004')
    verifier.verify(time(10, 20, 30, 40, None, fold=1), expected_result='10:20:30.000040')

    verifier.verify(time(1, 2, 3, 0, None, fold=0), expected_result='01:02:03')
    verifier.verify(time(10, 20, 30, 0, None, fold=1), expected_result='10:20:30')

    timespec_verifier.verify(time(10, 20, 30, 40, None, fold=0), 'auto', expected_result='10:20:30.000040')
    timespec_verifier.verify(time(10, 20, 30, 40, None, fold=0), 'hours', expected_result='10')
    timespec_verifier.verify(time(10, 20, 30, 40, None, fold=0), 'minutes', expected_result='10:20')
    timespec_verifier.verify(time(10, 20, 30, 40, None, fold=0), 'seconds', expected_result='10:20:30')
    timespec_verifier.verify(time(10, 20, 30, 40, None, fold=0), 'milliseconds', expected_result='10:20:30.000')
    timespec_verifier.verify(time(10, 20, 30, 40, None, fold=0), 'microseconds', expected_result='10:20:30.000040')


def test_str():
    def function(x: time) -> str:
        return str(x)

    verifier = verifier_for(function)

    verifier.verify(time(1, 2, 3, 0, None, fold=0), expected_result='01:02:03')
    verifier.verify(time(10, 20, 30, 0, None, fold=1), expected_result='10:20:30')
    verifier.verify(time(1, 2, 3, 4, None, fold=0), expected_result='01:02:03.000004')


def test_strftime():
    def function(x: time, fmt: str) -> str:
        return x.strftime(fmt)

    verifier = verifier_for(function)

    verifier.verify(time(1, 2, 3, 4, None, fold=0), '%H',
                    expected_result='01')
    verifier.verify(time(13, 2, 3, 4, None, fold=0), '%I',
                    expected_result='01')
    verifier.verify(time(13, 2, 3, 4, None, fold=0), '%p',
                    expected_result='PM')
    verifier.verify(time(1, 2, 3, 4, None, fold=0), '%M',
                    expected_result='02')
    verifier.verify(time(1, 2, 3, 4, None, fold=0), '%S',
                    expected_result='03')
    verifier.verify(time(1, 2, 3, 4, None, fold=0), '%f',
                    expected_result='000004')

    # %X is locale-specific, and Java/Python locale definitions can slightly differ
    # ex: en_US = '1:02:03 AM' in Java, but '01:02:03 AM' in Python
    # verifier.verify(time(1, 2, 3, 4, None, fold=0), '%X',
    #                 expected_result='01:02:03 AM')
    verifier.verify(time(1, 2, 3, 4, None, fold=0), '%%',
                    expected_result='%')

# TODO: strftime, __format__, utcoffset, dst, tzname
