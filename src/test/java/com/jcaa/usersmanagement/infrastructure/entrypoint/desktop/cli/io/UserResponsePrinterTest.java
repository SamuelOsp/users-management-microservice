package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.CliTestSupport;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserResponsePrinterTest {

  @Test
  @DisplayName("print muestra todos los campos del usuario")
  void printShowsAllFields() {
    final CliTestSupport cli = new CliTestSupport();
    new UserResponsePrinter(cli.console()).print(CliTestSupport.USER);
    final String out = cli.output();
    assertAll(
        () -> assertTrue(out.contains("u-1")),
        () -> assertTrue(out.contains("Ana Perez")),
        () -> assertTrue(out.contains("ana@example.com")),
        () -> assertTrue(out.contains("MEMBER")),
        () -> assertTrue(out.contains("ACTIVE")));
  }

  @Test
  @DisplayName("printList avisa cuando no hay usuarios")
  void printListEmpty() {
    final CliTestSupport cli = new CliTestSupport();
    new UserResponsePrinter(cli.console()).printList(List.of());
    assertTrue(cli.output().contains("No users found."));
  }

  @Test
  @DisplayName("printList muestra el total y cada usuario")
  void printListWithUsers() {
    final CliTestSupport cli = new CliTestSupport();
    new UserResponsePrinter(cli.console()).printList(List.of(CliTestSupport.USER));
    assertTrue(cli.output().contains("Total: 1 user(s)"));
    assertTrue(cli.output().contains("Ana Perez"));
  }
}
