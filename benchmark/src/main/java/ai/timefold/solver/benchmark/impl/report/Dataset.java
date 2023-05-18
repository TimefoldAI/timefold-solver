package ai.timefold.solver.benchmark.impl.report;

import java.util.List;

public record Dataset<X extends Number>(String label, List<X> data, boolean favorite) {

}
