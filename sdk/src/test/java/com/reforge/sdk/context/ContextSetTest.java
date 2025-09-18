package com.reforge.sdk.context;

import static org.assertj.core.api.Assertions.assertThat;

import cloud.prefab.domain.Prefab;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextSetTest {

  static final Context PREFAB_USER_CONTEXT_1 = Context.fromMap(
    "User",
    Map.of(
      "firstName",
      Prefab.ConfigValue.newBuilder().setString("James").build(),
      "isHuman",
      Prefab.ConfigValue.newBuilder().setBool(true).build()
    )
  );

  static final Context PREFAB_USER_CONTEXT_1_LOWERCASE = Context.fromMap(
    "user",
    Map.of(
      "firstName",
      Prefab.ConfigValue.newBuilder().setString("James").build(),
      "isHuman",
      Prefab.ConfigValue.newBuilder().setBool(true).build()
    )
  );

  static final Context PREFAB_USER_CONTEXT_2 = Context.fromMap(
    "User",
    Map.of(
      "firstName",
      Prefab.ConfigValue.newBuilder().setString("Johnny").build(),
      "isHuman",
      Prefab.ConfigValue.newBuilder().setBool(false).build()
    )
  );

  static final Context PREFAB_COMPANY_CONTEXT = Context.fromMap(
    "Company",
    Map.of("Name", Prefab.ConfigValue.newBuilder().setString("Enron").build())
  );

  @Test
  void itIgnoresNullContextWhenAdding() {
    ContextSet prefabContextSet = new ContextSet();
    prefabContextSet.addContext(null);
    assertThat(prefabContextSet.isEmpty()).isTrue();
  }

  @Test
  void lastContextAddedForEachTypeWins() {
    ContextSet prefabContextSet = new ContextSet();
    prefabContextSet.addContext(PREFAB_USER_CONTEXT_1);
    prefabContextSet.addContext(PREFAB_COMPANY_CONTEXT);
    prefabContextSet.addContext(PREFAB_USER_CONTEXT_2);

    assertThat(prefabContextSet.getContexts()).hasSize(2);
    assertThat(prefabContextSet.getByName("Company")).contains(PREFAB_COMPANY_CONTEXT);
    assertThat(prefabContextSet.getByName("User")).contains(PREFAB_USER_CONTEXT_2);
  }

  @Test
  void lastContextAddedForEachTypeWinsInFromMethod() {
    ContextSet prefabContextSet = ContextSet.from(
      PREFAB_USER_CONTEXT_1,
      PREFAB_COMPANY_CONTEXT,
      PREFAB_USER_CONTEXT_2
    );

    assertThat(prefabContextSet.getContexts()).hasSize(2);
    assertThat(prefabContextSet.getByName("Company")).contains(PREFAB_COMPANY_CONTEXT);
    assertThat(prefabContextSet.getByName("User")).contains(PREFAB_USER_CONTEXT_2);
  }

  @Test
  void itIsCaseInsensitive() {
    ContextSet prefabContextSet = ContextSet.from(
      PREFAB_USER_CONTEXT_2,
      PREFAB_COMPANY_CONTEXT,
      PREFAB_USER_CONTEXT_1_LOWERCASE
    );

    assertThat(prefabContextSet.getContexts()).hasSize(2);
    assertThat(prefabContextSet.getByName("Company")).contains(PREFAB_COMPANY_CONTEXT);
    assertThat(prefabContextSet.getByName("User"))
      .contains(PREFAB_USER_CONTEXT_1_LOWERCASE);
    assertThat(prefabContextSet.getByName("user"))
      .contains(PREFAB_USER_CONTEXT_1_LOWERCASE);
  }

  @Test
  void convertWorksForSetReadableCase() {
    assertThat(ContextSet.convert(PREFAB_COMPANY_CONTEXT))
      .isEqualTo(ContextSet.from(PREFAB_COMPANY_CONTEXT));
  }

  @Test
  void convertWorksForContextSetCase() {
    ContextSet prefabContextSet = ContextSet.from(PREFAB_COMPANY_CONTEXT);
    assertThat(ContextSet.convert(prefabContextSet)).isEqualTo(prefabContextSet);
  }

  @Test
  void fingerPrintIsEmptyForKeyLess() {
    assertThat(
      ContextSet
        .from(
          PREFAB_USER_CONTEXT_2,
          PREFAB_COMPANY_CONTEXT,
          PREFAB_USER_CONTEXT_1_LOWERCASE
        )
        .getFingerPrint()
    )
      .isEmpty();
  }

  @Test
  void fingerPrintForSingleContextWorks() {
    assertThat(
      ContextSet
        .from(Context.newBuilder("user").put("key", "u123").build())
        .getFingerPrint()
    )
      .isEqualTo("user--string: \"u123\"");
  }

  @Test
  void fingerPrintForContextSetWorks() {
    assertThat(
      ContextSet
        .from(
          Context.newBuilder("user").put("key", "u123").build(),
          Context.newBuilder("team").put("key", "t123").build()
        )
        .getFingerPrint()
    )
      .isEqualTo("team--string: \"t123\"user--string: \"u123\"");
  }
}
