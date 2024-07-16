package ai.timefold.jpyinterpreter.types.wrappers;

import java.util.Iterator;

record WrappingJavaObjectIterator(Iterator<?> delegate) implements Iterator<JavaObjectWrapper> {

    @Override
    public boolean hasNext() {
        return delegate.hasNext();
    }

    @Override
    public JavaObjectWrapper next() {
        return new JavaObjectWrapper(delegate.next());
    }
}
