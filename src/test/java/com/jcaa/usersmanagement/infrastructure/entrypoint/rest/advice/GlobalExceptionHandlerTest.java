package com.jcaa.usersmanagement.infrastructure.entrypoint.rest.advice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jcaa.usersmanagement.domain.exception.InvalidCredentialsException;
import com.jcaa.usersmanagement.domain.exception.UserAlreadyExistsException;
import com.jcaa.usersmanagement.domain.exception.UserNotFoundException;
import com.jcaa.usersmanagement.infrastructure.adapter.persistence.exception.PersistenceException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  @DisplayName("Usuario inexistente responde 404 con el mensaje del dominio")
  void notFound() {
    final var ex = UserNotFoundException.becauseIdWasNotFound("x");
    final var response = handler.handleUserNotFound(ex);
    assertEquals(404, response.status());
    assertEquals(ex.getMessage(), response.message());
  }

  @Test
  @DisplayName("Correo duplicado responde 409")
  void conflict() {
    assertEquals(409, handler.handleUserAlreadyExists(UserAlreadyExistsException.becauseEmailAlreadyExists("a@b.co")).status());
  }

  @Test
  @DisplayName("Credenciales inválidas responden 401")
  void unauthorized() {
    assertEquals(401, handler.handleInvalidCredentials(InvalidCredentialsException.becauseCredentialsAreInvalid()).status());
  }

  @Test
  @DisplayName("Violaciones de validación responden 400 con campo y mensaje")
  void constraintViolation() {
    @SuppressWarnings("unchecked")
    final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    final Path path = mock(Path.class);
    when(path.toString()).thenReturn("email");
    when(violation.getPropertyPath()).thenReturn(path);
    when(violation.getMessage()).thenReturn("no es valido");
    final var response = handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));
    assertEquals(400, response.status());
    assertEquals("email: no es valido", response.message());
  }

  @Test
  @DisplayName("Cuerpo inválido responde 400 con los campos que fallan")
  void methodArgumentNotValid() {
    final MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
    final BindingResult binding = mock(BindingResult.class);
    when(ex.getBindingResult()).thenReturn(binding);
    when(binding.getFieldErrors()).thenReturn(List.of(new FieldError("req", "name", "muy corto"), new FieldError("req", "role", "requerido")));
    final var response = handler.handleMethodArgumentNotValid(ex);
    assertEquals(400, response.status());
    assertEquals("name: muy corto, role: requerido", response.message());
  }

  @Test
  @DisplayName("Otra excepción de dominio responde 400")
  void domain() {
    assertEquals(400, handler.handleDomain(InvalidCredentialsException.becauseUserIsNotActive()).status());
  }

  @Test
  @DisplayName("Errores de persistencia y genéricos responden 500 sin filtrar detalles")
  void serverErrors() {
    final var persistence = handler.handlePersistence(mock(PersistenceException.class));
    final var general = handler.handleGeneral(new IllegalStateException("detalle interno"));
    assertEquals(500, persistence.status());
    assertEquals("Error de persistencia.", persistence.message());
    assertEquals(500, general.status());
    assertEquals("Error interno del servidor.", general.message());
  }
}
