package br.com.recargapay.wallet.infrastructure.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Left<L, R> extends Either<L, R> {

  private final L value;

  Left(L value) {
    this.value = value;
  }

  @Override
  public boolean isLeft() {
    return true;
  }

  @Override
  public boolean isRight() {
    return false;
  }

  @Override
  public Optional<L> getLeft() {
    return Optional.of(value);
  }

  @Override
  public Optional<R> getRight() {
    return Optional.empty();
  }

  @Override
  public <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
    return leftMapper.apply(value);
  }

  @Override
  public void apply(Consumer<L> leftConsumer, Consumer<R> rightConsumer) {
    leftConsumer.accept(value);
  }
}
