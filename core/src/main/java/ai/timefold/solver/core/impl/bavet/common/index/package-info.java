/**
 * Indexing for Bavet join and ifExists nodes.
 *
 * <h2>1. End-to-end flow</h2>
 *
 * For {@code join(...)} and {@code ifExists(...)}, joiners move through this pipeline:
 * <ol>
 * <li><b>Comber</b> ({@code Bi/Tri/Quad/PentaJoinerComber}, {@code BiNeighborhoodsJoinerComber}):
 * separates indexing and filtering joiners, enforces indexing-before-filtering, and keeps indexing joiners equal-first.</li>
 * <li><b>{@link ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory}</b>:
 * creates key extractors and index structures used by both sides of the node.</li>
 * <li><b>Indexed node</b> ({@code AbstractIndexed{Join,IfExists}Node}):
 * indexes tuples on insert/update/retract, queries the opposite side, and propagates results downstream.</li>
 * </ol>
 *
 * <h2>2. API contracts between components</h2>
 *
 * <ul>
 * <li><b>Joiners:</b> {@code Default*Joiner} plus {@link ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType}
 * define matching semantics; filtering joiners are evaluated after index lookup.</li>
 * <li><b>Keys:</b> extractors produce {@link ai.timefold.solver.core.impl.bavet.common.index.CompositeKey} values
 * consumed consistently by both sides.</li>
 * <li><b>Index abstraction:</b> {@link ai.timefold.solver.core.impl.bavet.common.index.Indexer}
 * exposes put/remove/iterate semantics regardless of index shape.</li>
 * <li><b>Storage backend:</b> leaf storage is provided by
 * {@link ai.timefold.solver.core.impl.bavet.common.index.LeafIndexer} implementations.</li>
 * </ul>
 *
 * <h2>3. Cross-side communication model</h2>
 *
 * A tuple is always looked up against the opposite side using its own key.
 *
 * <ul>
 * <li><b>Unified path:</b> if there is an equal prefix and random access is not required,
 * {@link ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory} builds a shared
 * {@link ai.timefold.solver.core.impl.bavet.common.index.JoinIndex}. Matching left/right tuples co-locate in the same
 * {@link ai.timefold.solver.core.impl.bavet.common.index.JoinBucket} and communicate there.</li>
 * <li><b>Non-unified path:</b> otherwise each side has its own index chain; communication happens by querying the
 * opposite chain.</li>
 * </ul>
 *
 * <h2>4. Node lifecycle interaction</h2>
 *
 * On insert/update/retract, indexed nodes keep tuple state (key, index entry, propagation state) and synchronize it
 * with the index. ifExists nodes keep per-left counters and update downstream state from index query results.
 *
 * <h2>5. Neighborhood integration</h2>
 *
 * Neighborhood streams reuse the same join/ifExists indexing pipeline after converting neighborhood joiners to regular
 * bi joiners. Random-iterator datasets use random-access index backends and stay on the non-unified path.
 *
 * <h2>6. Extension points</h2>
 *
 * When adding a new {@code JoinerType} or {@link ai.timefold.solver.core.impl.bavet.common.index.Indexer}, update:
 * matching semantics, indexer creation in {@link ai.timefold.solver.core.impl.bavet.common.index.IndexerFactory}, and
 * indexed/unindexed node pairs where behavior must stay aligned.
 */
package ai.timefold.solver.core.impl.bavet.common.index;
