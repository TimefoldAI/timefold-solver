package ai.timefold.solver.benchmark.impl.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public record BarChart<Y extends Number & Comparable<Y>>(String id, String title, String xLabel, String yLabel,
        List<String> categories, List<Dataset<Y>> datasets, boolean timeOnY)
        implements
            Chart {

    public BarChart {
        id = Chart.makeIdUnique(id);
    }

    @SuppressWarnings("unused") // Used by FreeMarker.
    public BigDecimal yStepSize() {
        List<Y> values = datasets.stream()
                .flatMap(d -> d.data().stream())
                .filter(Objects::nonNull)
                .toList();
        return LineChart.stepSize(values, timeOnY);
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
        return LineChart.useLogarithmicProblemScale(values);
    }

    @Override
    public void writeToFile(Path parentFolder) {
        File file = new File(parentFolder.toFile(), id() + ".js");
        file.getParentFile().mkdirs();

        Configuration freeMarkerCfg = BenchmarkReport.createFreeMarkerConfiguration();
        freeMarkerCfg.setClassForTemplateLoading(getClass(), "");

        String templateFilename = "chart-bar.js.ftl";
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

    public static final class Builder<Y extends Number & Comparable<Y>> {

        private final Map<String, NavigableMap<String, Y>> data = new LinkedHashMap<>();
        private final Set<String> favoriteSet = new HashSet<>();

        public Builder<Y> add(String dataset, String category, Y y) {
            data.computeIfAbsent(dataset, k -> new TreeMap<>())
                    .put(category, y);
            return this;
        }

        public Set<String> keys() {
            return data.keySet();
        }

        public Builder<Y> markFavorite(String dataset) {
            favoriteSet.add(dataset);
            return this;
        }

        public BarChart<Y> build(String fileName, String title, String xLabel, String yLabel, boolean timeOnY) {
            // First find all categories across all data sets.
            List<String> categories = data.values().stream()
                    .flatMap(m -> m.keySet().stream())
                    .distinct()
                    .sorted(Comparable::compareTo)
                    .toList();
            /*
             * Now gather Y values for every such category, even if some are null.
             * Specifying the data like this helps avoid Chart.js quirks during rendering.
             */
            List<Dataset<Y>> datasetList = new ArrayList<>(data.size());
            for (String datasetLabel : data.keySet()) {
                List<Y> datasetData = new ArrayList<>(categories.size());
                for (String category : categories) {
                    Y yValue = data.get(datasetLabel).get(category);
                    datasetData.add(yValue);
                }
                datasetList.add(new Dataset<>(datasetLabel, datasetData, favoriteSet.contains(datasetLabel)));
            }
            return new BarChart<>(fileName, title, xLabel, yLabel, categories, datasetList, timeOnY);
        }

    }

}
