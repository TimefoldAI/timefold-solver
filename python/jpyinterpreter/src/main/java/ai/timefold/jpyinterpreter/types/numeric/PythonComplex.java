package ai.timefold.jpyinterpreter.types.numeric;

import java.util.List;

import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class PythonComplex extends AbstractPythonLikeObject implements PythonNumber, PlanningImmutable {
    final PythonNumber real;
    final PythonNumber imaginary;

    public final static PythonLikeType COMPLEX_TYPE = new PythonLikeType("complex", PythonComplex.class, List.of(NUMBER_TYPE));

    static {
        PythonOverloadImplementor.deferDispatchesFor(PythonComplex::registerMethods);
    }

    public PythonComplex(PythonNumber real, PythonNumber imaginary) {
        super(COMPLEX_TYPE);
        this.real = real;
        this.imaginary = imaginary;
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // TODO
        return COMPLEX_TYPE;
    }

    public PythonComplex valueOf(PythonNumber real, PythonNumber imaginary) {
        return new PythonComplex(real, imaginary);
    }

    @Override
    public Number getValue() {
        return (real.getValue().doubleValue() * real.getValue().doubleValue()) +
                (imaginary.getValue().doubleValue() * imaginary.getValue().doubleValue());
    }

    public PythonNumber getReal() {
        return real;
    }

    public PythonNumber getImaginary() {
        return imaginary;
    }
}
