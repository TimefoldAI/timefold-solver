package ai.timefold.solver.benchmark.impl.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
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
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 *
 * @param title
 * @param xLabel
 * @param yLabel
 * @param categories Typically dataset names.
 * @param ranges Keys are range labels (such "Machine reassignment Tabu Search"), values are ranges for each category.
 *        Value may be null if there is no data for a category, such as the solver did not run on the dataset.
 * @param favorites Range labels that should be highlighted.
 */
public record BoxPlot(String id, String title, String xLabel, String yLabel, List<String> categories,
        Map<String, List<Range>> ranges, Set<String> favorites)
        implements
            Chart {

    public BoxPlot {
        id = Chart.makeIdUnique(id);
    }

    @Override
    public void writeToFile(Path parentFolder) {
        File file = new File(parentFolder.toFile(), id() + ".js");
        file.getParentFile().mkdirs();

        Configuration freeMarkerCfg = BenchmarkReport.createFreeMarkerConfiguration();
        freeMarkerCfg.setClassForTemplateLoading(getClass(), "");

        String templateFilename = "chart-box.js.ftl";
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

    @SuppressWarnings("unused") // Used by FreeMarker.
    public static final class Builder {

        private final Map<String, NavigableMap<String, List<Double>>> data = new LinkedHashMap<>();
        private final Set<String> favoriteSet = new HashSet<>();

        public Builder add(String dataset, String category, double y) {
            data.computeIfAbsent(dataset, k -> new TreeMap<>())
                    .computeIfAbsent(category, k -> new ArrayList<>())
                    .add(y);
            return this;
        }

        public Set<String> keys() {
            return data.keySet();
        }

        public Builder markFavorite(String dataset) {
            favoriteSet.add(dataset);
            return this;
        }

        public BoxPlot build(String fileName, String title, String xLabel, String yLabel) {
            // First find all categories across all data sets.
            List<String> categories = data.values().stream()
                    .flatMap(m -> m.keySet().stream())
                    .distinct()
                    .sorted(Comparable::compareTo)
                    .toList();
            // Now gather Y values for every such category, even if some are null.
            // Specifying the data like this helps avoid Chart.js quirks during rendering.
            Map<String, List<Range>> rangeMap = new LinkedHashMap<>(data.size());
            for (String rangeLabel : data.keySet()) {
                List<Range> rangeList = new ArrayList<>();
                for (String category : categories) {
                    List<Double> values = data.get(rangeLabel)
                            .getOrDefault(category, Collections.emptyList());
                    if (values.size() == 0) {
                        rangeList.add(null);
                    } else {
                        DescriptiveStatistics stats = new DescriptiveStatistics();
                        values.forEach(stats::addValue);
                        rangeList.add(new Range(rangeLabel, stats.getMin(), stats.getPercentile(25), stats.getPercentile(50),
                                stats.getPercentile(75), stats.getMax(), stats.getMean()));
                    }
                }
                rangeMap.put(rangeLabel, rangeList);
            }
            return new BoxPlot(fileName, title, xLabel, yLabel, categories, rangeMap, favoriteSet);
        }

    }

}
