package ai.timefold.solver.benchmark.impl.statistic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementations must be immutable.
 */
public abstract class StatisticPoint {

    private static final Pattern DOUBLE_QUOTE = Pattern.compile("\"\"");
    private static final Pattern SINGLE_QUOTE = Pattern.compile("\"");

    public abstract String toCsvLine();

    public static String buildCsvLineWithLongs(long timeMillisSpent, long... values) {
        Object[] array = Stream.concat(Stream.of(timeMillisSpent), Arrays.stream(values).boxed())
                .toArray(Object[]::new);
        return buildCsvLine(array);
    }

    public static String buildCsvLineWithDoubles(long timeMillisSpent, double... values) {
        Object[] array = Stream.concat(Stream.of(timeMillisSpent), Arrays.stream(values).boxed())
                .toArray(Object[]::new);
        return buildCsvLine(array);
    }

    public static String buildCsvLineWithStrings(long timeMillisSpent, String... values) {
        Object[] array = Stream.concat(Stream.of(timeMillisSpent), Arrays.stream(values))
                .toArray(Object[]::new);
        return buildCsvLine(array);
    }

    public static String buildCsvLine(Object... values) {
        return Arrays.stream(values)
                .map(s -> {
                    if (s instanceof Boolean bool) {
                        return bool ? "true" : "false";
                    } else if (s instanceof Integer num) {
                        return Integer.toString(num);
                    } else if (s instanceof Long num) {
                        return Long.toString(num);
                    } else {
                        return '"' + SINGLE_QUOTE.matcher(s.toString()).replaceAll("\"\"") + '"';
                    }
                })
                .collect(Collectors.joining(","));
    }

    public static List<String> parseCsvLine(String line) {
        String[] tokens = line.split(",");
        List<String> csvLine = new ArrayList<>(tokens.length);
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            while (token.startsWith("\"") && !token.endsWith("\"")) {
                i++;
                if (i >= tokens.length) {
                    throw new IllegalArgumentException("The CSV line (" + line + ") is not a valid CSV line.");
                }
                token += "," + tokens[i];
            }
            if (token.startsWith("\"") && token.endsWith("\"")) {
                token = token.substring(1, token.length() - 1);
                token = DOUBLE_QUOTE.matcher(token).replaceAll("\"");
            }
            csvLine.add(token);
        }
        return csvLine;
    }

}
