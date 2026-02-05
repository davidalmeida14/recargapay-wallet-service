package br.com.recargapay.wallet.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests. DataJpaTest with real database (Replace.NONE).
 *
 * <p>Tag: {@code integration}. Profiles: {@code integration}, {@code test}.
 *
 * <p>Scans wallet persistence and application layers for beans required by repositories.
 */
@Tag("integration")
@ActiveProfiles({"integration", "test"})
@DataJpaTest
@ComponentScan(
    basePackages = {
      "br.com.recargapay.wallet.infrastructure.persistence",
      "br.com.recargapay.wallet.infrastructure.common",
    })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true",
      "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
      "spring.main.allow-bean-definition-overriding=true",
    })
public abstract class IntegrationTest {}
