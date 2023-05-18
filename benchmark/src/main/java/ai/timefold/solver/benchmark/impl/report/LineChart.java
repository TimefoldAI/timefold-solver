package ai.timefold.solver.benchmark.impl.report;

import static ai.timefold.solver.benchmark.impl.report.BenchmarkReport.LOG_SCALE_MIN_DATASETS_COUNT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
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

import freemarker.template.Configuration;
import freemarker.template.Template;
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
    public BigDecimal xStepSize() {
        return stepSize(keys, timeOnX);
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal yStepSize() {
        List<Y> values = datasets.stream()
                .flatMap(d -> d.data().stream())
                .filter(Objects::nonNull)
                .toList();
        return stepSize(values, timeOnY);
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
        List<Y> values = datasets.stream()
                .flatMap(d -> d.data().stream())
                .filter(Objects::nonNull)
                .toList();
        return useLogarithmicProblemScale(values);
    }

    static <N extends Number & Comparable<N>> boolean useLogarithmicProblemScale(List<N> seriesList) {
        NavigableSet<Double> valueSet = new TreeSet<>();
        for (N dataItem : seriesList) {
            double value = dataItem.doubleValue();
            if (value <= 0) { // Logarithm undefined.
                return false;
            }
            valueSet.add(value);
        }
        if (valueSet.size() < LOG_SCALE_MIN_DATASETS_COUNT) {
            return false;
        }
        // If 60% of the points are in 20% of the value space, use a logarithmic scale.
        double threshold = 0.2 * (valueSet.last() - valueSet.first());
        int belowThresholdCount = valueSet.headSet(threshold).size();
        return belowThresholdCount >= (0.6 * valueSet.size());
    }

    static <N extends Number & Comparable<N>> BigDecimal stepSize(List<N> seriesList, boolean isTime) {
        // Prevents ticks of ugly values.
        // For example, if the diff is 123_456_789, the step size will be 1_000_000.
        // In practice, this prescribes the amount of ticks to be around 100.
        if (seriesList.isEmpty()) {
            return BigDecimal.ONE;
        }
        double min = isTime ? 0.0
                : seriesList.stream()
                        .mapToDouble(Number::doubleValue)
                        .min()
                        .orElse(0.0);
        double max = seriesList.stream()
                .mapToDouble(Number::doubleValue)
                .max()
                .orElse(0.0);
        double diff = Math.abs(max - min);
        if (diff == 0) {
            return BigDecimal.ONE;
        } else if (diff > 1000) {
            int digits = 0;
            long num = (long) diff;
            while (num != 0) {
                num /= 10;
                ++digits;
            }
            return BigDecimal.TEN.pow(digits - 2);
        } else if (diff > 100) {
            return BigDecimal.TEN;
        } else if (diff > 10) {
            return BigDecimal.ONE;
        } else if (diff > 1) {
            return new BigDecimal("0.1");
        } else if (diff > 0.1) {
            return new BigDecimal("0.01");
        } else if (diff > 0.01) {
            return new BigDecimal("0.001");
        } else if (diff > 0.001) {
            return new BigDecimal("0.0001");
        } else {
            return new BigDecimal("0.00001");
        }
    }

    @Override
    public void writeToFile(Path parentFolder) {
        File file = new File(parentFolder.toFile(), id() + ".js");
        file.getParentFile().mkdirs();

        Configuration freeMarkerCfg = BenchmarkReport.createFreeMarkerConfiguration();
        freeMarkerCfg.setClassForTemplateLoading(getClass(), "");

        String templateFilename = "chart-line.js.ftl";
        Map<String, Object> model = new HashMap<>();
        model.put("chart", this);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            Template template = freeMarkerCfg.getTemplate(templateFilename);
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
                List<Map.Entry<X, Y>> entries = map.entrySet().stream().toList();
                if (entries.size() < 3) {
                    return;
                }
                for (int i = 0; i < entries.size() - 2; i++) {
                    Map.Entry<X, Y> entry1 = entries.get(i);
                    Map.Entry<X, Y> entry2 = entries.get(i + 1);
                    if (!entry1.getValue().equals(entry2.getValue())) {
                        continue;
                    }
                    Map.Entry<X, Y> entry3 = entries.get(i + 2);
                    if (entry2.getValue().equals(entry3.getValue())) {
                        map.remove(entry2.getKey());
                    }
                }
            });
            // Then find all points on the X axis across all data sets.
            List<X> xValues = data.values().stream()
                    .flatMap(m -> m.keySet().stream())
                    .distinct()
                    .sorted(Comparable::compareTo)
                    .toList();
            /*
             * Finally gather Y values for every such X, even if some are null.
             * Specifying the data like this helps avoid Chart.js quirks during rendering.
             */
            List<Dataset<Y>> datasetList = new ArrayList<>(data.size());
            for (String datasetLabel : data.keySet()) {
                NavigableMap<X, Y> dataset = data.get(datasetLabel);
                List<Y> datasetData = new ArrayList<>(xValues.size());
                for (X xValue : xValues) {
                    Y yValue = dataset.get(xValue);
                    datasetData.add(yValue);
                }
                datasetList.add(new Dataset<>(datasetLabel, datasetData, favoriteSet.contains(datasetLabel)));
            }
            return new LineChart<>(fileName, title, xLabel, yLabel, xValues, datasetList, stepped, timeOnX, timeOnY);
        }

    }

}
