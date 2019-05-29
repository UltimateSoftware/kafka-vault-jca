package com.ultimatesoftware.dataplatform.vaultjca;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

public class VaultLoginModuleTest {

  protected static final String VAULT_KAFKA_ADMIN_PATH = "secrets/kafka/admin";
  protected static final String VAULT_KAFKA_USERS_PATH = "secrets/kafka/users";
  private static final String ADMIN = "admin";
  private static final String ADMINPWD = "adminpwd";
  private VaultService vaultService = mock(VaultService.class);
  private final VaultLoginModule vaultLoginModule = new VaultLoginModule(vaultService);
  private Subject subject;
  private CallbackHandler callbackHandler = mock(CallbackHandler.class);
  private Map<String, String> options;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    subject = new Subject(false, new HashSet<>(), new HashSet<>(), new HashSet<>());
    options = new HashMap<>();
    options.put(VaultLoginModule.ADMIN_PATH, VAULT_KAFKA_ADMIN_PATH);
    options.put(VaultAuthenticationLoginCallbackHandler.USERS_PATH, VAULT_KAFKA_USERS_PATH);
  }

  @Test
  public void shouldInitializeLoginModuleForAdmin() {
    trainMockForSuccess();

    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, options);

    assertThat(subject.getPublicCredentials(), contains(ADMIN));
    assertThat(subject.getPrivateCredentials(), contains(ADMINPWD));
  }

  @Test
  public void shouldThrowExceptionWhenCredentialsNotPresentInVault() {
    when(vaultService.getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH))).thenReturn(new HashMap<>());
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(containsString("Secret not found for path " + VAULT_KAFKA_ADMIN_PATH));
    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, options);
  }

  @Test
  public void shouldInitializeLoginModuleForClient() {
    options = new HashMap<>();
    String alice = "alice";
    String alicepwd = "alicepwd";
    options.put(VaultLoginModule.USERNAME_KEY, alice);
    options.put(VaultLoginModule.PASSWORD_KEY, alicepwd);

    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, options);
    assertThat(subject.getPublicCredentials(), contains(alice));
    assertThat(subject.getPrivateCredentials(), contains(alicepwd));
  }

  @Test
  public void shouldThrownExceptionOnInvalidJaasEntry() {
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(containsString("Not a valid jaas file"));
    vaultLoginModule.initialize(subject, callbackHandler, Collections.EMPTY_MAP, Collections.EMPTY_MAP);

  }

  private void trainMockForSuccess() {
    Map<String, String> adminCredentials = new HashMap<>();
    adminCredentials.put(VaultLoginModule.USERNAME_KEY, ADMIN);
    adminCredentials.put(VaultLoginModule.PASSWORD_KEY, ADMINPWD);
    when(vaultService.getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH))).thenReturn(adminCredentials);
  }

}