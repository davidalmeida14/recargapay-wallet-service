package br.com.recargapay.wallet.infrastructure.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Either<L, R> {

  public abstract boolean isLeft();

  public abstract boolean isRight();

  public abstract Optional<L> getLeft();

  public abstract Optional<R> getRight();

  public abstract <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper);

  public abstract void apply(Consumer<L> leftConsumer, Consumer<R> rightConsumer);

  public static <L, R> Either<L, R> left(L value) {
    return new Left<>(value);
  }

  public static <L, R> Either<L, R> right(R value) {
    return new Right<>(value);
  }

  public <T> Either<L, T> map(Function<R, T> mapper) {
    return fold(Either::left, r -> Either.right(mapper.apply(r)));
  }

  public <T> Either<L, T> flatMap(Function<R, Either<L, T>> mapper) {
    return fold(Either::left, mapper);
  }

  public Either<L, R> peek(Consumer<R> consumer) {
    if (isRight()) {
      getRight().ifPresent(consumer);
    }
    return this;
  }

  public Either<L, R> peekLeft(Consumer<L> consumer) {
    if (isLeft()) {
      getLeft().ifPresent(consumer);
    }
    return this;
  }
}
