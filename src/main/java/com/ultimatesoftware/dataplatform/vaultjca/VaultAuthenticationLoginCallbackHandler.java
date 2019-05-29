package com.ultimatesoftware.dataplatform.vaultjca;

import com.ultimatesoftware.dataplatform.vaultjca.services.DefaultVaultService;
import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.kafka.common.security.JaasContext;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;
import org.apache.kafka.common.security.plain.PlainAuthenticateCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// https://strimzi.io/2018/11/16/using-vault-with-strimzi.html
public class VaultAuthenticationLoginCallbackHandler implements AuthenticateCallbackHandler {
  private static final Logger log = LoggerFactory.getLogger(VaultAuthenticationLoginCallbackHandler.class);
  protected static final String USERS_PATH = "users_path";
  protected static final String ADMIN_PATH = "admin_path";
  private final VaultService vaultService;
  private String usersPathVault;
  private String adminPathVault;

  public VaultAuthenticationLoginCallbackHandler() {
    this.vaultService = new DefaultVaultService();
  }

  // for testing
  protected VaultAuthenticationLoginCallbackHandler(VaultService vaultService) {
    this.vaultService = vaultService;
  }

  @Override
  public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
    adminPathVault = JaasContext.configEntryOption(jaasConfigEntries, ADMIN_PATH, VaultLoginModule.class.getName());
    usersPathVault = JaasContext.configEntryOption(jaasConfigEntries, USERS_PATH, VaultLoginModule.class.getName());
    log.info("usersPathVault = {}", usersPathVault);
    if (usersPathVault == null || usersPathVault.isEmpty()) {
      throw new RuntimeException(String.format("Jaas file needs an entry %s to the path in vault where the users reside", USERS_PATH));
    }
    if (adminPathVault == null || adminPathVault.isEmpty()) {
      throw new RuntimeException(String.format("Jaas file needs an entry %s to the path in vault where the users reside", ADMIN_PATH));
    }
  }

  @Override
  public void close() {
    log.debug("CLOSED CALLLED");
  }

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
    String username = null;
    for (Callback callback : callbacks) {
      if (callback instanceof NameCallback) {
        username = ((NameCallback) callback).getDefaultName();
        log.info("Handling callback for NameCallback {}", username);
      }
      else if (callback instanceof PlainAuthenticateCallback) {
        log.info("Handling callback for PlainAuth {}", ((PlainAuthenticateCallback) callback).password());
        PlainAuthenticateCallback plainCallback = (PlainAuthenticateCallback) callback;
        plainCallback.authenticated(authenticateWithVault(username, plainCallback.password()));
      }
      else {
        throw new UnsupportedCallbackException(callback);
      }
    }
  }

  protected boolean authenticateWithVault(String username, char[] password) {
    if (username == null) {
      return false;
    }
    else {
      String pathVault = (username.equals("admin")) ? adminPathVault : usersPathVault;
      log.info("Trying authentication for {} with pwd {} in path {}", username, password, pathVault);
      // getting this secret per call can be expensive, but eases any updates on vault.
      // TODO (mauricio) use guava cache to keep this in memory with a TTL
      Map<String, String> usersMap = vaultService.getSecret(pathVault);
      log.info("userMap {}", usersMap);
      // TODO (mauricio) do we need to use password encoders? if so, adjust accordingly.
      if (username.equals("admin")) {
        return usersMap.get("username").equals(username) && Arrays.equals(usersMap.get("password").toCharArray(), password);
      }
      else {
        log.info("Contains {} - {}", username, usersMap.containsKey(username));
        log.info("Password match {}", Arrays.equals(usersMap.get(username).toCharArray(), password));
        return usersMap.containsKey(username) && Arrays.equals(usersMap.get(username).toCharArray(), password);
      }
    }
  }
}
