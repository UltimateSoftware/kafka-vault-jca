package com.ultimatesoftware.dataplatform.vaultjca.services;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

// Just for smoke tests with a specific version of vault
public class DefaultVaultServiceTest {

  public static final String ADMIN_PATH = "secret/kafka/private/admin";
  public static final String USERS_PATH = "secret/kafka/private/users";
  public static final String ROOT_TOKEN = "root-token";
  public static final String ALICE = "alice";
  public static final String PASSWORD = "password";
  @ClassRule
  public static GenericContainer vaultContainer = new GenericContainer("vault:1.1.2")
      .withEnv("VAULT_DEV_ROOT_TOKEN_ID", ROOT_TOKEN)
      .withExposedPorts(8200);

  private VaultService vaultService = new DefaultVaultService(String.format("http://localhost:%s", vaultContainer.getMappedPort(8200)), ROOT_TOKEN);

  @Before
  public void initSecrets() {
    Map<String, String> adminSecrets = new HashMap<>();
    adminSecrets.put("username", "admin");
    adminSecrets.put(PASSWORD, "admin");
    vaultService.writeSecret(ADMIN_PATH, adminSecrets);

    Map<String, String> usersSecret = new HashMap<>();
    usersSecret.put(PASSWORD, "alicepwd");
    vaultService.writeSecret(String.format("%s/%s", USERS_PATH, ALICE), usersSecret);

  }

  @Test
  public void shouldLoadAdminCredentials() {
    final Map<String, String> secret = vaultService.getSecret(ADMIN_PATH);
    assertThat(secret, is(notNullValue()));
    assertThat(secret.get("username"), is(notNullValue()));
    assertThat(secret.get(PASSWORD), is(notNullValue()));
  }

  @Test
  public void shouldLoadUserCredentials() {
    Map<String, String> secret = vaultService.getSecret(String.format("%s/%s", USERS_PATH, ALICE));
    assertThat(secret, hasKey(PASSWORD));
    assertThat(secret.get(PASSWORD), is("alicepwd"));
  }

  @Test
  public void shouldFailWhenUserNotPresent() {
    Map<String, String> secret = vaultService.getSecret(String.format("%s/%s", USERS_PATH, "other-user"));
    assertThat(secret.size(), is(0));
  }

}