package ai.timefold.solver.benchmark.impl.report;

import static java.util.Collections.emptySet;
import static java.util.Map.entry;
import static java.util.Set.of;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;

import org.webjars.WebJarAssetLocator;

public final class WebsiteResourceUtils {

    private static final String RESOURCE_NAMESPACE = "/ai/timefold/solver/benchmark/impl/report/";

    public static void copyResourcesTo(File benchmarkReportDirectory) {
        /*
         * Describe which webjar resources we need.
         * Otherwise the report would be dozens of megabytes in size.
         */
        Map<String, Set<String>> webjarToResourceMap = Map.ofEntries(
                entry("timefold",
                        emptySet()),
                entry("bootstrap",
                        of("css/bootstrap.min.css", "js/bootstrap.bundle.min.js")),
                entry("font-awesome",
                        of("css/all.min.css", "sprites/brands.svg", "sprites/solid.svg")),
                entry("jquery",
                        of("jquery.min.js")));
        // Extract webjars.
        File webjarDirectory = new File(benchmarkReportDirectory, "website/webjars/");
        new WebJarAssetLocator()
                .getAllWebJars()
                .forEach((artifactId, webjarInfo) -> {
                    if (!webjarToResourceMap.containsKey(artifactId)) {
                        return;
                    }
                    // The webjarInfo typically contains the version, but we don't want to use it in the path.
                    String resourcePrefix = "META-INF/resources/webjars/" + artifactId + "/";
                    String resourcePrefixWithVersion = resourcePrefix + webjarInfo.getVersion() + "/";
                    File webjarTargetDirectory = new File(webjarDirectory, artifactId);
                    webjarInfo.getContents().forEach(resource -> {
                        Set<String> resourceSet = webjarToResourceMap.get(artifactId); // Empty means all resources.
                        /*
                         * Some webjars do not have a version in the resource path.
                         * Case in point: the Timefold webjar.
                         */
                        String actualResourcePrefix =
                                resource.startsWith(resourcePrefixWithVersion) ? resourcePrefixWithVersion : resourcePrefix;
                        if (resourceSet.isEmpty() || resourceSet.stream().anyMatch(resource::endsWith)) {
                            // Only copy the resources we need.
                            String relativePath = resource.substring(actualResourcePrefix.length());
                            copyResource(webjarTargetDirectory, "/", resource, relativePath);
                        }
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
