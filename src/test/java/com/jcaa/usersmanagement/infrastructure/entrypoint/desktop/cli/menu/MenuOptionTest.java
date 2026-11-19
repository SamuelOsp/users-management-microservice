package com.jcaa.usersmanagement.infrastructure.entrypoint.desktop.cli.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MenuOptionTest {

  @ParameterizedTest
  @EnumSource(MenuOption.class)
  @DisplayName("fromNumber encuentra cada opción por su número")
  void fromNumberFindsEveryOption(final MenuOption option) {
    assertEquals(Optional.of(option), MenuOption.fromNumber(option.getNumber()));
    assertTrue(!option.getDescription().isBlank());
  }

  @Test
  @DisplayName("fromNumber devuelve vacío para un número que no existe")
  void fromNumberUnknown() {
    assertTrue(MenuOption.fromNumber(99).isEmpty());
  }
}
