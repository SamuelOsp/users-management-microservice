package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.CliTestSupport;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io.UserResponsePrinter;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.controller.UserController;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.CreateUserRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.LoginRequest;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.UpdateUserRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OperationHandlersTest {

  private UserController controller;

  @BeforeEach
  void setUp() {
    controller = mock(UserController.class);
  }

  private static UserResponsePrinter printer(final CliTestSupport cli) {
    return new UserResponsePrinter(cli.console());
  }

  @Test
  @DisplayName("Listar imprime los usuarios del controlador")
  void listUsers() {
    final CliTestSupport cli = new CliTestSupport();
    when(controller.listAllUsers()).thenReturn(List.of(CliTestSupport.USER));
    new ListUsersHandler(controller, printer(cli)).handle();
    assertTrue(cli.output().contains("Total: 1 user(s)"));
  }

  @Test
  @DisplayName("Buscar por id imprime el usuario encontrado")
  void findFound() {
    final CliTestSupport cli = new CliTestSupport("u-1");
    when(controller.findUserById("u-1")).thenReturn(CliTestSupport.USER);
    new FindUserByIdHandler(controller, cli.console(), printer(cli)).handle();
    assertTrue(cli.output().contains("ana@example.com"));
  }

  @Test
  @DisplayName("Buscar por id avisa cuando no existe")
  void findNotFound() {
    final CliTestSupport cli = new CliTestSupport("x");
    when(controller.findUserById("x")).thenThrow(UserNotFoundException.becauseIdWasNotFound("x"));
    new FindUserByIdHandler(controller, cli.console(), printer(cli)).handle();
    assertTrue(cli.output().contains("Not found:"));
  }

  @Test
  @DisplayName("Crear envía los datos leídos y muestra el resultado")
  void createOk() {
    final CliTestSupport cli = new CliTestSupport("u-1", "Ana Perez", "ana@example.com", "Clave123!", "MEMBER");
    when(controller.createUser(any())).thenReturn(CliTestSupport.USER);
    new CreateUserHandler(controller, cli.console(), printer(cli)).handle();
    verify(controller)
        .createUser(new CreateUserRequest("u-1", "Ana Perez", "ana@example.com", "Clave123!", "MEMBER"));
    assertTrue(cli.output().contains("User created successfully."));
  }

  @Test
  @DisplayName("Crear avisa cuando el correo ya existe")
  void createDuplicate() {
    final CliTestSupport cli = new CliTestSupport("u-1", "Ana Perez", "ana@example.com", "Clave123!", "MEMBER");
    when(controller.createUser(any()))
        .thenThrow(UserAlreadyExistsException.becauseEmailAlreadyExists("ana@example.com"));
    new CreateUserHandler(controller, cli.console(), printer(cli)).handle();
    assertTrue(cli.output().contains("Error:"));
  }

  @Test
  @DisplayName("Actualizar con clave vacía conserva la actual (envía null)")
  void updateKeepsPassword() {
    final CliTestSupport cli = new CliTestSupport("u-1", "Ana", "ana@example.com", "", "MEMBER", "ACTIVE");
    when(controller.updateUser(any())).thenReturn(CliTestSupport.USER);
    new UpdateUserHandler(controller, cli.console(), printer(cli)).handle();
    final ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
    verify(controller).updateUser(captor.capture());
    assertNull(captor.getValue().password());
    assertTrue(cli.output().contains("User updated successfully."));
  }

  @Test
  @DisplayName("Actualizar con clave nueva la envía")
  void updateWithPassword() {
    final CliTestSupport cli = new CliTestSupport("u-1", "Ana", "ana@example.com", "Nueva123!", "MEMBER", "ACTIVE");
    when(controller.updateUser(any())).thenReturn(CliTestSupport.USER);
    new UpdateUserHandler(controller, cli.console(), printer(cli)).handle();
    final ArgumentCaptor<UpdateUserRequest> captor = ArgumentCaptor.forClass(UpdateUserRequest.class);
    verify(controller).updateUser(captor.capture());
    assertEquals("Nueva123!", captor.getValue().password());
  }

  @Test
  @DisplayName("Actualizar avisa cuando el usuario no existe")
  void updateNotFound() {
    final CliTestSupport cli = new CliTestSupport("x", "Ana", "ana@example.com", "", "MEMBER", "ACTIVE");
    when(controller.updateUser(any())).thenThrow(UserNotFoundException.becauseIdWasNotFound("x"));
    new UpdateUserHandler(controller, cli.console(), printer(cli)).handle();
    assertTrue(cli.output().contains("Not found:"));
  }

  @Test
  @DisplayName("Eliminar confirma el borrado")
  void deleteOk() {
    final CliTestSupport cli = new CliTestSupport("u-1");
    new DeleteUserHandler(controller, cli.console()).handle();
    verify(controller).deleteUser("u-1");
    assertTrue(cli.output().contains("User deleted successfully."));
  }

  @Test
  @DisplayName("Eliminar avisa cuando el usuario no existe")
  void deleteNotFound() {
    final CliTestSupport cli = new CliTestSupport("x");
    doThrow(UserNotFoundException.becauseIdWasNotFound("x")).when(controller).deleteUser("x");
    new DeleteUserHandler(controller, cli.console()).handle();
    assertTrue(cli.output().contains("Not found:"));
  }

  @Test
  @DisplayName("Login correcto da la bienvenida")
  void loginOk() {
    final CliTestSupport cli = new CliTestSupport("ana@example.com", "Clave123!");
    when(controller.login(new LoginRequest("ana@example.com", "Clave123!"))).thenReturn(CliTestSupport.USER);
    new LoginHandler(controller, cli.console(), printer(cli)).handle();
    assertTrue(cli.output().contains("Login successful. Welcome!"));
  }

  @Test
  @DisplayName("Login con credenciales inválidas muestra el error")
  void loginInvalid() {
    final CliTestSupport cli = new CliTestSupport("ana@example.com", "mala");
    when(controller.login(any())).thenThrow(InvalidCredentialsException.becauseCredentialsAreInvalid());
    new LoginHandler(controller, cli.console(), printer(cli)).handle();
    assertTrue(cli.output().contains("Error:"));
  }
}
