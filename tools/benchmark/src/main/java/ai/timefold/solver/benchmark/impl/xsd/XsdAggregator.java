package ai.timefold.solver.benchmark.impl.xsd;

import static ai.timefold.solver.benchmark.config.PlannerBenchmarkConfig.SOLVER_NAMESPACE_PREFIX;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.io.jaxb.GenericJaxbIO;

import org.jspecify.annotations.NullMarked;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * This class merges solver.xsd and benchmark.xsd into a single XML Schema file that contains both Solver and Benchmark XML
 * types under a single namespace of the benchmark.xsd.
 * <p>
 * Both solver.xsd and benchmark.xsd declare its own namespace as they are supposed to be used for different purposes. As the
 * benchmark configuration contains solver configuration, the benchmark.xsd imports the solver.xsd. To avoid distributing
 * dependent schemas and using prefixes in users' XML configuration files, the types defined by solver.xsd are merged to
 * the benchmark.xsd under its namespace.
 */
@NullMarked
public final class XsdAggregator {

    private static final String TNS_PREFIX = "tns";

    public static void main(String[] args) {
        if (args.length != 3) {
            var msg = """
                    The XSD Aggregator expects 3 arguments:
                    1) a path to the solver XSD file.
                    2) a path to the benchmark XSD file.
                    3) a path to an output file where the merged benchmark XSD should be saved to.""";
            throw new IllegalArgumentException(msg);
        }
        var solverXsd = checkFileExists(new File(args[0]));
        var benchmarkXsd = checkFileExists(new File(args[1]));
        var outputXsd = new File(args[2]);

        if (!outputXsd.getParentFile().exists()) {
            outputXsd.getParentFile().mkdirs();
        }

        new XsdAggregator().mergeXmlSchemas(solverXsd, benchmarkXsd, outputXsd);
    }

    private static File checkFileExists(File file) {
        Objects.requireNonNull(file);
        if (!file.exists()) {
            throw new IllegalArgumentException(String.format("The file (%s) does not exist.", file.getAbsolutePath()));
        }
        return file;
    }

    private void mergeXmlSchemas(File solverSchemaFile, File benchmarkSchemaFile, File outputSchemaFile) {
        var factory = GenericJaxbIO.createDocumentBuilderFactory();
        var solverSchema = parseXml(solverSchemaFile, factory);
        var solverRootElement = solverSchema.getDocumentElement();
        var benchmarkSchema = parseXml(benchmarkSchemaFile, factory);

        removeReferencesToSolverConfig(benchmarkSchema, benchmarkSchemaFile);

        copySolverConfigTypes(benchmarkSchema, solverRootElement);

        var source = new DOMSource(benchmarkSchema);
        var result = new StreamResult(outputSchemaFile);
        try {
            createTransformer().transform(source, result);
        } catch (TransformerException e) {
            throw new IllegalArgumentException("Failed to write the resulting XSD to a file (%s)."
                    .formatted(outputSchemaFile.getAbsolutePath()), e);
        }
    }

    private Document parseXml(File xmlFile, DocumentBuilderFactory documentBuilderFactory) {
        try {
            return documentBuilderFactory.newDocumentBuilder().parse(xmlFile);
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException("Failed to create a %s instance."
                    .formatted(DocumentBuilder.class.getSimpleName()), e);
        } catch (SAXException | IOException exception) {
            throw new IllegalArgumentException("Failed to parse an XML file (%s)."
                    .formatted(xmlFile.getAbsolutePath()), exception);
        }
    }

    private void removeReferencesToSolverConfig(Document benchmarkSchema, File benchmarkSchemaFile) {
        var solverNamespaceRemoved = false;
        var solverElementRefRemoved = false;
        var importRemoved = false;

        var nodeList = benchmarkSchema.getElementsByTagName("*");
        for (var i = 0; i < nodeList.getLength(); i++) {
            var node = Objects.requireNonNull(nodeList.item(i));
            var element = (Element) node;

            if ("xs:schema".equals(node.getNodeName())) { // Remove the solver namespace declaration.
                element.removeAttribute("xmlns:" + SOLVER_NAMESPACE_PREFIX);
                solverNamespaceRemoved = true;
            }

            // Replace a reference to a solver element by a reference to type.
            if (isXsElement(node) && hasAttribute(node, "ref", SOLVER_NAMESPACE_PREFIX + ":" + SolverConfig.XML_ELEMENT_NAME)) {
                element.removeAttribute("ref");
                element.setAttribute("name", SolverConfig.XML_ELEMENT_NAME);
                element.setAttribute("type", TNS_PREFIX + ":" + SolverConfig.XML_TYPE_NAME);
                solverElementRefRemoved = true;
            }

            if ("xs:import".equals(node.getNodeName())) { // Remove the solver.xsd import.
                node.getParentNode().removeChild(node);
                importRemoved = true;
            }

            // Replace the solver namespace prefix by a standard "tns:" in all attributes.
            updateNodeAttributes(node,
                    attr -> attr.getValue() != null && attr.getValue().startsWith(SOLVER_NAMESPACE_PREFIX + ":"),
                    oldValue -> oldValue.replace(SOLVER_NAMESPACE_PREFIX + ":", TNS_PREFIX + ":"));
        }

        /*
         * Fail fast if some of the expected modifications were not done. Remaining modifications are necessary for
         * a successful validation by the resulting XML schema.
         */
        if (!solverElementRefRemoved) {
            var msg = String.format("An expected reference to the solver element was not found. Check the content of (%s).",
                    benchmarkSchemaFile);
            throw new AssertionError(msg);
        }

        if (!solverNamespaceRemoved) {
            var msg = String.format("An expected namespace (%s) declaration was not found. Check the content of (%s).",
                    SolverConfig.XML_NAMESPACE, benchmarkSchemaFile);
            throw new AssertionError(msg);
        }

        if (!importRemoved) {
            var msg =
                    String.format("An expected import element was not found. Check the content of (%s).", benchmarkSchemaFile);
            throw new AssertionError(msg);
        }
    }

    private void copySolverConfigTypes(Document benchmarkSchema, Element solverSchemaRoot) {
        var benchmarkSchemaRoot = benchmarkSchema.getDocumentElement();
        var solverChildNodes = solverSchemaRoot.getChildNodes();
        for (var i = 0; i < solverChildNodes.getLength(); i++) {
            var node = solverChildNodes.item(i);
            var isSolverElementDeclaration = isXsElement(node) && hasAttribute(node, "name", SolverConfig.XML_ELEMENT_NAME);
            if (!isSolverElementDeclaration) { // Skip the solver root element.
                benchmarkSchemaRoot.appendChild(benchmarkSchema.importNode(node, true));
            }
        }
    }

    private boolean isXsElement(Node node) {
        return "xs:element".equals(node.getNodeName());
    }

    private boolean hasAttribute(Node node, String attributeName, String attributeValue) {
        var attribute = ((Element) node).getAttributeNode(attributeName);
        return (attribute != null && attributeValue.equals(attribute.getValue()));
    }

    private void updateNodeAttributes(Node node, Predicate<Attr> attributePredicate, UnaryOperator<String> valueFunction) {
        for (var i = 0; i < node.getAttributes().getLength(); i++) {
            var attribute = (Attr) node.getAttributes().item(i);
            if (attributePredicate.test(attribute)) {
                attribute.setValue(valueFunction.apply(attribute.getValue()));
            }
        }
    }

    private Transformer createTransformer() {
        var transformerFactory = GenericJaxbIO.createTransformerFactory();
        try {
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.STANDALONE, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            return transformer;
        } catch (TransformerConfigurationException e) {
            throw new IllegalArgumentException("Failed to create a %s."
                    .formatted(Transformer.class.getSimpleName()), e);
        }
    }
}
