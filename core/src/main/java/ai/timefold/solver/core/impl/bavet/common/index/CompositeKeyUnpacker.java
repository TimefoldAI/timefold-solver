package ai.timefold.solver.core.impl.bavet.common.index;

record CompositeKeyUnpacker<Key_>(int id) implements KeyUnpacker<Key_> {

    @Override
    public Key_ apply(Object compositeKey) {
        return ((CompositeKey) compositeKey).get(id);
    }

}
