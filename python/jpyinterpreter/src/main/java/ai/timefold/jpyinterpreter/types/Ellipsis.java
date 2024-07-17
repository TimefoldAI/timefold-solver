package ai.timefold.jpyinterpreter.types;

import ai.timefold.jpyinterpreter.builtins.GlobalBuiltins;
import ai.timefold.solver.core.impl.domain.solution.cloner.PlanningImmutable;

public class Ellipsis extends AbstractPythonLikeObject implements PlanningImmutable {
    public static final Ellipsis INSTANCE;
    public static final PythonLikeType ELLIPSIS_TYPE = new PythonLikeType("EllipsisType", Ellipsis.class);
    public static final PythonLikeType $TYPE = ELLIPSIS_TYPE;

    static {
        INSTANCE = new Ellipsis();

        GlobalBuiltins.addBuiltinConstant("Ellipsis", INSTANCE);
    }

    private Ellipsis() {
        super(ELLIPSIS_TYPE);
    }

    @Override
    public PythonString $method$__str__() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        return "NotImplemented";
    }
}
