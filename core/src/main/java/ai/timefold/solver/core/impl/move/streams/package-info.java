/**
 * This package contains a work-in-progress implementation of a major new feature, codenamed "Move Streams".
 * Nothing in this package is considered stable, and it will change on a regular basis.
 * As the feature matures, the package structure will be refactored,
 * classes and interfaces moved to other packages,
 * and the public API will be stabilized.
 * <p>
 * For all intents and purposes, nothing in this package exists;
 * no stable solver code should depend on any of this.
 * As the feature matures, more and more of it will be integrated into the stable solver,
 * when sufficiently stable and tested.
 * At that point, the feature will be publicly announced and treated as a standard preview feature.
 * <p>
 * Typically, this work would be done on a separate branch.
 * Unfortunately, the solver moves forward at a fast pace,
 * and the branch would require frequent and significant conflict resolution.
 * Therefore we are doing this work in the main development branch,
 * but entirely separately from the stable solver.
 */
package ai.timefold.solver.core.impl.move.streams;