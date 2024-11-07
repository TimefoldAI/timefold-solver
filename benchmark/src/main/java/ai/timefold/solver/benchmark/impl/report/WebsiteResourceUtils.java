package ai.timefold.solver.benchmark.impl.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.function.Function;

public final class WebsiteResourceUtils {

    private static final String RESOURCE_NAMESPACE = "/ai/timefold/solver/benchmark/impl/report/";
    private static final String WEBJAR_RESOURCE_NAMESPACE = "META-INF/resources/webjars/timefold/";

    public static void copyResourcesTo(File benchmarkReportDirectory) {
        // Manually extract webjars, as webjar-locator had issues with Quarkus.
        copyWebjarResource(benchmarkReportDirectory, "css/timefold-webui.css");
        copyWebjarResource(benchmarkReportDirectory, "js/timefold-webui.js");
        copyWebjarResource(benchmarkReportDirectory, "img/timefold-favicon.svg");
        copyWebjarResource(benchmarkReportDirectory, "img/timefold-logo-horizontal-negative.svg");
        copyWebjarResource(benchmarkReportDirectory, "img/timefold-logo-horizontal-positive.svg");
        copyWebjarResource(benchmarkReportDirectory, "img/timefold-logo-stacked-positive.svg");
        // Manually copy some other resources.
        copyResource(benchmarkReportDirectory, "website/css/prettify.css");
        copyResource(benchmarkReportDirectory, "website/css/app.css");
        copyResource(benchmarkReportDirectory, "website/js/chartjs-plugin-watermark.js");
        copyResource(benchmarkReportDirectory, "website/js/prettify.js");
        copyResource(benchmarkReportDirectory, "website/js/app.js");
    }

    private static void copyWebjarResource(File benchmarkReportDirectory, String websiteResource) {
        copyResource(benchmarkReportDirectory, WEBJAR_RESOURCE_NAMESPACE, websiteResource, "website/" + websiteResource,
                s -> WebsiteResourceUtils.class.getClassLoader().getResourceAsStream(s));
    }

    private static void copyResource(File benchmarkReportDirectory, String websiteResource) {
        copyResource(benchmarkReportDirectory, RESOURCE_NAMESPACE, websiteResource, websiteResource,
                WebsiteResourceUtils.class::getResourceAsStream);
    }

    private static void copyResource(File benchmarkReportDirectory, String namespace, String websiteResource,
            String targetResource, Function<String, InputStream> resourceLoader) {
        var outputFile = new File(benchmarkReportDirectory, targetResource);
        outputFile.getParentFile().mkdirs();
        try (var in = resourceLoader.apply(namespace + websiteResource)) {
            if (in == null) {
                throw new IllegalStateException("The websiteResource (%s) does not exist."
                        .formatted(websiteResource));
            }
            Files.copy(in, outputFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not copy websiteResource (%s) to outputFile (%s)."
                    .formatted(websiteResource, outputFile), e);
        }
    }

    private WebsiteResourceUtils() {
    }

}
