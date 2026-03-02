package ai.timefold.solver.core.impl.io.jaxb;

import java.util.Map;
import java.util.stream.Collectors;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class JaxbCustomPropertiesAdapter extends XmlAdapter<JaxbAdaptedMap, Map<String, String>> {

    @Override
    public @Nullable Map<String, String> unmarshal(@Nullable JaxbAdaptedMap jaxbAdaptedMap) {
        if (jaxbAdaptedMap == null) {
            return null;
        }
        return jaxbAdaptedMap.getEntries().stream()
                .collect(Collectors.toMap(JaxbAdaptedMapEntry::getName, JaxbAdaptedMapEntry::getValue));
    }

    @Override
    public @Nullable JaxbAdaptedMap marshal(@Nullable Map<String, String> originalMap) {
        if (originalMap == null) {
            return null;
        }
        var entries = originalMap.entrySet().stream()
                .map(entry -> new JaxbAdaptedMapEntry(entry.getKey(), entry.getValue()))
                .toList();
        return new JaxbAdaptedMap(entries);
    }

}
