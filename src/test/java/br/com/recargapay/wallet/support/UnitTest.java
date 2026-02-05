package br.com.recargapay.wallet.support;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for unit tests. Uses Mockito only, no Spring context.
 *
 * <p>Tag: {@code unit}. Profile: {@code unit}.
 */
@Tag("unit")
@ActiveProfiles("unit")
@ExtendWith(MockitoExtension.class)
public abstract class UnitTest {}
