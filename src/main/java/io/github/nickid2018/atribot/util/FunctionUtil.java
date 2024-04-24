package io.github.nickid2018.atribot.util;

import lombok.SneakyThrows;

import java.util.function.*;

public class FunctionUtil {

    @FunctionalInterface
    public interface FunctionWithException<S, T, E extends Throwable> {
        T apply(S s) throws E;
    }

    @FunctionalInterface
    public interface ConsumerWithException<T, E extends Throwable> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface SupplierWithException<T, E extends Throwable> {
        T get() throws E;
    }

    @FunctionalInterface
    public interface RunnableWithException<E extends Throwable> {
        void run() throws E;
    }

    public static Runnable sneakyThrowsRunnable(RunnableWithException<?> runnable) {
        return new Runnable() {
            @Override
            @SneakyThrows
            public void run() {
                runnable.run();
            }
        };
    }

    public static Runnable tryOrElse(RunnableWithException<?> runnable, Runnable exceptionCase) {
        return tryOrElse(runnable, e -> exceptionCase.run());
    }

    public static Runnable tryOrElse(RunnableWithException<?> runnable, Consumer<Throwable> exceptionCase) {
        return () -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                exceptionCase.accept(e);
            }
        };
    }

    public static <S, T> Function<S, T> tryUntil(FunctionWithException<S, T, ?> function, int times) {
        return s -> {
            for (int i = 0; i < times; i++) {
                try {
                    return function.apply(s);
                } catch (Throwable e) {
                    if (i == times - 1)
                        throw new RuntimeException(e);
                }
            }
            return null;
        };
    }

    public static <S, T> Function<S, T> sneakyThrowsFunc(FunctionWithException<S, T, ?> function) {
        return new Function<>() {
            @Override
            @SneakyThrows
            public T apply(S s) {
                return function.apply(s);
            }
        };
    }

    public static <S, T> Function<S, T> tryOrElse(FunctionWithException<S, T, ?> function, Function<S, T> exceptionCase) {
        return tryOrElse(function, (s, e) -> exceptionCase.apply(s));
    }

    public static <S, T> Function<S, T> tryOrElse(FunctionWithException<S, T, ?> function, BiFunction<S, Throwable, T> exceptionCase) {
        return s -> {
            try {
                return function.apply(s);
            } catch (Throwable e) {
                return exceptionCase.apply(s, e);
            }
        };
    }

    public static <T> Consumer<T> sneakyThrowsConsumer(ConsumerWithException<T, ? extends Throwable> consumer) {
        return new Consumer<T>() {
            @Override
            @SneakyThrows
            public void accept(T t) {
                consumer.accept(t);
            }
        };
    }

    public static <T> Consumer<T> tryOrElse(ConsumerWithException<T, ?> consumer, Consumer<T> exceptionCase) {
        return tryOrElse(consumer, (t, e) -> exceptionCase.accept(t));
    }

    public static <T> Consumer<T> tryOrElse(ConsumerWithException<T, ?> consumer, BiConsumer<T, Throwable> exceptionCase) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                exceptionCase.accept(t, e);
            }
        };
    }

    public static <T> Supplier<T> tryUntil(SupplierWithException<T, ?> supplier, int times) {
        return () -> {
            for (int i = 0; i < times; i++) {
                try {
                    return supplier.get();
                } catch (Throwable e) {
                    if (i == times - 1)
                        throw new RuntimeException(e);
                }
            }
            return null;
        };
    }

    public static <T> Supplier<T> tryUntil(SupplierWithException<T, ?> supplier, int times, Function<Throwable, T> exceptionCase) {
        return () -> {
            for (int i = 0; i < times; i++) {
                try {
                    return supplier.get();
                } catch (Throwable e) {
                    if (i == times - 1)
                        return exceptionCase.apply(e);
                }
            }
            return null;
        };
    }

    public static <T> Supplier<T> sneakyThrowsSupplier(SupplierWithException<T, ?> supplier) {
        return new Supplier<T>() {
            @Override
            @SneakyThrows
            public T get() {
                return supplier.get();
            }
        };
    }

    public static <T> Supplier<T> tryOrElse(SupplierWithException<T, ?> supplier, Supplier<T> exceptionCase) {
        return tryOrElse(supplier, e -> exceptionCase.get());
    }

    public static <T> Supplier<T> tryOrElse(SupplierWithException<T, ?> supplier, Function<Throwable, T> exceptionCase) {
        return () -> {
            try {
                return supplier.get();
            } catch (Throwable e) {
                return exceptionCase.apply(e);
            }
        };
    }
}
