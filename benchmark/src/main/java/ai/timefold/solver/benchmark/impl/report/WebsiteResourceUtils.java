package ai.timefold.solver.benchmark.impl.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.webjars.WebJarAssetLocator;

public class WebsiteResourceUtils {

    private static final String RESOURCE_NAMESPACE = "/ai/timefold/solver/benchmark/impl/report/";

    public static void copyResourcesTo(File benchmarkReportDirectory) {
        // Extract webjars.
        File webjarDirectory = new File(benchmarkReportDirectory, "website/webjars/");
        new WebJarAssetLocator()
                .getAllWebJars()
                .forEach((artifactId, webjarInfo) -> {
                    // The webjarInfo typically contains the version, but we don't want to use it in the path.
                    String resourcePrefix = "META-INF/resources/webjars/" + artifactId + "/";
                    String resourcePrefixWithVersion = resourcePrefix + webjarInfo.getVersion() + "/";
                    File webjarTargetDirectory = new File(webjarDirectory, artifactId);
                    webjarInfo.getContents().forEach(resource -> {
                        /*
                         * Some webjars do not have a version in the resource path.
                         * Case in point: the Timefold webjar.
                         */
                        String actualResourcePrefix =
                                resource.startsWith(resourcePrefixWithVersion) ? resourcePrefixWithVersion : resourcePrefix;
                        String relativePath = resource.substring(actualResourcePrefix.length());
                        copyResource(webjarTargetDirectory, "/", resource, relativePath);
                    });
                });
        // Manually copy some additional resources.
        copyResource(benchmarkReportDirectory, "website/css/prettify.css");
        copyResource(benchmarkReportDirectory, "website/css/app.css");
        copyResource(benchmarkReportDirectory, "website/js/chartjs-plugin-watermark.js");
        copyResource(benchmarkReportDirectory, "website/js/prettify.js");
        copyResource(benchmarkReportDirectory, "website/js/app.js");
    }

    private static void copyResource(File benchmarkReportDirectory, String websiteResource) {
        copyResource(benchmarkReportDirectory, websiteResource, websiteResource);
    }

    private static void copyResource(File benchmarkReportDirectory, String websiteResource, String targetResource) {
        copyResource(benchmarkReportDirectory, RESOURCE_NAMESPACE, websiteResource, targetResource);
    }

    private static void copyResource(File benchmarkReportDirectory, String namespace, String websiteResource,
            String targetResource) {
        File outputFile = new File(benchmarkReportDirectory, targetResource);
        outputFile.getParentFile().mkdirs();
        try (InputStream in = WebsiteResourceUtils.class.getResourceAsStream(namespace + websiteResource)) {
            if (in == null) {
                throw new IllegalStateException("The websiteResource (" + websiteResource
                        + ") does not exist.");
            }
            Files.copy(in, outputFile.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not copy websiteResource (" + websiteResource
                    + ") to outputFile (" + outputFile + ").", e);
        }
    }

    private WebsiteResourceUtils() {
    }

}
