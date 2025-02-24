/**
 * Contains streams that serve as bridges.
 * Fore bridges go before the stream they bridge,
 * while aft bridges go after.
 * <p>
 * Aft bridges are node-shared,
 * therefore their {@link java.lang.Object#equals(Object)} and {@link java.lang.Object#hashCode()} (java.lang.Object)}
 * methods are overridden to reference the bridged stream,
 * which carries all the equality data.
 * <p>
 * Fore bridges are node-shared through their child stream
 * and therefore the equality logic can reside there entirely.
 */
package ai.timefold.solver.core.impl.score.stream.bavet.common.bridge;