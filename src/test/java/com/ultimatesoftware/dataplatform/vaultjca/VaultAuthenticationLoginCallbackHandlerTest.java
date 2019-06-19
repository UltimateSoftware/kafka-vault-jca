package com.ultimatesoftware.dataplatform.vaultjca;

import static com.ultimatesoftware.dataplatform.vaultjca.VaultLoginModuleTest.VAULT_KAFKA_ADMIN_PATH;
import static com.ultimatesoftware.dataplatform.vaultjca.VaultLoginModuleTest.VAULT_KAFKA_USERS_PATH;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.sasl.AuthorizeCallback;
import org.apache.kafka.common.security.plain.PlainAuthenticateCallback;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentMatchers;

public class VaultAuthenticationLoginCallbackHandlerTest {

  public static final String SASL_MECHANISM = "PLAIN";
  private VaultService vaultService = mock(VaultService.class);
  private VaultAuthenticationLoginCallbackHandler callbackHandler = new VaultAuthenticationLoginCallbackHandler(vaultService);
  private Map<String, String> options = new HashMap<>();
  private List<AppConfigurationEntry> jaasConfigEntries;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @Before
  public void init() {
    options.put(VaultAuthenticationLoginCallbackHandler.ADMIN_PATH, VAULT_KAFKA_ADMIN_PATH);
    options.put(VaultAuthenticationLoginCallbackHandler.USERS_PATH, VAULT_KAFKA_USERS_PATH);

    jaasConfigEntries = new ArrayList<>();
    AppConfigurationEntry entry =
        new AppConfigurationEntry("com.ultimatesoftware.dataplatform.vaultjca.VaultLoginModule",
            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
    jaasConfigEntries.add(entry);
  }

  @Test
  public void shouldConfigureCallback() {
    callbackHandler.configure(Collections.EMPTY_MAP, SASL_MECHANISM, jaasConfigEntries);

  }

  @Test
  public void shouldThrownExceptionWhenPathsAreNotPresent() {
    options.remove(VaultAuthenticationLoginCallbackHandler.ADMIN_PATH);
    thrown.expect(RuntimeException.class);
    thrown.expectMessage(containsString("Jaas file needs an entry"));
    callbackHandler.configure(Collections.EMPTY_MAP, SASL_MECHANISM, jaasConfigEntries);
  }

  @Test
  public void shouldHandleAdminLogin() throws Exception {
    callbackHandler.configure(Collections.EMPTY_MAP, SASL_MECHANISM, jaasConfigEntries);

    Callback[] callbacks = new Callback[2];
    callbacks[0] = new NameCallback("username", "admin");
    callbacks[1] = new PlainAuthenticateCallback("adminpwd".toCharArray());

    Map<String, String> adminCreds = new HashMap<>();
    adminCreds.put("username", "admin");
    adminCreds.put(VaultAuthenticationLoginCallbackHandler.PASSWORD_MAP_ENTRY_KEY, "adminpwd");

    when(vaultService.getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH))).thenReturn(adminCreds);

    callbackHandler.handle(callbacks);
    assertThat(((PlainAuthenticateCallback) callbacks[1]).authenticated(), is(true));
    verify(vaultService).getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH));
    verify(vaultService, never()).getSecret(ArgumentMatchers.eq(VAULT_KAFKA_USERS_PATH));
  }

  @Test
  public void shouldHandleClientLogin() throws Exception {
    callbackHandler.configure(Collections.EMPTY_MAP, SASL_MECHANISM, jaasConfigEntries);

    Callback[] callbacks = new Callback[2];
    String clientUsername = "alice";
    callbacks[0] = new NameCallback("username", clientUsername);
    callbacks[1] = new PlainAuthenticateCallback("alicepwd".toCharArray());

    Map<String, String> usersMap = new HashMap<>();
    usersMap.put(VaultAuthenticationLoginCallbackHandler.PASSWORD_MAP_ENTRY_KEY, "alicepwd");

    when(vaultService.getSecret(ArgumentMatchers.eq(VAULT_KAFKA_USERS_PATH+"/"+clientUsername))).thenReturn(usersMap);

    callbackHandler.handle(callbacks);
    assertThat(((PlainAuthenticateCallback) callbacks[1]).authenticated(), is(true));
    verify(vaultService, never()).getSecret(ArgumentMatchers.eq(VAULT_KAFKA_ADMIN_PATH));
    verify(vaultService).getSecret(ArgumentMatchers.eq(VAULT_KAFKA_USERS_PATH+"/"+clientUsername));

  }

  @Test
  public void shouldThrownExceptionWithNotSupportedCallback() throws Exception {
    Callback[] callbacks = new Callback[1];
    callbacks[0] = new AuthorizeCallback("authnID", "authzID");

    thrown.expect(UnsupportedCallbackException.class);
    callbackHandler.handle(callbacks);
  }
}