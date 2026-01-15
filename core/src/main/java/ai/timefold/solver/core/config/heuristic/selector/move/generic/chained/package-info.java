/**
 * @deprecated Chained variable is deprecated. Use {@link ai.timefold.solver.core.api.domain.variable.PlanningListVariable list
 *             variable} instead.
 */
@Deprecated(forRemoval = true, since = "1.31.0")
@XmlSchema(
        namespace = SolverConfig.XML_NAMESPACE,
        elementFormDefault = XmlNsForm.QUALIFIED)
package ai.timefold.solver.core.config.heuristic.selector.move.generic.chained;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;

import ai.timefold.solver.core.config.solver.SolverConfig;
