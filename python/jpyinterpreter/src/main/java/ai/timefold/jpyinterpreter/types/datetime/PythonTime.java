package ai.timefold.jpyinterpreter.types.datetime;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.ValueError;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class PythonTime extends AbstractPythonLikeObject implements PlanningImmutable {
    // Taken from https://docs.python.org/3/library/datetime.html#datetime.time.fromisoformat
    private static final Pattern ISO_FORMAT_PATTERN = Pattern.compile("^(?<hour>\\d\\d)" +
            "(:(?<minute>\\d\\d)" +
            "(:(?<second>\\d\\d)" +
            "(\\.(?<microHigh>\\d\\d\\d)" +
            "(?<microLow>\\d\\d\\d)?" +
            ")?)?)?" +
            "(\\+(?<timezoneHour>\\d\\d):(?<timezoneMinute>\\d\\d)" +
            "(:(?<timezoneSecond>\\d\\d)" +
            "(:\\.(?<timezoneMicro>\\d\\d\\d\\d\\d\\d)" +
            ")?)?)?$");
    public static PythonLikeType TIME_TYPE = new PythonLikeType("time",
            PythonTime.class);

    public static PythonLikeType $TYPE = TIME_TYPE;

    static {
        try {
            registerMethods();

            TIME_TYPE.$setAttribute("min", new PythonTime(LocalTime.MAX));
            TIME_TYPE.$setAttribute("max", new PythonTime(LocalTime.MIN));
            TIME_TYPE.$setAttribute("resolution", new PythonTimeDelta(Duration.ofNanos(1000L)));

            PythonOverloadImplementor.createDispatchesFor(TIME_TYPE);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerMethods() throws NoSuchMethodException {
        TIME_TYPE.addConstructor(ArgumentSpec.forFunctionReturning("datetime.time", PythonTime.class.getName())
                .addArgument("hour", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("minute", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("second", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("microsecond", PythonInteger.class.getName(), PythonInteger.ZERO)
                .addArgument("tzinfo", PythonLikeObject.class.getName(), PythonNone.INSTANCE)
                .addKeywordOnlyArgument("fold", PythonInteger.class.getName(), PythonInteger.ZERO)
                .asPythonFunctionSignature(
                        PythonTime.class.getMethod("of", PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonInteger.class, PythonLikeObject.class,
                                PythonInteger.class)));

        TIME_TYPE.addMethod("fromisoformat",
                ArgumentSpec.forFunctionReturning("fromisoformat", PythonTime.class.getName())
                        .addArgument("time_string", PythonString.class.getName())
                        .asStaticPythonFunctionSignature(PythonTime.class.getMethod("from_iso_format", PythonString.class)));

        TIME_TYPE.addMethod("replace",
                ArgumentSpec.forFunctionReturning("replace", PythonTime.class.getName())
                        .addNullableArgument("hour", PythonInteger.class.getName())
                        .addNullableArgument("minute", PythonInteger.class.getName())
                        .addNullableArgument("second", PythonInteger.class.getName())
                        .addNullableArgument("microsecond", PythonInteger.class.getName())
                        .addNullableArgument("tzinfo", PythonLikeObject.class.getName())
                        .addNullableKeywordOnlyArgument("fold", PythonInteger.class.getName())
                        .asPythonFunctionSignature(PythonTime.class.getMethod("replace", PythonInteger.class,
                                PythonInteger.class, PythonInteger.class,
                                PythonInteger.class, PythonLikeObject.class,
                                PythonInteger.class)));

        TIME_TYPE.addMethod("isoformat",
                ArgumentSpec.forFunctionReturning("isoformat", PythonString.class.getName())
                        .addArgument("timespec", PythonString.class.getName(), PythonString.valueOf("auto"))
                        .asPythonFunctionSignature(PythonTime.class.getMethod("isoformat", PythonString.class)));

        TIME_TYPE.addMethod("strftime",
                ArgumentSpec.forFunctionReturning("strftime", PythonString.class.getName())
                        .addArgument("format", PythonString.class.getName())
                        .asPythonFunctionSignature(PythonTime.class.getMethod("strftime", PythonString.class)));

        TIME_TYPE.addMethod("tzname",
                PythonTime.class.getMethod("tzname"));

        TIME_TYPE.addMethod("utcoffset",
                PythonTime.class.getMethod("utcoffset"));

        TIME_TYPE.addMethod("dst",
                PythonTime.class.getMethod("dst"));

    }

    final LocalTime localTime;
    final ZoneId zoneId;

    public final PythonInteger hour;
    public final PythonInteger minute;
    public final PythonInteger second;
    public final PythonInteger microsecond;
    public final PythonInteger fold;
    public final PythonLikeObject tzinfo;

    public PythonTime(LocalTime localTime) {
        this(localTime, null, 0);
    }

    public PythonTime(LocalTime localTime, ZoneId zoneId) {
        this(localTime, zoneId, 0);
    }

    public PythonTime(LocalTime localTime, ZoneId zoneId, int fold) {
        super(TIME_TYPE);

        this.localTime = localTime;
        this.zoneId = zoneId;

        hour = PythonInteger.valueOf(localTime.getHour());
        minute = PythonInteger.valueOf(localTime.getMinute());
        second = PythonInteger.valueOf(localTime.getSecond());
        microsecond = PythonInteger.valueOf(localTime.getNano() / 1000); // Micro = Nano // 1000
        tzinfo = zoneId == null ? PythonNone.INSTANCE : new PythonTzinfo(zoneId);
        this.fold = PythonInteger.valueOf(fold);
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
            case "tzinfo":
                return tzinfo;
            case "fold":
                return fold;
            default:
                return super.$getAttributeOrNull(name);
        }
    }

    public static PythonTime of(PythonInteger hour, PythonInteger minute, PythonInteger second, PythonInteger microsecond,
            PythonLikeObject tzinfo, PythonInteger fold) {
        return of(hour.value.intValueExact(), minute.value.intValueExact(), second.value.intValueExact(),
                microsecond.value.intValueExact(), (tzinfo == PythonNone.INSTANCE) ? null : ((PythonTzinfo) tzinfo).zoneId,
                fold.value.intValueExact());
    }

    public static PythonTime of(int hour, int minute, int second, int microsecond, ZoneId zoneId, int fold) {
        if (hour < 0 || hour >= 24) {
            throw new ValueError("hour must be in range 0 <= hour < 24");
        }
        if (minute < 0 || minute >= 60) {
            throw new ValueError("minute must be in range 0 <= minute < 60");
        }
        if (second < 0 || second >= 60) {
            throw new ValueError("second must be in range 0 <= second < 60");
        }
        if (microsecond < 0 || microsecond >= 1000000) {
            throw new ValueError("microsecond must be in range 0 <= microsecond < 1000000");
        }
        if (fold != 0 && fold != 1) {
            throw new ValueError("fold must be in [0, 1]");
        }
        return new PythonTime(LocalTime.of(hour, minute, second, microsecond * 1000),
                zoneId, fold);
    }

    public static PythonTime from_iso_format(PythonString dateString) {
        Matcher matcher = ISO_FORMAT_PATTERN.matcher(dateString.getValue());
        if (!matcher.find()) {
            throw new ValueError("String \"" + dateString.getValue() + "\" is not an isoformat string");
        }

        String hour = matcher.group("hour");
        String minute = matcher.group("minute");
        String second = matcher.group("second");
        String microHigh = matcher.group("microHigh");
        String microLow = matcher.group("microLow");

        String timezoneHour = matcher.group("timezoneHour");
        String timezoneMinute = matcher.group("timezoneMinute");
        String timezoneSecond = matcher.group("timezoneSecond");
        String timezoneMicro = matcher.group("timezoneMicro");

        int hoursPart = 0;
        int minutePart = 0;
        int secondPart = 0;
        int microPart = 0;

        if (hour != null && !hour.isEmpty()) {
            hoursPart = Integer.parseInt(hour);
        }
        if (minute != null && !minute.isEmpty()) {
            minutePart = Integer.parseInt(minute);
        }
        if (second != null && !second.isEmpty()) {
            secondPart = Integer.parseInt(second);
        }
        if (microHigh != null && !microHigh.isEmpty()) {
            if (microLow != null && !microLow.isEmpty()) {
                microPart = Integer.parseInt(microHigh + microLow);
            } else {
                microPart = 1000 * Integer.parseInt(microHigh);
            }
        }

        LocalTime time = LocalTime.of(hoursPart, minutePart, secondPart, microPart * 1000);

        if (timezoneHour == null || timezoneHour.isEmpty()) {
            return new PythonTime(time);
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
        return new PythonTime(time, timezone);
    }

    public PythonTime replace(PythonInteger hour, PythonInteger minute, PythonInteger second,
            PythonInteger microsecond, PythonLikeObject tzinfo, PythonInteger fold) {
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
            tzinfo = (zoneId != null) ? new PythonTzinfo(zoneId) : PythonNone.INSTANCE;
        }

        if (fold == null) {
            fold = this.fold;
        }

        return of(hour, minute, second, microsecond, tzinfo, fold);
    }

    public PythonLikeObject utcoffset() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(Duration.ofSeconds(zoneId.getRules().getOffset(Instant.ofEpochMilli(0L)).getTotalSeconds()));
    }

    public PythonLikeObject dst() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return new PythonTimeDelta(zoneId.getRules().getDaylightSavings(Instant.ofEpochMilli(0L)));
    }

    public PythonLikeObject tzname() {
        if (zoneId == null) {
            return PythonNone.INSTANCE;
        }
        return PythonString.valueOf(zoneId.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()));
    }

    public PythonString isoformat(PythonString formatSpec) {
        final String result;
        switch (formatSpec.value) {
            case "auto":
                if (microsecond.value.equals(BigInteger.ZERO)) {
                    result = String.format("%02d:%02d:%02d", localTime.getHour(), localTime.getMinute(), localTime.getSecond());
                } else {
                    result = String.format("%02d:%02d:%02d.%06d", localTime.getHour(), localTime.getMinute(),
                            localTime.getSecond(),
                            localTime.get(ChronoField.MICRO_OF_SECOND));
                }
                break;
            case "hours":
                result = String.format("%02d", localTime.getHour());
                break;
            case "minutes":
                result = String.format("%02d:%02d", localTime.getHour(), localTime.getMinute());
                break;
            case "seconds":
                result = String.format("%02d:%02d:%02d", localTime.getHour(), localTime.getMinute(), localTime.getSecond());
                break;
            case "milliseconds":
                result = String.format("%02d:%02d:%02d.%03d", localTime.getHour(), localTime.getMinute(), localTime.getSecond(),
                        localTime.get(ChronoField.MILLI_OF_SECOND));
                break;
            case "microseconds":
                result = String.format("%02d:%02d:%02d.%06d", localTime.getHour(), localTime.getMinute(), localTime.getSecond(),
                        localTime.get(ChronoField.MICRO_OF_SECOND));
                break;
            default:
                throw new ValueError("Invalid timespec: " + formatSpec.repr());
        }
        return PythonString.valueOf(result);
    }

    public PythonString strftime(PythonString formatSpec) {
        var formatter = PythonDateTimeFormatter.getDateTimeFormatter(formatSpec.value);
        return PythonString.valueOf(formatter.format(localTime));
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        return localTime.toString();
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }
}
