package ai.timefold.solver.benchmark.impl.report;

import java.nio.file.Path;

@SuppressWarnings("unused") // Used by FreeMarker.
public interface Chart {

    /**
     * It is not possible to guarantee that an ID we generate in the Java code
     * will be unique in the HTML page without making the Java code complex enough to track those IDs.
     * For that reason, we take a human-readable ID and make it unique by appending a random number.
     *
     * @param id never null
     * @return never null, guaranteed to be unique
     */
    static String makeIdUnique(String id) {
        return id + "_" + Integer.toHexString((int) (Math.random() * 1_000_000));
    }

    String id();

    String title();

    String xLabel();

    String yLabel();

    void writeToFile(Path parentFolder);

}
