package br.com.recargapay.wallet.infrastructure.common;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Right<L, R> extends Either<L, R> {

  private final R value;

  Right(R value) {
    this.value = value;
  }

  @Override
  public boolean isLeft() {
    return false;
  }

  @Override
  public boolean isRight() {
    return true;
  }

  @Override
  public Optional<L> getLeft() {
    return Optional.empty();
  }

  @Override
  public Optional<R> getRight() {
    return Optional.of(value);
  }

  @Override
  public <T> T fold(Function<L, T> leftMapper, Function<R, T> rightMapper) {
    return rightMapper.apply(value);
  }

  @Override
  public void apply(Consumer<L> leftConsumer, Consumer<R> rightConsumer) {
    rightConsumer.accept(value);
  }
}
