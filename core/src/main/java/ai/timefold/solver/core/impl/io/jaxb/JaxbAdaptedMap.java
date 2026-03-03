package ai.timefold.solver.core.impl.io.jaxb;

import java.util.List;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

// Required to generate the XSD type in the same namespace.
@NullMarked
@XmlType(namespace = SolverConfig.XML_NAMESPACE)
public final class JaxbAdaptedMap {

    @XmlElement(name = "property", namespace = SolverConfig.XML_NAMESPACE)
    private @Nullable List<JaxbAdaptedMapEntry> entries;

    public JaxbAdaptedMap() {
        // Required by JAXB
    }

    public JaxbAdaptedMap(@Nullable List<JaxbAdaptedMapEntry> entries) {
        this.entries = entries;
    }

    public @Nullable List<JaxbAdaptedMapEntry> getEntries() {
        return entries;
    }

}
