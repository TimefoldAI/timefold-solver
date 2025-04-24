/**
 * Provided shadow variables work by calculating the topological order
 * of each shadow variable.
 * <br/>
 * The nodes in the graph are paths to each shadow variable, bound to a
 * particular entity instance.
 * <ul>
 * <li>
 * The path `e1:Entity.#id.a` is the source path for the shadow variable `a` on entity e1.
 * </li>
 * <li>
 * If `e2.previous = e1`, then the path `e2:Entity.#id.#previous.a` is an alias path for the shadow
 * variable `a` on e1.
 * </li>
 * <li>
 * The path can have multiple parts; like
 * `e1:Entity.#id.#previous.#previous.a`. In this case,
 * `e1:Entity.#id.#previous` is the parent of
 * `e1:Entity.#id.#previous.#previous`.
 * </li>
 * </ul>
 * The edges in the graph are the aliases and dependencies for each shadow variable:
 * <ul>
 * <li>
 * There is a fixed edge from the parent to each of its children.
 * (i.e. `e1:Entity.#id.#previous` -> `e1:Entity.#id.#previous.a`)
 * </li>
 * <li>
 * There is a fixed edge from the direct dependencies of a shadow variable to the shadow variable.
 * (i.e. `e1:Entity.#id.#previous.readyTime` -> `e1:Entity.#id.#startTime`)
 * </li>
 * <li>
 * There is a dynamic edge from each shadow variable to all its aliases.
 * (i.e. `e1:Entity.#id.startTime` ->
 * `e2:Entity.#id.#previous.startTime`, if e1 is the previous of e2.)
 * </li>
 * </ul>
 * Once the topological order of each node is known, to update from
 * a set of changes:
 * <ol>
 * <li>
 * Pick a changed node with the minimum topological order that was not
 * visited.
 * </li>
 * <li>
 * Update the changed node.
 * </li>
 * <li>
 * If the value of the node changed, marked all its children as changed.
 * </li>
 * </ol>
 */
package ai.timefold.solver.core.impl.domain.variable.declarative;