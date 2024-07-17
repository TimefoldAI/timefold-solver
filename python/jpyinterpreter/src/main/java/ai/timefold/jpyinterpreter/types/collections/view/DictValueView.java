package ai.timefold.jpyinterpreter.types.collections.view;

import java.util.Collection;

import ai.timefold.jpyinterpreter.PythonBinaryOperator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonOverloadImplementor;
import ai.timefold.jpyinterpreter.PythonUnaryOperator;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.AbstractPythonLikeObject;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.DelegatePythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.util.IteratorUtils;

public class DictValueView extends AbstractPythonLikeObject {
    public final static PythonLikeType $TYPE = BuiltinTypes.DICT_VALUE_VIEW_TYPE;

    final PythonLikeDict mapping;
    final Collection<PythonLikeObject> valueCollection;

    static {
        PythonOverloadImplementor.deferDispatchesFor(DictValueView::registerMethods);
    }

    private static PythonLikeType registerMethods() throws NoSuchMethodException {
        // Unary
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.LENGTH,
                DictValueView.class.getMethod("getValuesSize"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.ITERATOR,
                DictValueView.class.getMethod("getValueIterator"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.REVERSED,
                DictValueView.class.getMethod("getReversedValueIterator"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.AS_STRING,
                DictValueView.class.getMethod("toRepresentation"));
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addUnaryMethod(PythonUnaryOperator.REPRESENTATION,
                DictValueView.class.getMethod("toRepresentation"));

        // Binary
        BuiltinTypes.DICT_VALUE_VIEW_TYPE.addBinaryMethod(PythonBinaryOperator.CONTAINS,
                DictValueView.class.getMethod("containsValue", PythonLikeObject.class));

        return BuiltinTypes.DICT_VALUE_VIEW_TYPE;
    }

    public DictValueView(PythonLikeDict mapping) {
        super(BuiltinTypes.DICT_VALUE_VIEW_TYPE);
        this.mapping = mapping;
        this.valueCollection = mapping.delegate.values();
        $setAttribute("mapping", mapping);
    }

    public PythonInteger getValuesSize() {
        return PythonInteger.valueOf(valueCollection.size());
    }

    public DelegatePythonIterator<PythonLikeObject> getValueIterator() {
        return new DelegatePythonIterator<>(valueCollection.iterator());
    }

    public PythonBoolean containsValue(PythonLikeObject value) {
        return PythonBoolean.valueOf(valueCollection.contains(value));
    }

    public DelegatePythonIterator<PythonLikeObject> getReversedValueIterator() {
        return new DelegatePythonIterator<>(IteratorUtils.iteratorMap(mapping.reversed(), mapping::get));
    }

    public PythonString toRepresentation() {
        return PythonString.valueOf(toString());
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("dict_values([");

        for (PythonLikeObject value : valueCollection) {
            out.append(UnaryDunderBuiltin.REPRESENTATION.invoke(value));
            out.append(", ");
        }
        out.delete(out.length() - 2, out.length());
        out.append("])");

        return out.toString();
    }
}
