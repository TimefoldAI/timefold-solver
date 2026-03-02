package ai.timefold.solver.core.impl.io.jaxb;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.util.ValidationEventCollector;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

@NullMarked
public final class GenericJaxbIO<T> {

    public static DocumentBuilderFactory createDocumentBuilderFactory() {
        try {
            var factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setNamespaceAware(true);
            return factory;
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(
                    "Failed to create a secure %s instance.".formatted(DocumentBuilderFactory.class.getSimpleName()), e);
        }
    }

    public static TransformerFactory createTransformerFactory() {
        var factory = TransformerFactory.newInstance();
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }

    public static SchemaFactory createSchemaFactory(Class<?> rootClass, String schemaResource) {
        try {
            var schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            schemaFactory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return schemaFactory;
        } catch (SAXNotSupportedException | SAXNotRecognizedException saxException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to configure the %s to validate an XML for a root class (%s) using the (%s) XML Schema."
                            .formatted(SchemaFactory.class.getSimpleName(), rootClass.getName(), schemaResource),
                    saxException);
        }
    }

    public static Validator createValidator(Schema schema, Class<?> rootClass) {
        try {
            var validator = schema.newValidator();
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return validator;
        } catch (SAXNotSupportedException | SAXNotRecognizedException saxException) {
            throw new TimefoldXmlSerializationException("Failed to configure the %s to validate an XML for a root class (%s)."
                    .formatted(Validator.class.getSimpleName(), rootClass.getName()), saxException);
        }
    }

    private static final int DEFAULT_INDENTATION = 2;

    private static final String ERR_MSG_WRITE = "Failed to marshall a root element class (%s) to XML.";
    private static final String ERR_MSG_READ = "Failed to unmarshall a root element class (%s) from XML.";
    private static final String ERR_MSG_READ_OVERRIDE_NAMESPACE =
            "Failed to unmarshall a root element class (%s) from XML with overriding elements' namespaces: (%s).";

    private final JAXBContext jaxbContext;
    private final Marshaller marshaller;
    private final Class<T> rootClass;
    private final int indentation;

    public GenericJaxbIO(Class<T> rootClass) {
        this(rootClass, DEFAULT_INDENTATION);
    }

    public GenericJaxbIO(Class<T> rootClass, int indentation) {
        this.rootClass = rootClass;
        this.indentation = indentation;
        try {
            jaxbContext = JAXBContext.newInstance(rootClass);
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StandardCharsets.UTF_8.toString());
        } catch (JAXBException jaxbException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to create JAXB Marshaller for a root element class (%s).".formatted(rootClass.getName()),
                    jaxbException);
        }
    }

    public T read(Reader reader) {
        try {
            return (T) createUnmarshaller().unmarshal(reader);
        } catch (JAXBException jaxbException) {
            throw new TimefoldXmlSerializationException(ERR_MSG_READ.formatted(rootClass.getName()), jaxbException);
        }
    }

    public T readAndValidate(Document document, String schemaResource) {
        return readAndValidate(document, readSchemaResource(schemaResource));
    }

    private Schema readSchemaResource(String schemaResource) {
        var schemaResourceUrl = GenericJaxbIO.class.getResource(schemaResource);
        if (schemaResourceUrl == null) {
            throw new IllegalArgumentException("""
                    The XML schema (%s) does not exist.
                    Maybe build the sources with Maven first?""".formatted(schemaResource));
        }
        try {
            var schemaFactory = createSchemaFactory(rootClass, schemaResource);
            return schemaFactory.newSchema(schemaResourceUrl);
        } catch (SAXException saxException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to read an XML Schema resource (%s) to validate an XML for a root class (%s)."
                            .formatted(schemaResource, rootClass.getName()),
                    saxException);
        }
    }

    public T readAndValidate(Reader reader, Schema schema) {
        return readAndValidate(parseXml(reader), schema);
    }

    public T readAndValidate(Document document, Schema schema) {
        var unmarshaller = createUnmarshaller();
        unmarshaller.setSchema(schema);

        var validationEventCollector = new ValidationEventCollector();
        try {
            unmarshaller.setEventHandler(validationEventCollector);
        } catch (JAXBException jaxbException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to set a validation event handler to the %s for a root element class (%s)."
                            .formatted(Unmarshaller.class.getSimpleName(), rootClass.getName()),
                    jaxbException);
        }

        try {
            return (T) unmarshaller.unmarshal(document);
        } catch (JAXBException jaxbException) {
            if (validationEventCollector.hasEvents()) {
                var validationErrors = Stream
                        .of(validationEventCollector.getEvents()).map(validationEvent -> validationEvent.getMessage()
                                + "\nNode: " + validationEvent.getLocator().getNode().getNodeName())
                        .collect(Collectors.joining("\n"));
                throw new TimefoldXmlSerializationException("""
                        XML validation failed for a root element class (%s).
                        %s
                        """.formatted(rootClass.getName(), validationErrors), jaxbException);
            } else {
                throw new TimefoldXmlSerializationException(ERR_MSG_READ.formatted(rootClass.getName()), jaxbException);
            }
        }
    }

    /**
     * Reads the input XML using the {@link Reader} overriding elements namespaces. If an element already has a namespace and
     * a {@link ElementNamespaceOverride} is defined for this element, its namespace is overridden. In case the element has no
     * namespace, new namespace defined in the {@link ElementNamespaceOverride} is added.
     *
     * @param reader input XML {@link Reader}; never null
     * @param elementNamespaceOverrides never null
     * @return deserialized object representation of the XML.
     */
    public T readOverridingNamespace(Reader reader, ElementNamespaceOverride... elementNamespaceOverrides) {
        return readOverridingNamespace(parseXml(reader), elementNamespaceOverrides);
    }

    /**
     * Reads the input XML {@link Document} overriding namespaces. If an element already has a namespace and
     * a {@link ElementNamespaceOverride} is defined for this element, its namespace is overridden. In case the element has no
     * namespace a new namespace defined in the {@link ElementNamespaceOverride} is added.
     *
     * @param document input XML {@link Document}; never null
     * @param elementNamespaceOverrides never null
     * @return deserialized object representation of the XML.
     */
    public T readOverridingNamespace(Document document, ElementNamespaceOverride... elementNamespaceOverrides) {
        try {
            var translatedDocument = overrideNamespaces(document, elementNamespaceOverrides);
            return (T) createUnmarshaller().unmarshal(translatedDocument);
        } catch (JAXBException e) {
            throw new TimefoldXmlSerializationException(
                    ERR_MSG_READ_OVERRIDE_NAMESPACE.formatted(rootClass.getName(), Arrays.toString(elementNamespaceOverrides)),
                    e);
        }
    }

    public Document parseXml(Reader reader) {
        try (reader) {
            var builder = createDocumentBuilderFactory().newDocumentBuilder();
            return builder.parse(new InputSource(reader));
        } catch (ParserConfigurationException e) {
            throw new TimefoldXmlSerializationException("Failed to create a %s instance to parse an XML for a root class (%s)."
                    .formatted(DocumentBuilder.class.getSimpleName(), rootClass.getName()), e);
        } catch (SAXException saxException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to parse an XML for a root class (%s).".formatted(rootClass.getName()), saxException);
        } catch (IOException ioException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to read an XML for a root class (%s).".formatted(rootClass.getName()), ioException);
        }
    }

    private Unmarshaller createUnmarshaller() {
        try {
            return jaxbContext.createUnmarshaller();
        } catch (JAXBException e) {
            throw new TimefoldXmlSerializationException("Failed to create a JAXB %s for a root element class (%s)."
                    .formatted(Unmarshaller.class.getSimpleName(), rootClass.getName()), e);
        }
    }

    public void validate(Document document, String schemaResource) {
        validate(document, readSchemaResource(schemaResource));
    }

    public void validate(Document document, Schema schema) {
        try {
            var validator = createValidator(schema, rootClass);
            validator.validate(new DOMSource(document));
        } catch (SAXException saxException) {
            throw new TimefoldXmlSerializationException(
                    "XML validation failed for a root element class (%s).".formatted(rootClass.getName()), saxException);
        } catch (IOException ioException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to read an XML for a root element class (%s) during validation.".formatted(rootClass.getName()),
                    ioException);
        }
    }

    public void write(T root, Writer writer) {
        write(root, writer, null);
    }

    private void write(T root, Writer writer, @Nullable StreamSource xslt) {
        formatXml(marshall(root), xslt, writer);
    }

    public void writeWithoutNamespaces(T root, Writer writer) {
        try (var xsltInputStream = getClass().getResourceAsStream("removeNamespaces.xslt")) {
            if (xsltInputStream == null) {
                throw new IllegalStateException("Impossible state: Failed to load XSLT stylesheet to remove namespaces.");
            }
            write(root, writer, new StreamSource(xsltInputStream));
        } catch (Exception e) {
            throw new TimefoldXmlSerializationException(String.format(ERR_MSG_WRITE, rootClass.getName()), e);
        }
    }

    private DOMResult marshall(T root) {
        try {
            var domResult = new DOMResult();
            marshaller.marshal(root, domResult);
            return domResult;
        } catch (JAXBException jaxbException) {
            throw new TimefoldXmlSerializationException(String.format(ERR_MSG_WRITE, rootClass.getName()), jaxbException);
        }
    }

    private void formatXml(DOMResult domResult, @Nullable Source transformationTemplate, Writer writer) {
        try {
            var transformerFactory = createTransformerFactory();
            var transformer = transformationTemplate == null ? transformerFactory.newTransformer()
                    : transformerFactory.newTransformer(transformationTemplate);
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", String.valueOf(indentation));
            transformer.transform(new DOMSource(domResult.getNode()), new StreamResult(writer));
        } catch (TransformerException transformerException) {
            throw new TimefoldXmlSerializationException(
                    "Failed to format XML for a root element class (%s).".formatted(rootClass.getName()), transformerException);
        }
    }

    private Document overrideNamespaces(Document document, ElementNamespaceOverride... elementNamespaceOverrides) {
        var elementNamespaceOverridesMap = new HashMap<String, String>();
        for (var namespaceOverride : elementNamespaceOverrides) {
            elementNamespaceOverridesMap.put(namespaceOverride.elementLocalName(), namespaceOverride.namespaceOverride());
        }

        var preOrderNodes = new LinkedList<NamespaceOverride>();
        preOrderNodes.push(new NamespaceOverride(document.getDocumentElement(), null));
        while (!preOrderNodes.isEmpty()) {
            var currentNodeOverride = preOrderNodes.pop();
            var currentNode = currentNodeOverride.node;
            var elementLocalName = currentNode.getLocalName() == null ? currentNode.getNodeName() : currentNode.getLocalName();

            // Is there any override defined for the current node?
            var detectedNamespaceOverride = elementNamespaceOverridesMap.get(elementLocalName);
            var effectiveNamespaceOverride =
                    detectedNamespaceOverride != null ? detectedNamespaceOverride : currentNodeOverride.namespace;

            if (effectiveNamespaceOverride != null) {
                document.renameNode(currentNode, effectiveNamespaceOverride, elementLocalName);
            }

            processChildNodes(currentNode, (childNode -> {
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    preOrderNodes.push(new NamespaceOverride(childNode, effectiveNamespaceOverride));
                }
            }));
        }

        return document;
    }

    private void processChildNodes(Node node, Consumer<Node> nodeConsumer) {
        var childNodes = node.getChildNodes();
        if (childNodes != null) {
            for (var i = 0; i < childNodes.getLength(); i++) {
                var childNode = childNodes.item(i);
                if (childNode != null) {
                    nodeConsumer.accept(childNode);
                }
            }
        }
    }

    private record NamespaceOverride(Node node, @Nullable String namespace) {
    }
}
