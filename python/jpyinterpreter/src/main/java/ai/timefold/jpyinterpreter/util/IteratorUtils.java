package ai.timefold.jpyinterpreter.util;

import java.util.Iterator;
import java.util.function.Function;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public class IteratorUtils {

    public static <Mapped_> Iterator<PythonLikeObject> iteratorMap(final Iterator<Mapped_> source,
            final Function<Mapped_, PythonLikeObject> mapFunction) {
        return new Iterator<PythonLikeObject>() {
            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public PythonLikeObject next() {
                return mapFunction.apply(source.next());
            }
        };
    }
}
