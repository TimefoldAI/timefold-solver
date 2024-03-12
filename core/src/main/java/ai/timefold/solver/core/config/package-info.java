/**
 * Classes which represent the XML Solver configuration of Timefold.
 * <p>
 * The XML Solver configuration is backwards compatible for all elements,
 * except for elements that require the use of non-public API classes.
 */
@XmlSchema(
        namespace = SolverConfig.XML_NAMESPACE,
        elementFormDefault = XmlNsForm.QUALIFIED)
package ai.timefold.solver.core.config;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;

import ai.timefold.solver.core.config.solver.SolverConfig;
