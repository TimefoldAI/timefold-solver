package ai.timefold.jpyinterpreter.util.function;

public interface QuadFunction<A_, B_, C_, D_, Result_> {
    Result_ apply(A_ a, B_ b, C_ c, D_ d);
}
