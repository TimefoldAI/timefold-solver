package ai.timefold.solver.benchmark.impl.report;

import static ai.timefold.solver.benchmark.impl.report.BenchmarkReport.LOG_SCALE_MIN_DATASETS_COUNT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.util.Pair;

import freemarker.template.TemplateException;

public record LineChart<X extends Number & Comparable<X>, Y extends Number & Comparable<Y>>(String id, String title,
        String xLabel, String yLabel, List<X> keys, List<Dataset<Y>> datasets, boolean stepped, boolean timeOnX,
        boolean timeOnY)
        implements
            Chart {

    public LineChart {
        id = Chart.makeIdUnique(id);
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal xMin() {
        return min(keys);
    }

    static <Number_ extends Number & Comparable<Number_>> BigDecimal min(List<Number_> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var min = Collections.min(values).doubleValue();
        if (min > 0) { // Always start with zero.
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(min);
        }
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal xMax() {
        return max(keys);
    }

    static <Number_ extends Number & Comparable<Number_>> BigDecimal max(List<Number_> values) {
        if (values.isEmpty()) {
            return BigDecimal.ZERO;
        }
        var max = Collections.max(values).doubleValue();
        if (max < 0) { // Always start with zero.
            return BigDecimal.ZERO;
        } else {
            return BigDecimal.valueOf(max);
        }
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal yMin() {
        return min(getYValues());
    }

    private List<Y> getYValues() {
        return datasets.stream()
                .flatMap(d -> d.data().stream())
                .filter(Objects::nonNull)
                .toList();
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal yMax() {
        return max(getYValues());
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal xStepSize() {
        return stepSize(xMin(), xMax());
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal yStepSize() {
        return stepSize(yMin(), yMax());
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public boolean xLogarithmic() {
        if (timeOnX) { // Logarithmic time doesn't make sense.
            return false;
        }
        return useLogarithmicProblemScale(keys);
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public boolean yLogarithmic() {
        if (timeOnY) { // Logarithmic time doesn't make sense.
            return false;
        }
        return useLogarithmicProblemScale(getYValues());
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public List<Pair<X, Y>> points(String label) {
        var dataset = datasets().stream().filter(d -> d.label().equals(label)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Dataset %s not found.".formatted(label)));
        return IntStream.range(0, dataset.data().size())
                .filter(i -> dataset.data().get(i) != null)
                .mapToObj(i -> new Pair<>(keys().get(i), dataset.data().get(i)))
                .toList();
    }

    static <N extends Number & Comparable<N>> boolean useLogarithmicProblemScale(List<N> seriesList) {
        NavigableSet<Double> valueSet = new TreeSet<>();
        for (var dataItem : seriesList) {
            var value = dataItem.doubleValue();
            if (value <= 0) { // Logarithm undefined.
                return false;
            }
            valueSet.add(value);
        }
        if (valueSet.size() < LOG_SCALE_MIN_DATASETS_COUNT) {
            return false;
        }
        // If 60% of the points are in 20% of the value space, use a logarithmic scale.
        var threshold = 0.2 * (valueSet.last() - valueSet.first());
        var belowThresholdCount = valueSet.headSet(threshold).size();
        return belowThresholdCount >= (0.6 * valueSet.size());
    }

    static BigDecimal stepSize(BigDecimal min, BigDecimal max) {
        // Prevents ticks of ugly values.
        // For example, if the diff is 123_456_789, the step size will be 1_000_000.
        var diff = max.subtract(min).abs();
        if (diff.signum() == 0) {
            return BigDecimal.ONE;
        } else {
            var nearestPowerOfTen = (int) Math.round(Math.log10(diff.doubleValue()));
            return BigDecimal.TEN.pow(nearestPowerOfTen - 2, MathContext.DECIMAL64);
        }
    }

    @Override
    public void writeToFile(Path parentFolder) {
        var file = new File(parentFolder.toFile(), id() + ".js");
        file.getParentFile().mkdirs();

        var freeMarkerCfg = BenchmarkReport.createFreeMarkerConfiguration();
        freeMarkerCfg.setClassForTemplateLoading(getClass(), "");

        var templateFilename = "chart-line.js.ftl";
        Map<String, Object> model = new HashMap<>();
        model.put("chart", this);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            var template = freeMarkerCfg.getTemplate(templateFilename);
            template.process(model, writer);
        } catch (IOException e) {
            throw new IllegalArgumentException("Can not read templateFilename (" + templateFilename
                    + ") or write chart file (" + file + ").", e);
        } catch (TemplateException e) {
            throw new IllegalArgumentException("Can not process Freemarker templateFilename (" + templateFilename
                    + ") to chart file (" + id + ").", e);
        }
    }

    public static final class Builder<X extends Number & Comparable<X>, Y extends Number & Comparable<Y>> {
        private static final int MAX_CHART_WIDTH = 3840;
        private final Map<String, NavigableMap<X, Y>> data = new LinkedHashMap<>();
        private final Set<String> favoriteSet = new HashSet<>();

        public Builder<X, Y> add(String dataset, X x, Y y) {
            data.computeIfAbsent(dataset, k -> new TreeMap<>())
                    .put(x, y);
            return this;
        }

        public Set<String> keys() {
            return data.keySet();
        }

        public int count(String dataset) {
            return data.getOrDefault(dataset, Collections.emptyNavigableMap()).size();
        }

        public Y getLastValue(String dataset) {
            return data.getOrDefault(dataset, Collections.emptyNavigableMap())
                    .lastEntry()
                    .getValue();
        }

        public Builder<X, Y> markFavorite(String dataset) {
            favoriteSet.add(dataset);
            return this;
        }

        public LineChart<X, Y> build(String fileName, String title, String xLabel, String yLabel, boolean stepped,
                boolean timeOnX, boolean timeOnY) {
            /*
             * If two of the same value have another of that value between them, remove it.
             * This allows Chart.js to only draw the absolute minimum necessary points.
             */
            data.values().forEach(map -> {
                var entries = map.entrySet().stream().toList();
                if (entries.size() < 3) {
                    return;
                }
                for (var i = 0; i < entries.size() - 2; i++) {
                    var entry1 = entries.get(i);
                    var entry2 = entries.get(i + 1);
                    if (!entry1.getValue().equals(entry2.getValue())) {
                        continue;
                    }
                    var entry3 = entries.get(i + 2);
                    if (entry2.getValue().equals(entry3.getValue())) {
                        map.remove(entry2.getKey());
                    }
                }
            });
            /*
             * Sometimes, when the dataset size is large, it can cause the browser to freeze or use excessive memory
             * while rendering the line chart. To solve the issue of a large volume of data points, we use the
             * Largest-Triangle-Three-Buckets algorithm to down-sample the data.
             */
            Map<String, Map<X, Y>> datasetMap = new LinkedHashMap<>(data.size());
            for (var entry : data.entrySet()) {
                datasetMap.put(entry.getKey(), largestTriangleThreeBuckets(entry.getValue(), MAX_CHART_WIDTH));
            }
            // We need to merge all the keys after the down-sampling process to create a consistent X values list.
            // The xValues list size can be "MAX_CHART_WIDTH * data.size" in the worst case.
            var xValues = data.values().stream()
                    .flatMap(k -> k.keySet().stream())
                    .distinct()
                    .sorted(Comparable::compareTo)
                    .toList();
            /*
             * Finally gather Y values for every such X, even if some are null.
             * Specifying the data like this helps avoid Chart.js quirks during rendering.
             */
            List<Dataset<Y>> datasetList = new ArrayList<>(data.size());
            for (var entry : datasetMap.entrySet()) {
                List<Y> datasetData = new ArrayList<>(xValues.size());
                var dataset = entry.getValue();
                for (var xValue : xValues) {
                    var yValue = dataset.get(xValue);
                    datasetData.add(yValue);
                }
                datasetList.add(new Dataset<>(entry.getKey(), datasetData, favoriteSet.contains(entry.getKey())));
            }
            return new LineChart<>(fileName, title, xLabel, yLabel, xValues, datasetList, stepped, timeOnX, timeOnY);
        }

        /**
         * The method uses the Largest-Triangle-Three-Buckets approach to reduce the size of the data points list.
         * 
         * @param datasetDataMap The ordered map of data points
         * @param sampleSize The final sample size
         * 
         * @return The compressed data
         *
         * @see https://github.com/sveinn-steinarsson/flot-downsample/
         */
        private Map<X, Y> largestTriangleThreeBuckets(NavigableMap<X, Y> datasetDataMap, int sampleSize) {
            if (datasetDataMap.size() <= sampleSize) {
                return datasetDataMap;
            }
            var sampled = new LinkedHashMap<X, Y>(sampleSize);
            List<X> keys = new ArrayList<>(datasetDataMap.keySet());

            // Bucket size. Leave room for start and end data points
            var every = (double) (datasetDataMap.size() - 2) / (double) (sampleSize - 2);

            var a = 0; // Initially a is the first point in the triangle
            var nextA = 0;
            Y maxAreaPoint = null;
            double maxArea;
            double area;

            // Always add the first point
            datasetDataMap.entrySet().stream().findFirst().ifPresent(e -> sampled.put(e.getKey(), e.getValue()));

            for (var i = 0; i < sampleSize - 2; i++) {

                // Calculate point average for next bucket (containing c)
                var avgX = 0.0D;
                var avgY = 0.0D;
                var avgRangeStart = (int) Math.floor((i + 1) * every) + 1;
                var avgRangeEnd = (int) Math.floor((i + 2) * every) + 1;
                avgRangeEnd = Math.min(avgRangeEnd, datasetDataMap.size());

                var avgRangeLength = avgRangeEnd - avgRangeStart;

                while (avgRangeStart < avgRangeEnd) {
                    avgX += keys.get(avgRangeStart).doubleValue();
                    avgY += datasetDataMap.get(keys.get(avgRangeStart)).doubleValue();
                    avgRangeStart++;
                }
                avgX /= avgRangeLength;
                avgY /= avgRangeLength;

                // Get the range for this bucket
                var rangeOffs = (int) Math.floor(i * every) + 1;
                var rangeTo = (int) Math.floor((i + 1) * every) + 1;

                // Point a
                var pointAX = keys.get(a).doubleValue();
                var pointAY = datasetDataMap.get(keys.get(a)).doubleValue();

                maxArea = -1;

                while (rangeOffs < rangeTo) {
                    // Calculate triangle area over three buckets
                    area = Math.abs((pointAX - avgX) * (datasetDataMap.get(keys.get(rangeOffs)).doubleValue() - pointAY)
                            - (pointAX - keys.get(rangeOffs).doubleValue()) * (avgY - pointAY)) * 0.5D;
                    if (area > maxArea) {
                        maxArea = area;
                        maxAreaPoint = datasetDataMap.get(keys.get(rangeOffs));
                        // Next a is this b
                        nextA = rangeOffs;
                    }
                    rangeOffs++;
                }
                // Pick this point from the bucket
                sampled.put(keys.get(nextA), maxAreaPoint);
                // This a is the next a (chosen b)
                a = nextA;
            }

            // Always add last
            sampled.put(keys.get(keys.size() - 1), datasetDataMap.get(keys.get(keys.size() - 1)));
            return sampled;
        }
    }
}
