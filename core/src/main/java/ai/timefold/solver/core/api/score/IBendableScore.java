package ai.timefold.solver.core.api.score;

import org.jspecify.annotations.NullMarked;

/**
 * Bendable score is a {@link Score} whose {@link #hardLevelsSize()} and {@link #softLevelsSize()}
 * are only known at runtime.
 *
 * <p>
 * Interfaces in Timefold are usually not prefixed with "I".
 * However, the conflict in name with its implementation ({@link BendableScore}) made this necessary.
 * All the other options were considered worse, some even harmful.
 * This is a minor issue, as users will access the implementation and not the interface anyway.
 *
 * @param <Score_> the actual score type to allow addition, subtraction and other arithmetic
 */
@NullMarked
public sealed interface IBendableScore<Score_ extends IBendableScore<Score_>>
        extends Score<Score_> permits BendableBigDecimalScore, BendableScore {

    /**
     * The sum of this and {@link #softLevelsSize()} equals {@link #levelsSize()}.
     *
     * @return {@code >= 0} and {@code <} {@link #levelsSize()}
     */
    int hardLevelsSize();

    /**
     * The sum of {@link #hardLevelsSize()} and this equals {@link #levelsSize()}.
     *
     * @return {@code >= 0} and {@code <} {@link #levelsSize()}
     */
    int softLevelsSize();

    /**
     * @return {@link #hardLevelsSize()} + {@link #softLevelsSize()}
     */
    default int levelsSize() {
        return hardLevelsSize() + softLevelsSize();
    }

}
