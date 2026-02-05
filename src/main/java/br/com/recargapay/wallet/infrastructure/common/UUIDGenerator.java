package br.com.recargapay.wallet.infrastructure.common;

import com.fasterxml.uuid.Generators;

import java.util.UUID;

public class UUIDGenerator {

  public static UUID generate() {
    return Generators.timeBasedEpochRandomGenerator().generate();
  }
}
