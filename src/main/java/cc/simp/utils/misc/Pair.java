package cc.simp.utils.misc;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public abstract class Pair<A, B> implements Serializable {

    public static <A, B> Pair<A, B> of(A a, B b) {
        return ImmutablePair.of(a, b);
    }

    public static <A> Pair<A, A> of(A a) {
        return ImmutablePair.of(a, a);
    }

    public abstract A getFirst();

    public abstract B getSecond();

    public abstract <R> R apply(BiFunction<? super A, ? super B, ? extends R> func);

    public abstract void use(BiConsumer<? super A, ? super B> func);

    @Override
    public int hashCode() {
        return Objects.hash(getFirst(), getSecond());
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) return true;
        if (that instanceof Pair<?, ?>) {
            final Pair<?, ?> other = (Pair<?, ?>) that;
            return Objects.equals(getFirst(), other.getFirst()) && Objects.equals(getSecond(), other.getSecond());
        }
        return false;
    }

    public static final class ImmutablePair<A, B> extends Pair<A, B> {
        private final A a;
        private final B b;

        ImmutablePair(A a, B b) {
            this.a = a;
            this.b = b;
        }

        public static <A, B> ImmutablePair<A, B> of(A a, B b) {
            return new ImmutablePair<>(a, b);
        }

        public Pair<A, A> pairOfFirst() {
            return Pair.of(a);
        }

        public Pair<B, B> pairOfSecond() {
            return Pair.of(b);
        }

        @Override
        public A getFirst() {
            return a;
        }

        @Override
        public B getSecond() {
            return b;
        }


        @Override
        public <R> R apply(BiFunction<? super A, ? super B, ? extends R> func) {
            return func.apply(a, b);
        }

        @Override
        public void use(BiConsumer<? super A, ? super B> func) {
            func.accept(a, b);
        }
    }

}
