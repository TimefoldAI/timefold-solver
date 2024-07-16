package ai.timefold.jpyinterpreter.types.datetime;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.types.PythonLikeComparable;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.numeric.PythonNumber;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

/**
 * Python docs: <a href="https://docs.python.org/3/library/datetime.html#datetime.datetime">datetime objects</a>
 */
public class PythonDateTime extends PythonDate<PythonDateTime> implements PlanningImmutable {
    // Taken from https://docs.python.org/3/library/datetime.html#datetime.datetime.fromisoformat
    private static final Pattern ISO_FORMAT_PATTERN = Pattern.compile("^(?<year>\\d\\d\\d\\d)-(?<month>\\d\\d)-(?<day>\\d\\d)" +
            "(.(?<hour>\\d\\d)" +
            "(:(?<minute>\\d\\d)" +
            "(:(?<second>\\d\\d)" +
            "(\\.(?<microHigh>\\d\\d\\d)" +
            "(?<microLow>\\d\\d\\d)?" +
            ")?)?)?)?" +
            "(\\+(?<timezoneHour>\\d\\d):(?<timezoneMinute>\\d\\d)" +
            "(:(?<timezoneSecond>\\d\\d)" +
            "(\\.(?<timezoneMicro>\\d\\d\\d\\d\\d\\d)" +
            ")?)?)?$");

    private static final int NANOS_PER_SECOND = 1_000_000_000;
    public static PythonLikeType DATE_TIME_TYPE = new PythonLikeType("datetime",
            PythonDateTime.class,
            List.of(DATE_TYPE));

    public static PythonLikeType $TYPE = DATE_TIME_TYPE;

    static {
        try {
            PythonLikeComparable.setup(DATE_TIME_TYPE);
            registerMethods();

            DATE_TIME_TYPE.$setAttribute("min", new PythonDateTime(LocalDate.of(1, 1, 1),
                    LocalTime.MAX));
            DATE_TIME_TYPE.$setAttribute("max", new PythonDateTime(LocalDate.of(9999, 12, 31),
                    LocalTime.MIN));
            DATE_TIME_TYPE.$setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000L)));

            PythonOverloadImplementor.createDispatchesFor(DATE_TIME_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        // Constructor
        DATE_TIME_TYPE.addConstructor(ArgumentSpec.forFunctionReturning("datetime", PythonDateTime.class.getName())
                .addArgument("year", PythonInteger.class.getName())
                .addArgument("month", PythonInteger.class.getName())
                .addArgument("day", PythonInteger.class.getName())
                .addArgument("hour", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("minute", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("second", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("microsecond", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("tzinfo", PythonLikeObject.class.getName(), PythonNone.INSTANCE)
                .addKeywordOnlyArgument("fold", PythonInteger.class.getName(), PythonInteger.ZERO)
                .asPythonFunctionSignature(
                        PythonDateTime.class.getMethod("of", PythonInteger.class, PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonInteger.class, PythonInteger.class, PythonInteger.class,
                                PythonLikeObject.class, PythonInteger.class)));

        // Class methods
        // Date handles today,
        DATE_TIME_TYPE.addMethod("now",
                ArgumentSpec.forFunctionReturning("now", PythonDateTime.class.getName())
                        .addArgument("datetime_type", PythonLikeType.class.getName())
                        .addArgument("tzinfo", PythonLikeObject.class.getName(), PythonNone.INSTANCE)
                        .asClassPythonFunctionSignature(
                                PythonDateTime.class.getMethod("now",
                                        PythonLikeType.class,
                                        PythonLikeObject.class)));

        DATE_TIME_TYPE.addMethod("utcnow",
                ArgumentSpec.forFunctionReturning("now", PythonDateTime.class.getName())
                        .addArgument("datetime_type", PythonLikeType.class.getName())
                        .asClassPythonFunctionSignature(
                                PythonDateTime.class.getMethod("utc_now",
                                        PythonLikeType.class)));

        DATE_TIME_TYPE.addMethod("fromtimestamp",
                ArgumentSpec.forFunctionReturning("fromtimestamp", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .addArgument("timestamp", PythonNumber.class.getName())
                        .addArgument("tzinfo", PythonLikeObject.class.getName(), PythonNone.INSTANCE)
                        .asClassPythonFunctionSignature(PythonDateTime.class.getMethod("from_timestamp",
                                PythonLikeType.class,
                                PythonNumber.class,
                                PythonLikeObject.class)));

        DATE_TIME_TYPE.addMethod("strptime",
                ArgumentSpec.forFunctionReturning("strptime", PythonDateTime.class.getName())
                        .addArgument("datetime_type", PythonLikeType.class.getName())
                        .addArgument("date_string", PythonString.class.getName())
                        .addArgument("format", PythonString.class.getName())
                        .asClassPythonFunctionSignature(PythonDateTime.class.getMethod("strptime",
                                PythonLikeType.class,
                                PythonString.class,
                                PythonString.class)));

        DATE_TIME_TYPE.addMethod("utcfromtimestamp",
                ArgumentSpec.forFunctionReturning("utcfromtimestamp", PythonDate.class.getName())
                        .addArgument("date_type", PythonLikeType.class.getName())
                        .addArgument("timestamp", PythonNumber.class.getName())
                        .asClassPythonFunctionSignature(PythonDateTime.class.getMethod("utc_from_timestamp",
                                PythonLikeType.class,
                                PythonNumber.class)));

        DATE_TIME_TYPE.addMethod("combine",
                ArgumentSpec.forFunctionReturning("combine", PythonDateTime.class.getName())
                        .addArgument("datetime_type", PythonLikeType.class.getName())
                        .addArgument("date", PythonDate.class.getName())
                        .addArgument("time", PythonTime.class.getName())
                        .addNullableArgument("tzinfo", PythonLikeObject.class.getName())
                        .asClassPythonFunctionSignature(
                                PythonDateTime.class.getMethod("combine",
                                        PythonLikeType.class, PythonDate.class,
                                        PythonTime.class, PythonLikeObject.class)));

        // Unary Operators
        DATE_TIME_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                PythonDateTime.class.getMethod("toPythonString"));

        // Binary Operators
        DATE_TIME_TYPE.addBinaryMethod(PythonBinaryOperator.ADD,
                PythonDateTime.class.getMethod("add_time_delta", PythonTimeDelta.class));
        DATE_TIME_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonDateTime.class.getMethod("subtract_time_delta", PythonTimeDelta.class));
        DATE_TIME_TYPE.addBinaryMethod(PythonBinaryOperator.SUBTRACT,
                PythonDateTime.class.getMethod("subtract_date_time", PythonDateTime.class));

        // Instance methods
        DATE_TIME_TYPE.addMethod("replace",
                ArgumentSpec.forFunctionReturning("replace", PythonDate.class.getName())
                        .addNullableArgument("year", PythonInteger.class.getName())
                        .addNullableArgument("month", PythonInteger.class.getName())
                        .addNullableArgument("day", PythonInteger.class.getName())
                        .addNullableArgument("hour", PythonInteger.class.getName())
                        .addNullableArgument("minute", PythonInteger.class.getName())
                        .addNullableArgument("second", PythonInteger.class.getName())
                        .addNullableArgument("microsecond", PythonInteger.class.getName())
                        .addNullableArgument("tzinfo", PythonLikeObject.class.getName())
                        .addNullableKeywordOnlyArgument("fold", PythonInteger.class.getName())
                        .asPythonFunctionSignature(PythonDateTime.class.getMethod("replace", PythonInteger.class,
                                PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonLikeObject.class, PythonInteger.class)));
        DATE_TIME_TYPE.addMethod("timetuple",
                PythonDateTime.class.getMethod("timetuple")); // TODO: use time.struct_time type

        DATE_TIME_TYPE.addMethod("utctimetuple",
                PythonDateTime.class.getMethod("utctimetuple")); // TODO: use time.struct_time type

        DATE_TIME_TYPE.addMethod("date",
                PythonDateTime.class.getMethod("date"));
        DATE_TIME_TYPE.addMethod("time",
                PythonDateTime.class.getMethod("time"));

        DATE_TIME_TYPE.addMethod("timetz",
                PythonDateTime.class.getMethod("timetz"));

        DATE_TIME_TYPE.addMethod("astimezone",
                PythonDateTime.class.getMethod("astimezone", PythonTzinfo.class));

        DATE_TIME_TYPE.addMethod("timestamp",
                PythonDateTime.class.getMethod("timestamp"));

        DATE_TIME_TYPE.addMethod("tzname",
                PythonDateTime.class.getMethod("tzname"));

        DATE_TIME_TYPE.addMethod("utcoffset",
                PythonDateTime.class.getMethod("utcoffset"));

        DATE_TIME_TYPE.addMethod("dst",
                PythonDateTime.class.getMethod("dst"));

        DATE_TIME_TYPE.addMethod("isoformat",
                ArgumentSpec.forFunctionReturning("isoformat", PythonString.class.getName())
                        .addArgument("sep", PythonString.class.getName(), PythonString.valueOf("T"))
                        .addArgument("timespec", PythonString.class.getName(), PythonString.valueOf("auto"))
                        .asPythonFunctionSignature(
                                PythonDateTime.class.getMethod("iso_format", PythonString.class, PythonString.class)));

        DATE_TIME_TYPE.addMethod("strftime",
                ArgumentSpec.forFunctionReturning("strftime", PythonString.class.getName())
                        .addArgument("format", PythonString.class.getName())
                        .asPythonFunctionSignature(PythonDateTime.class.getMethod("strftime", PythonString.class)));

        DATE_TIME_TYPE.addMethod("ctime",
                PythonDateTime.class.getMethod("ctime"));

        // The following virtual methods are inherited from date:
        // toordinal, weekday, isoweekday, isocalendar
    }

    final Temporal dateTime;
    final ZoneId zoneId;

    public final PythonInteger hour;
    public final PythonInteger minute;
    public final PythonInteger second;
    public final PythonInteger microsecond;
    public final PythonInteger fold;
    public final PythonLikeObject tzinfo;

    public PythonDateTime(ZonedDateTime zonedDateTime) {
        this(zonedDateTime.toLocalDate(), zonedDateTime.toLocalTime(), zonedDateTime.getZone(),
                zonedDateTime.equals(zonedDateTime.withEarlierOffsetAtOverlap()) ? 0 : 1);
    }

    public PythonDateTime(LocalDateTime localDateTime) {
        this(localDateTime.toLocalDate(), localDateTime.toLocalTime(), (ZoneId) null, 0);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime) {
        this(localDate, localTime, (ZoneId) null, 0);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime, ZoneId zoneId) {
        this(localDate, localTime, zoneId, 0);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime, PythonLikeObject tzinfo, int fold) {
        this(localDate, localTime,
                (tzinfo instanceof PythonTzinfo) ? ((PythonTzinfo) tzinfo).zoneId : null,
                fold);
    }

    public PythonDateTime(LocalDate localDate, LocalTime localTime, ZoneId zoneId, int fold) {
        super(DATE_TIME_TYPE, localDate);

        this.zoneId = zoneId;
        if (zoneId == null) {
            dateTime = LocalDateTime.of(localDate, localTime);
        } else {
            dateTime = ZonedDateTime.of(localDate, localTime, zoneId);
        }

        hour = PythonInteger.valueOf(localTime.getHour());
        minute = PythonInteger.valueOf(localTime.getMinute());
        second = PythonInteger.valueOf(localTime.getSecond());
        microsecond = PythonInteger.valueOf(localTime.getNano() / 1000); // Micro = Nano // 1000
        tzinfo = zoneId == null ? PythonNone.INSTANCE : new PythonTzinfo(zoneId);
        this.fold = PythonInteger.valueOf(fold);
    }

    public static PythonDateTime of(PythonInteger year, PythonInteger month, PythonInteger day, PythonInteger hour,
            PythonInteger minute, PythonInteger second,
            PythonInteger microsecond, PythonLikeObject tzinfo, PythonInteger fold) {
        if (month.value.intValueExact() < 1 || month.value.intValueExact() > 12) {
            throw new ValueError("month must be between 1 and 12");
        }
        if (!YearMonth.of(year.value.intValueExact(), month.value.intValueExact()).isValidDay(day.value.intValueExact())) {
            throw new ValueError("day must be between 1 and "
                    + YearMonth.of(year.value.intValueExact(), month.value.intValueExact()).lengthOfMonth());
        }
        if (hour.value.intValueExact() < 0 || hour.value.intValueExact() >= 24) {
            throw new ValueError("hour must be in range 0 <= hour < 24");
        }
        if (minute.value.intValueExact() < 0 || minute.value.intValueExact() >= 60) {
            throw new ValueError("minute must be in range 0 <= minute < 60");
        }
        if (second.value.intValueExact() < 0 || second.value.intValueExact() >= 60) {
            throw new ValueError("second must be in range 0 <= second < 60");
        }
        if (microsecond.value.intValueExact() < 0 || microsecond.value.intValueExact() >= 1000000) {
            throw new ValueError("microsecond must be in range 0 <= microsecond < 1000000");
        }
        if (fold.value.intValueExact() != 0 && fold.value.intValueExact() != 1) {
            throw new ValueError("fold must be in [0, 1]");
        }

        return new PythonDateTime(
                LocalDate.of(year.value.intValueExact(), month.value.intValueExact(), day.value.intValueExact()),
                LocalTime.of(hour.value.intValueExact(), minute.value.intValueExact(), second.value.intValueExact(),
                        microsecond.value.intValueExact() * 1000),
                (tzinfo != PythonNone.INSTANCE) ? ((PythonTzinfo) tzinfo).zoneId : null, fold.value.intValueExact());
    }

    public static PythonDateTime of(int year, int month, int day, int hour, int minute, int second,
            int microsecond, String tzname, int fold) {
        return new PythonDateTime(LocalDate.of(year, month, day), LocalTime.of(hour, minute, second, microsecond * 1000),
                (tzname != null) ? ZoneId.of(tzname) : null, fold);
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String name) {
        switch (name) {
            case "hour":
                return hour;
            case "minute":
                return minute;
            case "second":
                return second;
            case "microsecond":
                return microsecond;
            case "fold":
                return fold;
            case "tzinfo":
                return tzinfo;
            default:
                return super.$getAttributeOrNull(name);
        }
    }

    public static PythonDateTime now(PythonLikeType type, PythonLikeObject tzinfo) {
        if (type != DATE_TIME_TYPE) {
            throw new TypeError("Unknown datetime type: " + type);
        }
        LocalDateTime result = LocalDateTime.now();
        return new PythonDateTime(result.toLocalDate(), result.toLocalTime(),
                tzinfo == PythonNone.INSTANCE ? null : ((PythonTzinfo) tzinfo).zoneId);
    }

    public static PythonDateTime utc_now(PythonLikeType type) {
        if (type != DATE_TIME_TYPE) {
            throw new TypeError("Unknown datetime type: " + type);
        }
        LocalDateTime result = LocalDateTime.now(Clock.systemUTC());
        return new PythonDateTime(result.toLocalDate(), result.toLocalTime(),
                null);
    }

    public static PythonDateTime from_ordinal(PythonInteger ordinal) {
        return new PythonDateTime(LocalDate.ofEpochDay(ordinal.getValue().longValue() - EPOCH_ORDINAL_OFFSET),
                LocalTime.MIDNIGHT, null);
    }

    public static PythonDateTime from_timestamp(PythonLikeType type, PythonNumber timestamp, PythonLikeObject tzinfo) {
        if (type != DATE_TIME_TYPE) {
            throw new TypeError("Unknown datetime type: " + type);
        }
        if (timestamp instanceof PythonInteger) {
            return from_timestamp((PythonInteger) timestamp, tzinfo);
        } else {
            return from_timestamp((PythonFloat) timestamp, tzinfo);
        }
    }

    public static PythonDateTime from_timestamp(PythonInteger timestamp, PythonLikeObject tzinfo) {
        Instant instant = Instant.ofEpochSecond(timestamp.getValue().longValue());
        if (tzinfo == PythonNone.INSTANCE) {
            LocalDateTime result = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            return new PythonDateTime(result);
        } else {
            ZoneId zoneId = ((PythonTzinfo) tzinfo).zoneId;
            LocalDateTime result = instant.atZone(zoneId).toLocalDateTime();
            return new PythonDateTime(result.toLocalDate(), result.toLocalTime(), zoneId);
        }
    }

    public static PythonDateTime from_timestamp(PythonFloat timestamp, PythonLikeObject tzinfo) {
        long epochSeconds = (long) Math.floor(timestamp.getValue().doubleValue());
        double remainder = timestamp.getValue().doubleValue() - epochSeconds;
        int nanos = (int) Math.round(remainder * NANOS_PER_SECOND);
        Instant instant = Instant.ofEpochSecond(timestamp.getValue().longValue(), nanos);

        if (tzinfo == PythonNone.INSTANCE) {
            LocalDateTime result = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            return new PythonDateTime(result);
        } else {
            ZoneId zoneId = ((PythonTzinfo) tzinfo).zoneId;
            LocalDateTime result = instant.atZone(zoneId).toLocalDateTime();
            return new PythonDateTime(result.toLocalDate(), result.toLocalTime(), zoneId);
        }
    }

    public static PythonDateTime utc_from_timestamp(PythonLikeType type, PythonNumber timestamp) {
        if (type != DATE_TIME_TYPE) {
            throw new TypeError("Unknown datetime type: " + type);
        }
        if (timestamp instanceof PythonInteger) {
            return utc_from_timestamp((PythonInteger) timestamp);
        } else {
            return utc_from_timestamp((PythonFloat) timestamp);
        }
    }

    public static PythonDateTime utc_from_timestamp(PythonInteger timestamp) {
        return new PythonDateTime(LocalDateTime.ofEpochSecond(timestamp.getValue().longValue(), 0,
                ZoneOffset.UTC));
    }

    public static PythonDateTime utc_from_timestamp(PythonFloat timestamp) {
        long epochSeconds = (long) Math.floor(timestamp.getValue().doubleValue());
        double remainder = timestamp.getValue().doubleValue() - epochSeconds;
        int nanos = (int) Math.round(remainder * 1_000_000_000);
        return new PythonDateTime(LocalDateTime.ofEpochSecond(timestamp.getValue().longValue(), nanos,
                ZoneOffset.UTC));
    }

    public static PythonDateTime combine(PythonLikeType type,
            PythonDate pythonDate, PythonTime pythonTime,
            PythonLikeObject tzinfo) {
        if (type != DATE_TIME_TYPE) {
            throw new TypeError("Unknown datetime type " + type.getTypeName());
        }
        if (tzinfo == null) {
            tzinfo = pythonTime.tzinfo;
        }
        return new PythonDateTime(pythonDate.localDate, pythonTime.localTime, tzinfo,
                pythonTime.fold.getValue().intValue());
    }

    public static PythonDateTime from_iso_format(PythonString dateString) {
        Matcher matcher = ISO_FORMAT_PATTERN.matcher(dateString.getValue());
        if (!matcher.find()) {
            throw new IllegalArgumentException("String \"" + dateString.getValue() + "\" is not an isoformat string");
        }

        String year = matcher.group("year");
        String month = matcher.group("month");
        String day = matcher.group("day");

        String hour = matcher.group("hour");
        String minute = matcher.group("minute");
        String second = matcher.group("second");
        String microHigh = matcher.group("microHigh");
        String microLow = matcher.group("microLow");

        String timezoneHour = matcher.group("timezoneHour");
        String timezoneMinute = matcher.group("timezoneMinute");
        String timezoneSecond = matcher.group("timezoneSecond");
        String timezoneMicro = matcher.group("timezoneMicro");

        LocalDate date = LocalDate.of(Integer.parseInt(year),
                Integer.parseInt(month),
                Integer.parseInt(day));

        int hoursPart = 0;
        int minutePart = 0;
        int secondPart = 0;
        int microPart = 0;

        if (hour != null) {
            hoursPart = Integer.parseInt(hour);
        }
        if (minute != null) {
            minutePart = Integer.parseInt(minute);
        }
        if (second != null) {
            secondPart = Integer.parseInt(second);
        }
        if (microHigh != null) {
            if (microLow != null) {
                microPart = Integer.parseInt(microHigh + microLow);
            } else {
                microPart = 1000 * Integer.parseInt(microHigh);
            }
        }

        LocalTime time = LocalTime.of(hoursPart, minutePart, secondPart, microPart * 1000);

        if (timezoneHour == null) {
            return new PythonDateTime(date, time);
        }

        int timezoneHourPart = Integer.parseInt(timezoneHour);
        int timezoneMinutePart = Integer.parseInt(timezoneMinute);
        int timezoneSecondPart = 0;
        int timezoneMicroPart = 0;

        if (timezoneSecond != null) {
            timezoneSecondPart = Integer.parseInt(timezoneSecond);
        }

        if (timezoneMicro != null) {
            timezoneMicroPart = Integer.parseInt(timezoneMicro);
        }

        // TODO: ZoneOffset does not support nanos
        ZoneOffset timezone = ZoneOffset.ofHoursMinutesSeconds(timezoneHourPart, timezoneMinutePart, timezoneSecondPart);
        return new PythonDateTime(date, time, timezone);
    }

    public static PythonDate from_iso_calendar(PythonInteger year, PythonInteger week, PythonInteger day) {
        int isoYear = year.getValue().intValue();
        int dayInIsoYear = (week.getValue().intValue() * 7) + day.getValue().intValue();
        int correction = LocalDate.of(isoYear, 1, 4).getDayOfWeek().getValue() + 3;
        int ordinalDate = dayInIsoYear - correction;
        if (ordinalDate <= 0) {
            int daysInYear = LocalDate.ofYearDay(isoYear - 1, 1).lengthOfYear();
            return new PythonDateTime(LocalDate.ofYearDay(isoYear - 1, ordinalDate + daysInYear), LocalTime.MIN);
        } else if (ordinalDate > LocalDate.ofYearDay(isoYear, 1).lengthOfYear()) {
            int daysInYear = LocalDate.ofYearDay(isoYear, 1).lengthOfYear();
            return new PythonDateTime(LocalDate.ofYearDay(isoYear + 1, ordinalDate - daysInYear), LocalTime.MIN);
        } else {
            return new PythonDateTime(LocalDate.ofYearDay(isoYear, ordinalDate), LocalTime.MIN);
        }
    }

    private static <T> T tryParseOrNull(DateTimeFormatter formatter, String text, TemporalQuery<T> query) {
        try {
            return formatter.parse(text, query);
        } catch (DateTimeException e) {
            return null;
        }
    }

    public static PythonDateTime strptime(PythonLikeType type, PythonString date_string, PythonString format) {
        if (type != DATE_TIME_TYPE) {
            throw new TypeError("Unknown datetime type (" + type + ").");
        }
        var formatter = PythonDateTimeFormatter.getDateTimeFormatter(format.value);
        var asZonedDateTime = tryParseOrNull(formatter, date_string.value, ZonedDateTime::from);
        if (asZonedDateTime != null) {
            return new PythonDateTime(asZonedDateTime);
        }
        var asLocalDateTime = tryParseOrNull(formatter, date_string.value, LocalDateTime::from);
        if (asLocalDateTime != null) {
            return new PythonDateTime(asLocalDateTime);
        }
        var asLocalDate = tryParseOrNull(formatter, date_string.value, LocalDate::from);
        if (asLocalDate != null) {
            return new PythonDateTime(asLocalDate.atTime(LocalTime.MIDNIGHT));
        }
        var asLocalTime = tryParseOrNull(formatter, date_string.value, LocalTime::from);
        if (asLocalTime != null) {
            return new PythonDateTime(asLocalTime.atDate(LocalDate.of(1900, 1, 1)));
        }
        throw new ValueError("data " + date_string.repr() + " does not match the format " + format.repr());
    }

    public PythonDateTime add_time_delta(PythonTimeDelta summand) {
        if (dateTime instanceof LocalDateTime) {
            return new PythonDateTime(((LocalDateTime) dateTime).plus(summand.duration));
        } else {
            return new PythonDateTime(((ZonedDateTime) dateTime).plus(summand.duration));
        }
    }

    public PythonDateTime subtract_time_delta(PythonTimeDelta subtrahend) {
        if (dateTime instanceof LocalDateTime) {
            return new PythonDateTime(((LocalDateTime) dateTime).minus(subtrahend.duration));
        } else {
            return new PythonDateTime(((ZonedDateTime) dateTime).minus(subtrahend.duration));
        }
    }

    public PythonTimeDelta subtract_date_time(PythonDateTime subtrahend) {
        return new PythonTimeDelta(Duration.between(subtrahend.dateTime, dateTime));
    }

    @Override
    public int compareTo(PythonDateTime other) {
        if (dateTime instanceof LocalDateTime) {
            return ((LocalDateTime) dateTime).compareTo((LocalDateTime) other.dateTime);
        } else {
            return ((ZonedDateTime) dateTime).compareTo((ZonedDateTime) other.dateTime);
        }
    }

    public PythonDate<PythonDate<?>> date() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonDate<>(((LocalDateTime) dateTime).toLocalDate());
        } else {
            return new PythonDate<>(((ZonedDateTime) dateTime).toLocalDate());
        }
    }

    public PythonTime time() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonTime(((LocalDateTime) dateTime).toLocalTime(), null, fold.getValue().intValue());
        } else {
            return new PythonTime(((ZonedDateTime) dateTime).toLocalTime(), null, fold.getValue().intValue());
        }
    }

    public PythonTime timetz() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonTime(((LocalDateTime) dateTime).toLocalTime(), null, fold.getValue().intValue());
        } else {
            ZonedDateTime zonedDateTime = (ZonedDateTime) dateTime;
            return new PythonTime(zonedDateTime.toLocalTime(), zonedDateTime.getZone(), fold.getValue().intValue());
        }
    }

    public PythonDateTime replace(PythonInteger year, PythonInteger month, PythonInteger day,
            PythonInteger hour, PythonInteger minute, PythonInteger second,
            PythonInteger microsecond, PythonLikeObject tzinfo, PythonInteger fold) {
        if (year == null) {
            year = this.year;
        }

        if (month == null) {
            month = this.month;
        }

        if (day == null) {
            day = this.day;
        }

        if (hour == null) {
            hour = this.hour;
        }

        if (minute == null) {
            minute = this.minute;
        }

        if (second == null) {
            second = this.second;
        }

        if (microsecond == null) {
            microsecond = this.microsecond;
        }

        if (tzinfo == null) {
            tzinfo = this.tzinfo;
        }

        if (fold == null) {
            fold = this.fold;
        }

        return new PythonDateTime(LocalDate.of(year.getValue().intValue(),
                month.getValue().intValue(),
                day.getValue().intValue()),
                LocalTime.of(hour.getValue().intValue(),
                        minute.getValue().intValue(),
                        second.getValue().intValue(),
                        microsecond.getValue().intValue() * 1000),
                tzinfo,
                fold.getValue().intValue());
    }

    public PythonDateTime astimezone(PythonTzinfo zoneId) {
        throw new UnsupportedOperationException(); // TODO
    }

    public PythonLikeObject utcoffset() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(Duration.ofSeconds(
                zoneId.getRules().getOffset(((ZonedDateTime) dateTime).toInstant()).getTotalSeconds()));
    }

    public PythonLikeObject dst() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(zoneId.getRules().getDaylightSavings(((ZonedDateTime) dateTime).toInstant()));
    }

    public PythonLikeObject tzname() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return PythonString.valueOf(zoneId.getRules().getOffset(((ZonedDateTime) dateTime).toInstant())
                .getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
    }

    @Override
    public PythonLikeTuple timetuple() {
        PythonInteger yday =
                to_ordinal().subtract(PythonDate.of(year.value.intValueExact(), 1, 1).to_ordinal()).add(PythonInteger.ONE);
        PythonInteger dst;
        if (zoneId != null) {
            dst = zoneId.getRules().isDaylightSavings(((ZonedDateTime) dateTime).toInstant()) ? PythonInteger.ONE
                    : PythonInteger.ZERO;
        } else {
            dst = PythonInteger.valueOf(-1);
        }
        return PythonLikeTuple.fromItems(
                year, month, day,
                hour, minute, second,
                weekday(), yday, dst);
    }

    public PythonLikeTuple utctimetuple() {
        if (zoneId == null) {
            return timetuple();
        } else {
            ZonedDateTime utcDateTime = ((ZonedDateTime) dateTime).withZoneSameInstant(ZoneOffset.UTC);
            return new PythonDateTime(utcDateTime.toLocalDateTime()).timetuple();
        }
    }

    public PythonFloat timestamp() {
        if (dateTime instanceof LocalDateTime) {
            LocalDateTime localDateTime = (LocalDateTime) dateTime;
            return PythonFloat.valueOf(localDateTime.toInstant(ZoneId.systemDefault()
                    .getRules()
                    .getOffset(localDateTime))
                    .toEpochMilli() / 1000.0);
        } else {
            return PythonFloat.valueOf(((ZonedDateTime) dateTime).toInstant().toEpochMilli() / 1000.0);
        }
    }

    public PythonString iso_format() {
        return iso_format(PythonString.valueOf("T"), PythonString.valueOf("auto"));
    }

    public PythonString iso_format(PythonString sep, PythonString timespec) {
        return new PythonString(localDate.toString() + sep.value + time().isoformat(timespec).value);
    }

    @Override
    public PythonString toPythonString() {
        return iso_format(PythonString.valueOf(" "), PythonString.valueOf("auto"));
    }

    @Override
    public PythonString ctime() {
        if (dateTime instanceof LocalDateTime) {
            return new PythonString(((LocalDateTime) dateTime).format(C_TIME_FORMATTER).replaceAll("(\\D)\\.", "$1"));
        } else {
            return new PythonString(((ZonedDateTime) dateTime).format(C_TIME_FORMATTER).replaceAll("(\\D)\\.", "$1"));
        }
    }

    @Override
    public PythonString strftime(PythonString format) {
        var formatter = PythonDateTimeFormatter.getDateTimeFormatter(format.value);
        return PythonString.valueOf(formatter.format(dateTime));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PythonDateTime that = (PythonDateTime) o;
        return dateTime.equals(that.dateTime);
    }

    @Override
    public int hashCode() {
        return dateTime.hashCode();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
