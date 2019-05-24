package com.ultimatesoftware.dataplatform.vaultjca.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

public class DefaultVaultServiceTest {

  // TODO(mauricio) use container test rule to start vault and get then assign these
  private VaultService vaultService = new DefaultVaultService("http://localhost:8200", "root-token");


  @Test
  public void shouldLoadAdminCredentials() {
    final Map<String, String> secret = vaultService.getSecret("secret/kafka/admin");
    assertThat(secret, is(notNullValue()));
    assertThat(secret.get("username"), is(notNullValue()));
    assertThat(secret.get("password"), is(notNullValue()));
  }

}