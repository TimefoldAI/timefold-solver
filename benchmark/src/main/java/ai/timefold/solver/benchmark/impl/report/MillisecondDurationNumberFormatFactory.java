package ai.timefold.solver.benchmark.impl.report;

import java.time.Duration;
import java.util.Locale;

import freemarker.core.Environment;
import freemarker.core.TemplateFormatUtil;
import freemarker.core.TemplateNumberFormat;
import freemarker.core.TemplateNumberFormatFactory;
import freemarker.core.TemplateValueFormatException;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateNumberModel;

public final class MillisecondDurationNumberFormatFactory extends TemplateNumberFormatFactory {

    static final MillisecondDurationNumberFormatFactory INSTANCE = new MillisecondDurationNumberFormatFactory();

    @Override
    public TemplateNumberFormat get(String params, Locale locale, Environment environment) throws TemplateValueFormatException {
        TemplateFormatUtil.checkHasNoParameters(params);
        return new MillisecondDurationNumberFormat(locale);
    }

    static final class MillisecondDurationNumberFormat extends TemplateNumberFormat {

        private final Locale locale;

        public MillisecondDurationNumberFormat(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String formatToPlainText(TemplateNumberModel templateNumberModel) throws TemplateModelException {
            Number n = templateNumberModel.getAsNumber();
            if (n == null) {
                return "None.";
            }
            long millis = n.longValue();
            return formatMillis(locale, millis);
        }

        @Override
        public boolean isLocaleBound() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Millisecond Duration";
        }
    }

    public static String formatMillis(Locale locale, long millis) {
        if (millis == 0L) {
            return "0 ms.";
        }
        Duration duration = Duration.ofMillis(millis);
        long daysPart = duration.toDaysPart();
        long hoursPart = duration.toHoursPart();
        long minutesPart = duration.toMinutesPart();
        double seconds = duration.toSecondsPart() + (duration.toMillisPart() / 1000.0d);
        if (daysPart > 0) {
            return String.format(locale, "%02d:%02d:%02d:%06.3f s. (%,d ms.)",
                    daysPart,
                    hoursPart,
                    minutesPart,
                    seconds,
                    millis);
        } else if (hoursPart > 0) {
            return String.format(locale, "%02d:%02d:%06.3f s. (%,d ms.)",
                    hoursPart,
                    minutesPart,
                    seconds,
                    millis);
        } else if (minutesPart > 0) {
            return String.format(locale, "%02d:%06.3f s. (%,d ms.)",
                    minutesPart,
                    seconds,
                    millis);
        } else {
            return String.format(locale, "%.3f s. (%,d ms.)",
                    seconds,
                    millis);
        }
    }

}
