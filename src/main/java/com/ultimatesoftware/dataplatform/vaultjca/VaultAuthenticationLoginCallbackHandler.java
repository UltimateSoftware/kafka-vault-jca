package com.ultimatesoftware.dataplatform.vaultjca;

import static com.ultimatesoftware.dataplatform.vaultjca.VaultLoginModule.ENV_CACHE_VAULT;

import com.ultimatesoftware.dataplatform.vaultjca.services.CacheDecoratorVaultService;
import com.ultimatesoftware.dataplatform.vaultjca.services.DefaultVaultService;
import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.plain.PlainAuthenticateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link AuthenticateCallbackHandler} that uses Vault to authenticate a user.
 *
 * <p>The default implementation of SASL/PLAIN in Kafka specifies user names and passwords in the JAAS configuration file.
 * In order to avoid storing these plain in disk you need to create your own implementation of common JCA
 * {@link javax.security.auth.spi.LoginModule} and {@link javax.security.auth.callback.CallbackHandler} that obtains the username and
 * password from an external source; in this case Vault.</p>
 *
 * <p>More info <a href="https://docs.confluent.io/current/kafka/authentication_sasl/authentication_sasl_plain.html#sasl-plain-overview.">here</a></p>
 *
 * <p>The expected organization of secrets in vault is as follows:</p>
 *
 * <table summary="values in vault per user type">
 *   <thead>
 *     <tr><th>Path</th><th>Description</th></tr>
 *   </thead>
 *   <tbody>
 *   <tr>
 *     <td>Admin: path = jaas file admin_path</td>
 *     <td>KV entry with username=admin and password=secret password</td>
 *   </tr>
 *   <tr>
 *     <td>Users/Clients: path =  jaas file users_path</td>
 *     <td>An entry at users_path/{username} with a kv pair as password=secret_password</td>
 *   </tr>
 *   </tbody>
 * </table>
 */
// https://strimzi.io/2018/11/16/using-vault-with-strimzi.html
public class VaultAuthenticationLoginCallbackHandler implements AuthenticateCallbackHandler {
  private static final Logger log = LoggerFactory.getLogger(VaultAuthenticationLoginCallbackHandler.class);
  static final String USERS_PATH = "users_path";
  static final String ADMIN_PATH = "admin_path";
  static final String PASSWORD_MAP_ENTRY_KEY = "password";
  private final VaultService vaultService;
  private String usersPathVault;
  private String adminPathVault;

  public VaultAuthenticationLoginCallbackHandler() {
    if (System.getenv(ENV_CACHE_VAULT) != null && System.getenv(ENV_CACHE_VAULT).equalsIgnoreCase("true")){
      log.debug("Cache vault enabled");
      vaultService = new CacheDecoratorVaultService(new DefaultVaultService());
    }
    else {
      vaultService = new DefaultVaultService();
    }
  }

  // for testing
  protected VaultAuthenticationLoginCallbackHandler(VaultService vaultService) {
    this.vaultService = vaultService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
    // Loading vault path from jaas config
    adminPathVault = JaasContext.configEntryOption(jaasConfigEntries, ADMIN_PATH, VaultLoginModule.class.getName());
    usersPathVault = JaasContext.configEntryOption(jaasConfigEntries, USERS_PATH, VaultLoginModule.class.getName());
    log.info("usersPathVault = {}", usersPathVault);
    if (usersPathVault == null || usersPathVault.isEmpty()) {
      throw new RuntimeException(String.format("Jaas file needs an entry %s to the path in vault where the users reside", USERS_PATH));
    }
    if (adminPathVault == null || adminPathVault.isEmpty()) {
      throw new RuntimeException(String.format("Jaas file needs an entry %s to the path in vault where the admin credentials reside", ADMIN_PATH));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    log.debug("Close called");
  }

  /**
   * Handles callback to Vault, expects a {@link NameCallback} with the username and a {@link PlainAuthenticateCallback} with the password.
   * @param callbacks Callback array with NameCallback and PlainAuthenticateCallback
   * @throws UnsupportedCallbackException thrown when callbacks are not of either type expected.
   * @see CallbackHandler#handle(javax.security.auth.callback.Callback[])
   */
  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    String username = null;
    for (Callback callback : callbacks) {
      if (callback instanceof NameCallback) {
        username = ((NameCallback) callback).getDefaultName();
        log.debug("Handling callback for NameCallback {}", username);
        continue;
      }

      if (callback instanceof PlainAuthenticateCallback) {
        log.debug("Handling callback for PlainAuthenticateCallback pwd length {}", ((PlainAuthenticateCallback) callback).password().length);
        PlainAuthenticateCallback plainCallback = (PlainAuthenticateCallback) callback;
        plainCallback.authenticated(authenticateWithVault(username, plainCallback.password()));
        continue;
      }

      throw new UnsupportedCallbackException(callback);
    }
  }

  private boolean authenticateWithVault(String username, char[] password) {
    if (username == null) {
      return false;
    }

    String pathVault = username.equals("admin") ? adminPathVault : String.format("%s/%s", usersPathVault, username);
    log.info("Trying authentication for {} in path {}", username, pathVault);
    Map<String, String> usersMap = vaultService.getSecret(pathVault);
    if (usersMap.size() == 0) {
      return false;
    }
    if (username.equals("admin")) {
      return usersMap.get("username").equals(username) && Arrays.equals(usersMap.get(PASSWORD_MAP_ENTRY_KEY).toCharArray(), password);
    }

    log.info("Password match {}", Arrays.equals(usersMap.get(PASSWORD_MAP_ENTRY_KEY).toCharArray(), password));
    return Arrays.equals(usersMap.get(PASSWORD_MAP_ENTRY_KEY).toCharArray(), password);
  }
}
