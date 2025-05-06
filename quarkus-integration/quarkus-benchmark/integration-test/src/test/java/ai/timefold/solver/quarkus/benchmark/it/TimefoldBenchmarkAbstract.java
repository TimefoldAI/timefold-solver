package ai.timefold.solver.quarkus.benchmark.it;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class TimefoldBenchmarkAbstract {

    public void deleteAllFolders(File directoryToBeDeleted) {
        var allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (var file : allContents) {
                deleteAllFolders(file);
            }
        }
        try {
            Files.delete(directoryToBeDeleted.toPath());
        } catch (IOException e) {
            // Ignore
        }
    }
}
