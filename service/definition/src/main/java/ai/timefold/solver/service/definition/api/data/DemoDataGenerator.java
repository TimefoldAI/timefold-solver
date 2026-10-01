package ai.timefold.solver.service.definition.api.data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Demo data generator for models that can be used to verify execution of the model's logic.
 * <p>
 * IMPORTANT: Implementations of this interface
 * must be dependency free meaning simple instantiation (even with reflection) of this class is enough to generate demo data.
 */
public interface DemoDataGenerator {

    /**
     * Generate demo datasets based on the UserModel together with its metadata.
     * <p>
     * The default implementation returns demo data for all meta data returned by {@link #demoMetaData()}.
     *
     * @return possible demo data set for given model
     */
    default List<DemoData> generateDemoData() {
        return demoMetaData().stream().map(metaData -> generateDemoData(metaData.id())).collect(Collectors.toList());
    }

    /**
     * Returns {@link DemoMetaData} about demo datasets supported by this generator.
     *
     * @return list of supported demo data sets metadata
     */
    List<DemoMetaData> demoMetaData();

    /**
     * Returns <code>ModelRequest</code> generated with demo data
     *
     * @param demoDataId ID of demo data that should be generated
     * @return ModelRequest populated with demo data and default configuration
     */
    DemoData generateDemoData(String demoDataId);
}
