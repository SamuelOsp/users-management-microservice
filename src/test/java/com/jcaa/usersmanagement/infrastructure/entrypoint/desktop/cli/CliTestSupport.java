package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli;

import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.io.ConsoleIO;
import com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.dto.UserResponse;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/** Consola real sobre una entrada fija y una salida en memoria, para las pruebas de la CLI. */
public final class CliTestSupport {

  public static final UserResponse USER =
      new UserResponse("u-1", "Ana Perez", "ana@example.com", "MEMBER", "ACTIVE");

  private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  private final ConsoleIO console;

  public CliTestSupport(final String... lines) {
    final String input = lines.length == 0 ? "" : String.join("\n", lines) + "\n";
    console =
        new ConsoleIO(
            new Scanner(input), new PrintStream(buffer, true, StandardCharsets.UTF_8));
  }

  public ConsoleIO console() {
    return console;
  }

  public String output() {
    return buffer.toString(StandardCharsets.UTF_8);
  }
}
