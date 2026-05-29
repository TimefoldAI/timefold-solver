/**
 * Indexing of tuples for Bavet join and ifExists nodes: the beta-memory data structures that, given a tuple
 * arriving on one side of a join, find the tuples on the other side that match it. This comment walks the whole
 * mechanism end-to-end — from the joiners a user writes, through the comber and the {@link IndexerFactory},
 * down to the {@link Indexer}s and the nodes that drive them — so it can be understood and modified as a whole.
 *
 * <h2>1. Pipeline overview</h2>
 *
 * A user writes {@code join(other, joiner1, joiner2, ...)} or {@code ifExists(other, joiner1, ...)}. The joiners
 * flow through four stages:
 * <ol>
 * <li><b>Comber</b> ({@code Bi/Tri/Quad/PentaJoinerComber}, {@code BiNeighborhoodsJoinerComber}): splits the
 * joiners into <em>indexing</em> and <em>filtering</em> ones, enforces indexing-before-filtering, merges the
 * indexing joiners into one {@code Default*Joiner}, and <b>reorders it equal-first</b>.</li>
 * <li>{@link IndexerFactory}: from that merged joiner it builds the per-side <em>keys extractors</em> (tuple →
 * composite key) and the <em>indexer structure</em> (the parent→child {@link Indexer} chain, or a unified
 * {@link JoinIndex}).</li>
 * <li><b>Node</b> ({@code AbstractIndexed{Join,IfExists}Node}): holds the indexer(s) and the keys extractors,
 * and on every insert/update/retract indexes its tuple and queries the opposite side.</li>
 * <li>The node propagates the resulting out-tuples (join) or counter changes (ifExists) downstream.</li>
 * </ol>
 *
 * <h2>2. Combers</h2>
 *
 * Each constraint-stream arity has a comber. {@code comb(joiners)} walks the joiner array: a {@code Filtering*Joiner}
 * goes to the filtering list, a {@code Default*Joiner} (indexing) is merged via {@code Default*Joiner.merge} /
 * {@code and} (which <b>append</b>, preserving declared order); an indexing joiner after a filtering one is an error.
 * The result is a {@code mergedJoiner} (used for indexing) and a {@code mergedFiltering} predicate (re-checked per
 * matched pair in the node).
 * <p>
 * <b>Equal-first reorder.</b> Joiners are ANDed, so reordering them cannot change which pairs match, but it lets the
 * indexer chain always put its equal level on top — which is what makes the unified {@link JoinIndex} possible. Each
 * {@code Default*Joiner} has {@code reorderedEqualsFirst()} (built on the shared {@code AbstractJoiner.equalsFirstOrder}
 * permutation), returning itself when already equal-first. The class combers apply it on read in {@code getMergedJoiner()}
 * — which also covers {@code BiJoinerComber.addJoiner} (used by {@code InnerConstraintFactory} for
 * {@code forEachUniquePair}, which appends a {@code lessThan(planningId)}; appending a comparison keeps the joiner
 * equal-first). {@code BiNeighborhoodsJoinerComber} is a record, so it reorders eagerly inside {@code comb()}.
 *
 * <h2>3. Joiners and JoinerType</h2>
 *
 * A {@code Default*Joiner} is three parallel arrays: a left mapping, a {@code JoinerType}, and a right mapping per
 * joiner. {@code JoinerType} is {@code EQUAL}; the comparisons {@code LESS_THAN}/{@code LESS_THAN_OR_EQUAL}/
 * {@code GREATER_THAN}/{@code GREATER_THAN_OR_EQUAL}; or the containments {@code CONTAINING}/{@code CONTAINED_IN}/
 * {@code CONTAINING_ANY_OF}. A pair matches when, for every joiner, {@code joinerType.matches(leftMapping(left),
 * rightMapping(right))} holds. {@code JoinerType.flip()} swaps a comparison's direction ({@code LESS_THAN} ↔
 * {@code GREATER_THAN}); {@code EQUAL.flip() == EQUAL}. The flip is why a tuple can query the opposite side's index
 * with its own key (see §7).
 *
 * <h2>4. Composite keys</h2>
 *
 * A keys extractor turns a tuple into one key per indexer level. {@link CompositeKey} has exactly two
 * implementations for bi-morphic call sites: {@link BiCompositeKey} (two levels) and {@link MegaCompositeKey} (zero,
 * or three-plus). {@link CompositeKey#none()} is the no-joiner key; {@link CompositeKey#of(Object)} returns the key
 * <em>itself</em> (no wrapper) for a single level; {@link CompositeKey#of(Object, Object)} and
 * {@link CompositeKey#ofMany(Object[])} build the multi-level keys. A single indexer level reads its key with
 * {@link SingleKeyUnpacker} (the composite key <em>is</em> the key); a level inside a multi-level key reads it with
 * {@link CompositeKeyUnpacker} {@code (id)} ({@code compositeKey.get(id)}). When several consecutive equal joiners are
 * merged into one level (§5), that level's key is itself a tuple of the merged values: {@code Pair}, {@code Triple},
 * {@code Quadruple} or, for five-plus, an {@link IndexerKey} array wrapper.
 *
 * <h2>5. IndexerFactory</h2>
 *
 * Because the comber reorders equal-first, every joiner reaching {@link IndexerFactory} has all its {@code EQUAL}
 * joiners as one leading run; {@code equalPrefixLength} records its length. The level model is therefore: one merged
 * equal-prefix level (when {@code equalPrefixLength >= 1}) followed by one single-joiner level per remaining
 * comparison/containing joiner ({@code levelEndIndices()} encodes the boundaries). The keys extractors
 * ({@code buildUniLeftKeysExtractor}/{@code buildBiLeftKeysExtractor}/...; {@code buildRightKeysExtractor}) build the
 * merged-prefix key once (raw / {@code Pair} / {@code Triple} / {@code Quadruple} / {@link IndexerKey}) and then one
 * single key per trailing level.
 * <p>
 * {@code buildIndexerChain(isLeftBridge, fromLevelInclusive, backendSupplier)} builds the parent→child
 * {@link Indexer} chain children-first: one indexer per level, each reading its key with a {@link CompositeKeyUnpacker}
 * (or {@link SingleKeyUnpacker} for the leaf of a single-level chain), the leaf delegating to an
 * {@link IndexerBackend}. The level ids are anchored to the full chain, so a suffix built with
 * {@code fromLevelInclusive == 1} keeps using {@code CompositeKeyUnpacker(1)} for its top. For a right bridge the
 * joiner type is flipped (§3). The backend is a {@link LinkedListIndexerBackend} normally, or a
 * {@link RandomAccessIndexerBackend} when random access is required (§9); the all-equal leaf is fused into a single map
 * by {@link FusedEqualLinkedListIndexer} / {@link FusedEqualRandomAccessIndexer}. {@code isJoinIndexEligible()} and
 * {@code buildJoinIndex()} drive the unified path (§7).
 *
 * <h2>6. Indexers</h2>
 *
 * {@link Indexer} is a sealed family. A non-leaf indexer is one level: it reads its key with its
 * {@link KeyUnpacker}, navigates its map to a downstream indexer, and delegates. The leaf is an
 * {@link IndexerBackend} holding the actual tuple list.
 * <ul>
 * <li>{@link EqualIndexer}: a {@code HashMap} (tuned 16 / 0.5f) keyed by the equal key; no {@code computeIfAbsent},
 * to avoid hot-path lambdas. {@code forEach(key)} visits exactly the matching downstream.</li>
 * <li>{@link ComparisonIndexer}: a {@code TreeMap}. {@code GREATER_THAN}(_OR_EQUAL) reverse the comparator so a range
 * query always iterates from the start until a boundary, avoiding sub-map allocation; {@code forEach(key)} visits
 * every downstream on the matching side of the boundary.</li>
 * <li>{@link ContainingIndexer} / {@link ContainedInIndexer} / {@link ContainingAnyOfIndexer}: set-membership joins,
 * whose modify key (a collection) differs from the query key.</li>
 * <li>{@link IndexerBackend} ({@link LinkedListIndexerBackend}, {@link RandomAccessIndexerBackend}): the leaf tuple
 * list. {@link FusedEqualLinkedListIndexer} / {@link FusedEqualRandomAccessIndexer} collapse an all-equal leaf and its
 * backend into one map, removing a per-key object and a dispatch.</li>
 * </ul>
 * An indexer is {@link Indexer#isRemovable() removable} once empty and balanced (every {@code put} had a
 * {@code remove}); parents drop empty children to keep the maps small.
 *
 * <h2>7. Unified vs non-unified paths</h2>
 *
 * A join/ifExists node always queries the OPPOSITE side with its OWN key: an arriving left tuple looks up right
 * tuples, and vice versa. There are two ways to hold those two sides.
 * <p>
 * <b>Non-unified</b> (no equal joiner): the node keeps two parallel chains, {@code indexerLeft} and
 * {@code indexerRight}, built from the same factory ({@code indexerRight} flipped). The same key is hashed in two
 * byte-identical top maps — the cross-side {@code forEach} is a second hash navigation. This path runs for pure
 * comparison/containing joins (and the random-access dataset, §9).
 * <p>
 * <b>Unified</b> (equal-bearing; the common case): because an {@code EQUAL} match is
 * {@code Objects.equals(leftKey, rightKey)} — exactly how a {@code HashMap} groups entries — matching left and right
 * tuples have the same equal-prefix key and so belong in the SAME bucket; co-location <em>is</em> the match. The node
 * keeps one {@link JoinIndex}: a map from the equal-prefix key to a {@link JoinBucket} holding a per-side downstream
 * {@link Indexer} (a bare tuple list for a pure-equal join, or the per-side comparison/containing suffix sub-chain
 * otherwise, the right side flipped). An insert does ONE top-level lookup ({@link JoinIndex#getOrCreateBucket}),
 * adds the tuple to its side, and iterates the other side of the same bucket. {@link IndexerFactory} selects this via
 * {@code isJoinIndexEligible()} ({@code equalPrefixLength >= 1 && !requiresRandomAccess}); the node branches on a
 * {@code useJoinIndex} flag. Only the shared equal lookup is unified; a comparison/containing suffix is still walked
 * per side (ranges and containments cannot co-locate).
 *
 * <h2>8. How nodes interact with the index</h2>
 *
 * {@code AbstractIndexedJoinNode} and {@code AbstractIndexedIfExistsNode} (whose non-indexing logic mirrors the
 * {@code AbstractUnindexed*} siblings) store per input tuple: its composite key, the {@code ListEntry} returned by the
 * index {@code put} (for O(1) removal), the out-tuple list (join) / counter entry (ifExists), and — on the unified
 * path — the resolved {@link JoinBucket}.
 * <ul>
 * <li><b>insert</b>: extract the key; {@code put} into this side; iterate the opposite side and propagate. Unified:
 * one bucket lookup, then cache the bucket on the tuple.</li>
 * <li><b>update, key unchanged</b>: re-iterate the opposite side for the (possibly changed) filtering result. Unified:
 * uses the cached bucket, so <em>no</em> lookup.</li>
 * <li><b>update, key changed</b>: remove from the old key, then insert at the new key. Unified: the cached old bucket
 * needs no lookup; one lookup for the new key.</li>
 * <li><b>retract</b>: remove from the index and retract the out-tuples. Unified: uses the cached bucket; the bucket is
 * dropped only when {@link JoinBucket#isEmpty() both sides are empty} ({@link JoinIndex#removeBucketIfEmpty}), so a
 * cached bucket reference never goes stale.</li>
 * </ul>
 * ifExists keeps an {@code ExistsCounter} per left tuple (its {@code countRight} is the number of matching right
 * tuples; non-filtering reads it directly from {@code rightSize}/{@code Indexer.size}); when filtering, a
 * {@code FilteringTracker} links each surviving left/right pair so the count can be maintained incrementally. The
 * counter/propagation logic lives in the {@code AbstractIfExistsNode} base and is identical on both paths.
 *
 * <h2>9. Neighborhoods</h2>
 *
 * Neighborhood (move-enumerating) streams reuse the very same join/ifExists nodes: they convert their
 * {@code DefaultBiNeighborhoodsJoiner} to a {@code DefaultBiJoiner} ({@code toBiJoiner()}, also reordered equal-first)
 * and use {@link LinkedListIndexerBackend}. The exception is the {@code UniRightDataset} right-of-a-join store, which
 * needs to pick matches uniformly at random: it builds a single random-access {@link Indexer} (via
 * {@code buildIndexer}, {@code requiresRandomAccess == true}) over a {@link RandomAccessIndexerBackend} /
 * {@link FusedEqualRandomAccessIndexer} and is queried with {@code Indexer.randomIterator} (see
 * {@code DefaultUniqueRandomIterator} / {@code FilteredUniqueRandomIterator}). Because random access is single-indexer,
 * it is never unified.
 *
 * <h2>10. Modifying the mechanism</h2>
 *
 * <ul>
 * <li><b>Add a {@code JoinerType}</b>: handle it in {@code JoinerType.matches}/{@code flip}, add an {@link Indexer}
 * case in {@code IndexerFactory.buildIndexerPart}, and decide whether it can ever be the unified top (only
 * {@code EQUAL} can).</li>
 * <li><b>Add an {@link Indexer}</b>: implement the sealed {@link Indexer} interface and extend its {@code permits}
 * clause.</li>
 * <li><b>Keep the node pairs in sync</b>: a change to {@code AbstractIndexedJoinNode} usually needs the same change in
 * {@code AbstractUnindexedJoinNode} (and likewise for ifExists), unless it is purely about indexing.</li>
 * <li><b>The cost model</b>: the hot cost is (a) {@code HashMap.get} on the user key and (c) the non-unified path's
 * redundant second lookup of that key; equal-first reorder (§2) plus the unified {@link JoinIndex} (§7) cut the second
 * lookup, and the cached bucket (§8) removes the lookup from same-key updates and retracts entirely.</li>
 * </ul>
 */
package ai.timefold.solver.core.impl.bavet.common.index;
