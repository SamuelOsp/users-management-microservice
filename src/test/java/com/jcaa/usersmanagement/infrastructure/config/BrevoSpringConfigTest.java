package com.jcaa.usersmanagement.infrastructure.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jcaa.usersmanagement.infrastructure.adapter.email.BrevoConfig;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BrevoSpringConfigTest {

  private final BrevoSpringConfig springConfig = new BrevoSpringConfig();

  @Test
  @DisplayName("Arma la configuración de Brevo con los tiempos límite recibidos")
  void buildsConfig() {
    final BrevoConfig config = springConfig.brevoConfig(
        "https://api.brevo.com", "xkeysib-prueba", "remitente@example.com", "Gestion de Usuarios",
        Duration.ofSeconds(2), Duration.ofSeconds(5), 3, Duration.ofMillis(500), 2.0, 50, 10, 5,
        Duration.ofSeconds(30));
    assertEquals(Duration.ofSeconds(2), config.connectTimeout());
    assertEquals(Duration.ofSeconds(5), config.readTimeout());
    assertEquals("remitente@example.com", config.fromAddress());
    assertNotNull(springConfig.brevoRestClient(config));
  }
}
