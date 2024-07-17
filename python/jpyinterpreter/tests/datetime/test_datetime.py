from datetime import date, time, datetime, timedelta
from typing import Union

from ..conftest import verifier_for


# Constructor
def test_constructor():
    def function(year: int, month: int, day: int) -> datetime:
        return datetime(year, month, day)

    def function_with_time(year: int, month: int, day: int,
                           hour: int, minute: int, second: int, microsecond: int, tzinfo, fold: int) -> datetime:
        return datetime(year, month, day,
                        hour=hour, minute=minute, second=second, microsecond=microsecond, tzinfo=tzinfo, fold=fold)

    verifier = verifier_for(function)
    verifier_with_time = verifier_for(function_with_time)

    verifier.verify(2000, 1, 1, expected_result=datetime(2000, 1, 1))
    verifier.verify(2000, 1, 30, expected_result=datetime(2000, 1, 30))
    verifier.verify(2000, 2, 3, expected_result=datetime(2000, 2, 3))
    verifier.verify(2001, 1, 1, expected_result=datetime(2001, 1, 1))

    verifier.verify(2000, 1, 0, expected_error=ValueError)
    verifier.verify(2000, 1, 32, expected_error=ValueError)
    verifier.verify(2000, 0, 1, expected_error=ValueError)
    verifier.verify(2000, 13, 1, expected_error=ValueError)

    verifier_with_time.verify(2000, 1, 1, 1, 2, 3, 4, None, 0, expected_result=datetime(2000, 1, 1, 1, 2, 3, 4, None,
                                                                                        fold=0))
    verifier_with_time.verify(2000, 1, 30, 10, 20, 30, 40, None, 1, expected_result=datetime(2000, 1, 30,
                                                                                             10, 20, 30, 40,
                                                                                             None, fold=1))


# Instance attributes
def test_instance_attributes():
    def function(a: datetime) -> tuple:
        return a.year, a.month, a.day, a.hour, a.minute, a.second, a.microsecond, a.tzinfo, a.fold

    verifier = verifier_for(function)

    verifier.verify(datetime(2000, 1, 1), expected_result=(2000, 1, 1, 0, 0, 0, 0, None, 0))
    verifier.verify(datetime(2000, 1, 30), expected_result=(2000, 1, 30, 0, 0, 0, 0, None, 0))
    verifier.verify(datetime(2000, 2, 3), expected_result=(2000, 2, 3, 0, 0, 0, 0, None, 0))
    verifier.verify(datetime(2001, 1, 1), expected_result=(2001, 1, 1, 0, 0, 0, 0, None, 0))

    verifier.verify(datetime(2000, 1, 1, 1, 2, 3, 4, None,
                             fold=0), expected_result=(2000, 1, 1, 1, 2, 3, 4, None, 0))
    verifier.verify(datetime(2000, 1, 30,
                             10, 20, 30, 40,
                             None, fold=1), expected_result=(2000, 1, 30, 10, 20, 30, 40, None, 1))


# Operations
def test_comparisons():
    def less_than(a: datetime, b: datetime) -> bool:
        return a < b

    def greater_than(a: datetime, b: datetime) -> bool:
        return a > b

    def less_than_or_equal(a: datetime, b: datetime) -> bool:
        return a <= b

    def greater_than_or_equal(a: datetime, b: datetime) -> bool:
        return a >= b

    def equal(a: datetime, b: datetime) -> bool:
        return a == b

    def not_equal(a: datetime, b: datetime) -> bool:
        return a != b

    less_than_verifier = verifier_for(less_than)
    greater_than_verifier = verifier_for(greater_than)
    less_than_or_equal_verifier = verifier_for(less_than_or_equal)
    greater_than_or_equal_verifier = verifier_for(greater_than_or_equal)
    equal_verifier = verifier_for(equal)
    not_equal_verifier = verifier_for(not_equal)

    sorted_list_of_datetimes = [
        datetime(2000, 1, 3),
        datetime(2000, 1, 3, 0, 0, 45, 0, None, fold=0),
        datetime(2000, 1, 3, 0, 0, 45, 1, None, fold=0),
        datetime(2000, 1, 3, 0, 30, 0, 0, None, fold=0),
        datetime(2000, 1, 3, 12, 0, 0, 0, None, fold=0),
        datetime(2000, 1, 4),
        datetime(2000, 1, 4, 0, 0, 45, 0, None, fold=0),
        datetime(2000, 1, 4, 0, 30, 0, 0, None, fold=0),
        datetime(2000, 1, 4, 12, 0, 0, 0, None, fold=0),
        datetime(2000, 2, 2),
        datetime(2000, 2, 2, 0, 0, 45, 0, None, fold=0),
        datetime(2000, 2, 2, 0, 30, 0, 0, None, fold=0),
        datetime(2000, 2, 2, 12, 0, 0, 0, None, fold=0),
        datetime(2001, 1, 1),
        datetime(2001, 1, 1, 0, 0, 45, 0, None, fold=0),
        datetime(2001, 1, 1, 0, 30, 0, 0, None, fold=0),
        datetime(2001, 1, 1, 12, 0, 0, 0, None, fold=0),
    ]

    for first_index in range(len(sorted_list_of_datetimes)):
        for second_index in range(first_index, len(sorted_list_of_datetimes)):
            smaller = sorted_list_of_datetimes[first_index]
            bigger = sorted_list_of_datetimes[second_index]

            if first_index == second_index:
                less_than_verifier.verify(smaller, bigger, expected_result=False)
                greater_than_verifier.verify(smaller, bigger, expected_result=False)
                less_than_or_equal_verifier.verify(smaller, bigger, expected_result=True)
                greater_than_or_equal_verifier.verify(smaller, bigger, expected_result=True)
                equal_verifier.verify(smaller, bigger, expected_result=True)
                not_equal_verifier.verify(smaller, bigger, expected_result=False)
            else:
                less_than_verifier.verify(smaller, bigger, expected_result=True)
                greater_than_verifier.verify(smaller, bigger, expected_result=False)
                less_than_or_equal_verifier.verify(smaller, bigger, expected_result=True)
                greater_than_or_equal_verifier.verify(smaller, bigger, expected_result=False)
                equal_verifier.verify(smaller, bigger, expected_result=False)
                not_equal_verifier.verify(smaller, bigger, expected_result=True)

                less_than_verifier.verify(bigger, smaller, expected_result=False)
                greater_than_verifier.verify(bigger, smaller, expected_result=True)
                less_than_or_equal_verifier.verify(bigger, smaller, expected_result=False)
                greater_than_or_equal_verifier.verify(bigger, smaller, expected_result=True)
                equal_verifier.verify(bigger, smaller, expected_result=False)
                not_equal_verifier.verify(bigger, smaller, expected_result=True)


def test_subtract_datetime():
    def function(first_date: datetime, second_date: datetime) -> timedelta:
        return first_date - second_date

    verifier = verifier_for(function)

    verifier.verify(datetime(2000, 1, 1), datetime(2000, 1, 1), expected_result=timedelta(days=0))

    verifier.verify(datetime(2000, 1, 3), datetime(2000, 1, 1), expected_result=timedelta(days=2))
    verifier.verify(datetime(2000, 1, 1), datetime(2000, 1, 3), expected_result=timedelta(days=-2))

    verifier.verify(datetime(2000, 2, 1), datetime(2000, 1, 1), expected_result=timedelta(days=31))
    verifier.verify(datetime(2000, 1, 1), datetime(2000, 2, 1), expected_result=timedelta(days=-31))

    verifier.verify(datetime(2001, 1, 1), datetime(2000, 1, 1), expected_result=timedelta(days=366))
    verifier.verify(datetime(2000, 1, 1), datetime(2001, 1, 1), expected_result=timedelta(days=-366))

    verifier.verify(datetime(2000, 1, 1, 12, 0, 0), datetime(2000, 1, 1, 5, 30, 45),
                    expected_result=timedelta(seconds=23355))


def test_add_timedelta():
    def function(first_date: datetime, difference: timedelta) -> datetime:
        return first_date + difference

    verifier = verifier_for(function)

    verifier.verify(datetime(2000, 1, 1), timedelta(days=0), expected_result=datetime(2000, 1, 1))

    verifier.verify(datetime(2000, 1, 1), timedelta(days=2), expected_result=datetime(2000, 1, 3))
    verifier.verify(datetime(2000, 1, 3), timedelta(days=-2), expected_result=datetime(2000, 1, 1))

    verifier.verify(datetime(2000, 1, 1), timedelta(days=31), expected_result=datetime(2000, 2, 1))
    verifier.verify(datetime(2000, 2, 1), timedelta(days=-31), expected_result=datetime(2000, 1, 1))

    verifier.verify(datetime(2000, 1, 1), timedelta(days=366), expected_result=datetime(2001, 1, 1))
    verifier.verify(datetime(2001, 1, 1), timedelta(days=-366), expected_result=datetime(2000, 1, 1))

    verifier.verify(datetime(2000, 1, 1), timedelta(hours=12, minutes=30), expected_result=datetime(2000, 1, 1, 12, 30))


def test_subtract_timedelta():
    def function(first_date: datetime, difference: timedelta) -> datetime:
        return first_date - difference

    verifier = verifier_for(function)

    verifier.verify(datetime(2000, 1, 1), timedelta(days=0), expected_result=datetime(2000, 1, 1))

    verifier.verify(datetime(2000, 1, 3), timedelta(days=2), expected_result=datetime(2000, 1, 1))
    verifier.verify(datetime(2000, 1, 1), timedelta(days=-2), expected_result=datetime(2000, 1, 3))

    verifier.verify(datetime(2000, 2, 1), timedelta(days=31), expected_result=datetime(2000, 1, 1))
    verifier.verify(datetime(2000, 1, 1), timedelta(days=-31), expected_result=datetime(2000, 2, 1))

    verifier.verify(datetime(2001, 1, 1), timedelta(days=366), expected_result=datetime(2000, 1, 1))
    verifier.verify(datetime(2000, 1, 1), timedelta(days=-366), expected_result=datetime(2001, 1, 1))
    verifier.verify(datetime(2000, 1, 1, 12, 30), timedelta(hours=12, minutes=30), expected_result=datetime(2000, 1, 1))


def test_fromtimestamp():
    def function(timestamp: Union[int, float]) -> datetime:
        return datetime.fromtimestamp(timestamp)

    verifier = verifier_for(function)

    # cannot use expected result; python timestamps use system timezone
    verifier.verify(0)
    verifier.verify(4000)
    verifier.verify(200000)


def test_fromordinal():
    def function(ordinal: int) -> datetime:
        return datetime.fromordinal(ordinal)

    verifier = verifier_for(function)

    verifier.verify(1, expected_result=datetime(1, 1, 1))
    verifier.verify(2, expected_result=datetime(1, 1, 2))
    verifier.verify(32, expected_result=datetime(1, 2, 1))
    verifier.verify(1000, expected_result=datetime(3, 9, 27))


def test_fromisoformat():
    def function(date_string: str) -> datetime:
        return datetime.fromisoformat(date_string)

    verifier = verifier_for(function)

    verifier.verify('2000-01-01', expected_result=datetime(2000, 1, 1))
    verifier.verify('1999-02-03', expected_result=datetime(1999, 2, 3))


def test_fromisocalendar():
    def function(year: int, week: int, day: int) -> datetime:
        return datetime.fromisocalendar(year, week, day)

    verifier = verifier_for(function)

    verifier.verify(2000, 1, 1, expected_result=datetime(2000, 1, 3))
    verifier.verify(1999, 2, 3, expected_result=datetime(1999, 1, 13))


def test_date():
    def function(x: datetime) -> date:
        return x.date()

    verifier = verifier_for(function)

    verifier.verify(datetime(2000, 1, 1, 1, 2, 3, 4, None, fold=0), expected_result=date(2000, 1, 1))
    verifier.verify(datetime(1999, 1, 13, 10, 20, 30, 40, None, fold=0), expected_result=date(1999, 1, 13))


def test_time():
    def function(x: datetime) -> time:
        return x.time()

    verifier = verifier_for(function)

    verifier.verify(datetime(2000, 1, 1, 1, 2, 3, 4, None, fold=0), expected_result=time(1, 2, 3, 4, None, fold=0))
    verifier.verify(datetime(1999, 1, 13, 10, 20, 30, 40, None, fold=0),
                    expected_result=time(10, 20, 30, 40, None, fold=0))


def test_combine():
    def function(a: date, b: time) -> datetime:
        return datetime.combine(a, b)

    verifier = verifier_for(function)
    verifier.verify(date(2000, 1, 1), time(1, 2, 3, 4, None, fold=0),
                    expected_result=datetime(2000, 1, 1, 1, 2, 3, 4, None, fold=0))


def test_replace():
    def replace_year(x: datetime, year: int) -> datetime:
        return x.replace(year=year)

    def replace_month(x: datetime, month: int) -> datetime:
        return x.replace(month=month)

    def replace_day(x: datetime, day: int) -> datetime:
        return x.replace(day=day)

    def replace_year_month(x: datetime, year: int, month: int) -> datetime:
        return x.replace(year=year, month=month)

    def replace_month_day(x: datetime, month: int, day: int) -> datetime:
        return x.replace(month=month, day=day)

    def replace_year_day(x: datetime, year: int, day: int) -> datetime:
        return x.replace(year=year, day=day)

    def replace_hour(x: datetime, hour: int) -> datetime:
        return x.replace(hour=hour)

    def replace_minute(x: datetime, minute: int) -> datetime:
        return x.replace(minute=minute)

    def replace_second(x: datetime, second: int) -> datetime:
        return x.replace(second=second)

    def replace_micro(x: datetime, micro: int) -> datetime:
        return x.replace(microsecond=micro)

    def replace_fold(x: datetime, fold: int) -> datetime:
        return x.replace(fold=fold)

    def replace_all(x: datetime, year: int, month: int, day: int,
                    hour: int, minute: int, second: int, microsecond: int, tzinfo, fold: int) -> datetime:
        return x.replace(year=year, month=month, day=day,
                         hour=hour, minute=minute, second=second, microsecond=microsecond,
                         tzinfo=tzinfo, fold=fold)

    replace_year_verifier = verifier_for(replace_year)
    replace_month_verifier = verifier_for(replace_month)
    replace_day_verifier = verifier_for(replace_day)
    replace_year_month_verifier = verifier_for(replace_year_month)
    replace_month_day_verifier = verifier_for(replace_month_day)
    replace_year_day_verifier = verifier_for(replace_year_day)
    replace_hour_verifier = verifier_for(replace_hour)
    replace_minute_verifier = verifier_for(replace_minute)
    replace_second_verifier = verifier_for(replace_second)
    replace_micro_verifier = verifier_for(replace_micro)
    replace_fold_verifier = verifier_for(replace_fold)
    replace_all_verifier = verifier_for(replace_all)

    replace_year_verifier.verify(datetime(2002, 12, 4), 3000, expected_result=datetime(3000, 12, 4))
    replace_month_verifier.verify(datetime(2002, 12, 4), 3, expected_result=datetime(2002, 3, 4))
    replace_day_verifier.verify(datetime(2002, 12, 4), 10, expected_result=datetime(2002, 12, 10))
    replace_year_month_verifier.verify(datetime(2002, 12, 4), 3000, 3, expected_result=datetime(3000, 3, 4))
    replace_month_day_verifier.verify(datetime(2002, 12, 4), 3, 10, expected_result=datetime(2002, 3, 10))
    replace_year_day_verifier.verify(datetime(2002, 12, 4), 3000, 10, expected_result=datetime(3000, 12, 10))
    replace_hour_verifier.verify(datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=0), 10,
                                 expected_result=datetime(2002, 12, 4, 10, 2, 3, 4, None, fold=0))
    replace_minute_verifier.verify(datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=0), 20,
                                   expected_result=datetime(2002, 12, 4, 1, 20, 3, 4, None, fold=0))
    replace_second_verifier.verify(datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=0), 30,
                                   expected_result=datetime(2002, 12, 4, 1, 2, 30, 4, None, fold=0))
    replace_micro_verifier.verify(datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=0), 40,
                                  expected_result=datetime(2002, 12, 4, 1, 2, 3, 40, None, fold=0))
    replace_fold_verifier.verify(datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=0), 1,
                                 expected_result=datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=1))
    replace_all_verifier.verify(datetime(2002, 12, 4, 1, 2, 3, 4, None, fold=0),
                                3000, 2, 1, 10, 20, 30, 40, None, 1,
                                expected_result=datetime(3000, 2, 1, 10, 20, 30, 40, None, fold=1))


def test_timetuple():
    def function(x: datetime) -> tuple:
        return x.timetuple()

    verifier = verifier_for(function)

    # TODO: enable type checking when named tuples are supported
    verifier.verify(datetime(1, 1, 1), expected_result=(1, 1, 1, 0, 0, 0, 0, 1, -1), type_check=False)
    verifier.verify(datetime(3, 9, 27), expected_result=(3, 9, 27, 0, 0, 0, 5, 270, -1), type_check=False)


def test_toordinal():
    def function(x: datetime) -> int:
        return x.toordinal()

    verifier = verifier_for(function)

    verifier.verify(datetime(1, 1, 1), expected_result=1)
    verifier.verify(datetime(1, 1, 2), expected_result=2)
    verifier.verify(datetime(1, 2, 1), expected_result=32)
    verifier.verify(datetime(3, 9, 27), expected_result=1000)


def test_weekday():
    def function(x: datetime) -> int:
        return x.weekday()

    verifier = verifier_for(function)

    verifier.verify(datetime(2002, 12, 4), expected_result=2)


def test_isoweekday():
    def function(x: datetime) -> int:
        return x.isoweekday()

    verifier = verifier_for(function)

    verifier.verify(datetime(2002, 12, 4), expected_result=3)


def test_isocalendar():
    def function(x: datetime) -> tuple:
        return x.isocalendar()

    verifier = verifier_for(function)

    verifier.verify(datetime(2003, 12, 29), expected_result=(2004, 1, 1), type_check=False)
    verifier.verify(datetime(2004, 1, 4), expected_result=(2004, 1, 7), type_check=False)


def test_isoformat():
    def function(x: datetime) -> str:
        return x.isoformat()

    verifier = verifier_for(function)

    verifier.verify(datetime(2003, 12, 29), expected_result='2003-12-29T00:00:00')
    verifier.verify(datetime(2004, 1, 4), expected_result='2004-01-04T00:00:00')


def test_str():
    def function(x: datetime) -> str:
        return str(x)

    verifier = verifier_for(function)

    verifier.verify(datetime(2003, 12, 29), expected_result='2003-12-29 00:00:00')
    verifier.verify(datetime(2004, 1, 4), expected_result='2004-01-04 00:00:00')


def test_ctime():
    def function(x: datetime) -> str:
        return x.ctime()

    verifier = verifier_for(function)

    verifier.verify(datetime(2002, 12, 4), expected_result='Wed Dec  4 00:00:00 2002')


def test_strftime():
    def function(x: datetime, fmt: str) -> str:
        return x.strftime(fmt)

    verifier = verifier_for(function)

    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%a',
                    expected_result='Sat')
    # Java C Locale uses the short form for the full variant of week days
    # verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%A',
    #                 expected_result='Saturday')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%W',
                    expected_result='05')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%d',
                    expected_result='03')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%b',
                    expected_result='Feb')
    # Java C Locale uses the short form for the full variant of months
    # verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%B',
    #                 expected_result='February')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%m',
                    expected_result='02')
    verifier.verify(datetime(1901, 2, 3, 4, 5, 6, 7), '%y',
                    expected_result='01')
    verifier.verify(datetime(2001, 2, 3, 4, 5, 6, 7), '%y',
                    expected_result='01')
    # %Y have different results depending on the platform;
    # Windows 0-pad it, Linux does not.
    # verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%Y',
    #                 expected_result='1')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%j',
                    expected_result='034')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%U',
                    expected_result='04')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%W',
                    expected_result='05')
    # %Y have different results depending on the platform;
    # Windows 0-pad it, Linux does not.
    # verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%G',
    #                 expected_result='1')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%u',
                    expected_result='6')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%%',
                    expected_result='%')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%V',
                    expected_result='05')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%H',
                    expected_result='04')
    verifier.verify(datetime(12, 2, 3, 13, 5, 6, 7), '%I',
                    expected_result='01')
    verifier.verify(datetime(13, 2, 3, 4, 5, 6, 7), '%p',
                    expected_result='AM')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%M',
                    expected_result='05')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%S',
                    expected_result='06')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%f',
                    expected_result='000007')
    # %X is locale-specific, and Java/Python locale definitions can slightly differ
    # ex: en_US = '4:05:06 AM' in Java, but '04:05:06 AM' in Python
    # verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%X',
    #                 expected_result='04:05:06 AM')
    verifier.verify(datetime(1, 2, 3, 4, 5, 6, 7), '%%',
                    expected_result='%')


def test_strptime():
    def function(date_string: str, fmt: str) -> datetime:
        return datetime.strptime(date_string, fmt)

    verifier = verifier_for(function)

    verifier.verify("21 Jun, 2018", "%d %b, %Y",
                    expected_result=datetime(2018, 6, 21))
    verifier.verify("12/11/2018 09:15:32", "%m/%d/%Y %H:%M:%S",
                    expected_result=datetime(2018, 12, 11, 9, 15, 32))
    verifier.verify("12/11/2018 09:15:32", "%d/%m/%Y %H:%M:%S",
                    expected_result=datetime(2018, 11, 12, 9, 15, 32))
    verifier.verify("09:15:32", "%H:%M:%S",
                    expected_result=datetime(1900, 1, 1, 9, 15, 32))
    verifier.verify("text", "%H:%M:%S",
                    expected_error=ValueError)
