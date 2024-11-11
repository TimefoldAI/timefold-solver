package ai.timefold.solver.core.impl.bavet.common;

public interface BavetStreamBinaryOperation<Stream_ extends BavetStream> {

    /**
     * @return An instance of fore bridge.
     */
    Stream_ getLeftParent();

    /**
     * @return An instance of fore bridge.
     */
    Stream_ getRightParent();

}
