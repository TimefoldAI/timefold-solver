package ai.timefold.solver.core.impl.bavet.common;

public interface BavetStreamBinaryOperation<Stream_ extends BavetStream> {

    Stream_ getLeftParent();

    Stream_ getRightParent();

}
