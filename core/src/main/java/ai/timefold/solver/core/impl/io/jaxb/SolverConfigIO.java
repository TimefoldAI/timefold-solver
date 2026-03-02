package ai.timefold.solver.core.impl.io.jaxb;

import java.io.Reader;
import java.io.Writer;

import ai.timefold.solver.core.config.solver.SolverConfig;

import org.jspecify.annotations.NullMarked;
import org.w3c.dom.Document;

@NullMarked
public class SolverConfigIO {
    private static final String SOLVER_XSD_RESOURCE = "/solver.xsd";
    private final GenericJaxbIO<SolverConfig> genericJaxbIO = new GenericJaxbIO<>(SolverConfig.class);

    public SolverConfig read(Reader reader) {
        Document document = genericJaxbIO.parseXml(reader);
        String rootElementNamespace = document.getDocumentElement().getNamespaceURI();
        if (SolverConfig.XML_NAMESPACE.equals(rootElementNamespace)) { // If there is the correct namespace, validate.
            return genericJaxbIO.readAndValidate(document, SOLVER_XSD_RESOURCE);
        } else if (rootElementNamespace == null || rootElementNamespace.isEmpty()) {
            // If not, add the missing namespace to maintain backward compatibility.
            return genericJaxbIO.readOverridingNamespace(document,
                    new ElementNamespaceOverride(SolverConfig.XML_ELEMENT_NAME, SolverConfig.XML_NAMESPACE));
        } else { // If there is an unexpected namespace, fail fast.
            String errorMessage = String.format("The <%s/> element belongs to a different namespace (%s) than expected (%s).\n"
                    + "Maybe you passed a benchmark configuration to a method expecting a solver configuration.",
                    SolverConfig.XML_ELEMENT_NAME, rootElementNamespace, SolverConfig.XML_NAMESPACE);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    public void write(SolverConfig solverConfig, Writer writer) {
        genericJaxbIO.writeWithoutNamespaces(solverConfig, writer);
    }

}
