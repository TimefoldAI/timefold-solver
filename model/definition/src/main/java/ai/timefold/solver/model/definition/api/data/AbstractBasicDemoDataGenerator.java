package ai.timefold.solver.model.definition.api.data;

import java.util.List;

import ai.timefold.solver.model.definition.api.ModelConfigOverrides;
import ai.timefold.solver.model.definition.api.ModelInput;
import ai.timefold.solver.model.definition.api.domain.ModelRequest;

/**
 * Default {@link DemoDataGenerator} implementation for models that provide only a single demo data set with ID
 * {@value #BASIC_DEMO_DATA_ID}.
 * <p>
 * In other cases, please implement the {@link DemoDataGenerator} interface directly.
 */
public abstract class AbstractBasicDemoDataGenerator<ModelInput_ extends ModelInput, ModelConfigOverrides_ extends ModelConfigOverrides>
        implements DemoDataGenerator {

    public static final String BASIC_DEMO_DATA_ID = "BASIC";

    /**
     * The default demo data set metadata with ID {@value #BASIC_DEMO_DATA_ID} and no description.
     */
    public static final DemoMetaData DEFAULT_BASIC_META_DATA = new DemoMetaData(BASIC_DEMO_DATA_ID);

    /**
     * Returns {@link DemoMetaData} about demo datasets supported by this generator.
     * <p>
     * This implementation supports only a single demo data set with ID {@value #BASIC_DEMO_DATA_ID}
     * and always returns {@link #DEFAULT_BASIC_META_DATA}.
     *
     * @return list of supported demo data sets metadata
     */
    @Override
    public final List<DemoMetaData> demoMetaData() {
        return List.of(DEFAULT_BASIC_META_DATA);
    }

    /**
     * Returns the demo data for the {@value #BASIC_DEMO_DATA_ID} demo data set.
     *
     * @param demoDataId ID of demo data that should be generated (must be {@value #BASIC_DEMO_DATA_ID})
     * @return ModelRequest populated with demo data and default configuration
     */
    @Override
    public final DemoData generateDemoData(String demoDataId) {
        if (BASIC_DEMO_DATA_ID.equals(demoDataId)) {
            return new DemoData(DEFAULT_BASIC_META_DATA, generateBasicDemoDataRequest());
        }
        throw new IllegalArgumentException("Unsupported demo data ID: " + demoDataId);
    }

    /**
     * Implement this method to provide the actual demo data for {@value #BASIC_DEMO_DATA_ID} dataset.
     *
     * @return the {@link ModelRequest} representing the {@value #BASIC_DEMO_DATA_ID} demo data
     */
    protected abstract ModelRequest<ModelInput_, ModelConfigOverrides_> generateBasicDemoDataRequest();
}
