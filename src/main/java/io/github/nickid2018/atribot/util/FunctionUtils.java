package io.github.nickid2018.atribot.util;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class FunctionUtils {

    @FunctionalInterface
    public interface FunctionWithException<S, T, E extends Throwable> {
        T apply(S s) throws E;
    }

    @FunctionalInterface
    public interface ConsumerWithException<T, E extends Throwable> {
        void accept(T t) throws E;
    }

    public static <S, T> Function<S, T> noException(FunctionWithException<S, T, ?> function) {
        return noException(function, RuntimeException::new);
    }

    public static <S, T> Function<S, T> noException(FunctionWithException<S, T, ?> function,
                                                    Function<Throwable, RuntimeException> exceptionMapper) {
        return noExceptionOrElse(function, (s, e) -> {
            throw exceptionMapper.apply(e);
        });
    }

    public static <S, T> Function<S, T> noExceptionOrElse(FunctionWithException<S, T, ?> function,
                                                          Function<S, T> exceptionCase) {
        return noExceptionOrElse(function, (s, e) -> exceptionCase.apply(s));
    }

    public static <S, T> Function<S, T> noExceptionOrElse(FunctionWithException<S, T, ?> function,
                                                          BiFunction<S, Throwable, T> exceptionCase) {
        return s -> {
            try {
                return function.apply(s);
            } catch (Throwable e) {
                return exceptionCase.apply(s, e);
            }
        };
    }

    public static <T> Consumer<T> noException(ConsumerWithException<T, ?> consumer) {
        return noException(consumer, RuntimeException::new);
    }

    public static <T> Consumer<T> noException(ConsumerWithException<T, ?> consumer,
                                              Function<Throwable, RuntimeException> exceptionMapper) {
        return noExceptionOrElse(consumer, (t, e) -> {
            throw exceptionMapper.apply(e);
        });
    }

    public static <T> Consumer<T> noExceptionOrElse(ConsumerWithException<T, ?> consumer,
                                                    Consumer<T> exceptionCase) {
        return noExceptionOrElse(consumer, (t, e) -> exceptionCase.accept(t));
    }

    public static <T> Consumer<T> noExceptionOrElse(ConsumerWithException<T, ?> consumer,
                                                    BiConsumer<T, Throwable> exceptionCase) {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Throwable e) {
                exceptionCase.accept(t, e);
            }
        };
    }
}
