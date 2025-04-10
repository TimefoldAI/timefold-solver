package ai.timefold.solver.core.impl.testdata.domain.invalid.variablemap;

import java.util.AbstractMap;
import java.util.Set;

import ai.timefold.solver.core.api.domain.constraintweight.ConstraintConfiguration;

@ConstraintConfiguration
public class DummyMapConstraintConfiguration extends AbstractMap<String, String> {

    @Override
    public Set<Entry<String, String>> entrySet() {
        return Set.of();
    }
}
