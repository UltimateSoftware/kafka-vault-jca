package com.ultimatesoftware.dataplatform.vaultjca.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

// Just for smoke tests with a specific version of vault
public class DefaultVaultServiceTest {

  private static final String ADMIN_PATH = "secret/kafka/admin";
  private static final String ROOT_TOKEN = "root-token";
  @ClassRule
  public static GenericContainer vaultContainer = new GenericContainer("vault:1.1.2")
      .withEnv("VAULT_DEV_ROOT_TOKEN_ID", ROOT_TOKEN)
      .withExposedPorts(8200);

  @Before
  public void initSecrets() {
    Map<String, String> secrets = new HashMap<>();
    secrets.put("username", "admin");
    secrets.put("password", "admin");
    vaultService.writeSecret(ADMIN_PATH, secrets);
  }

  private VaultService vaultService = new DefaultVaultService(String.format("http://localhost:%s", vaultContainer.getMappedPort(8200)), ROOT_TOKEN);

  @Test
  public void shouldLoadAdminCredentials() {
    final Map<String, String> secret = vaultService.getSecret(ADMIN_PATH);
    assertThat(secret, is(notNullValue()));
    assertThat(secret.get("username"), is(notNullValue()));
    assertThat(secret.get("password"), is(notNullValue()));
  }

}