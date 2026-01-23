package ai.timefold.solver.core.impl.bavet.common.index;

record SingleKeyUnpacker<Key_>() implements KeyUnpacker<Key_> {

    @SuppressWarnings("unchecked")
    @Override
    public Key_ apply(Object compositeKey) {
        return (Key_) compositeKey;
    }

}
