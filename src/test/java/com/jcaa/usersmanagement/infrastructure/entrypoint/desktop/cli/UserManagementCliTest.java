package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.controller.UserController;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserManagementCliTest {

  private final UserController controller = mock(UserController.class);

  @Test
  @DisplayName("Muestra el menú, rechaza una opción inválida, ejecuta una válida y sale con 0")
  void runsMenuUntilExit() {
    when(controller.listAllUsers()).thenReturn(List.of(CliTestSupport.USER));
    final CliTestSupport cli = new CliTestSupport("9", "1", "0");
    new UserManagementCli(controller, cli.console()).start();
    final String out = cli.output();
    verify(controller).listAllUsers();
    assertTrue(out.contains("Users Management System"));
    assertTrue(out.contains("Invalid option. Please try again."));
    assertTrue(out.contains("Total: 1 user(s)"));
    assertTrue(out.contains("Goodbye!"));
  }

  @Test
  @DisplayName("Muestra los errores de validación y sigue funcionando")
  void showsValidationErrors() {
    @SuppressWarnings("unchecked")
    final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getMessage()).thenReturn("email must be a valid email address");
    final ConstraintViolationException exception = new ConstraintViolationException(Set.of(violation));
    when(controller.listAllUsers()).thenThrow(exception);
    final CliTestSupport cli = new CliTestSupport("1", "0");
    new UserManagementCli(controller, cli.console()).start();
    assertTrue(cli.output().contains("Validation errors:"));
    assertTrue(cli.output().contains("- email must be a valid email address"));
  }

  @Test
  @DisplayName("Un error inesperado se informa sin cerrar la aplicación")
  void showsUnexpectedErrors() {
    when(controller.listAllUsers()).thenThrow(new IllegalStateException("base caida"));
    final CliTestSupport cli = new CliTestSupport("1", "0");
    new UserManagementCli(controller, cli.console()).start();
    assertTrue(cli.output().contains("Unexpected error: base caida"));
    assertTrue(cli.output().contains("Goodbye!"));
  }
}
