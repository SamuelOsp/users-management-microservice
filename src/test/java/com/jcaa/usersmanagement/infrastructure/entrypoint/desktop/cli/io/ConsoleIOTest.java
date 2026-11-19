package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.CliTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConsoleIOTest {

  @Test
  @DisplayName("readRequired insiste hasta recibir un valor y lo devuelve sin espacios")
  void readRequiredRepromptsOnBlank() {
    final CliTestSupport cli = new CliTestSupport("   ", "  ana  ");
    assertEquals("ana", cli.console().readRequired("Nombre: "));
    assertTrue(cli.output().contains("Value cannot be blank"));
  }

  @Test
  @DisplayName("readOptional acepta un valor vacío")
  void readOptionalAcceptsBlank() {
    final CliTestSupport cli = new CliTestSupport("");
    assertEquals("", cli.console().readOptional("Clave: "));
    assertTrue(cli.output().contains("Clave: "));
  }

  @Test
  @DisplayName("readInt rechaza texto no numérico y devuelve el primer número válido")
  void readIntRepromptsOnInvalidNumber() {
    final CliTestSupport cli = new CliTestSupport("dos", "2");
    assertEquals(2, cli.console().readInt("Opcion: "));
    assertTrue(cli.output().contains("Invalid input. Please enter a number."));
  }

  @Test
  @DisplayName("println y printf escriben en la salida")
  void writesOutput() {
    final CliTestSupport cli = new CliTestSupport();
    cli.console().println("hola");
    cli.console().println();
    cli.console().printf("%d usuarios", 3);
    assertEquals("hola" + System.lineSeparator() + System.lineSeparator() + "3 usuarios", cli.output());
  }
}
