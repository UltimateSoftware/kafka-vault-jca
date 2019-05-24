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
  private static final String USERS_PATH = "users_path";
  private final VaultService vaultService;
  private String usersPathVault;

  public VaultAuthenticationLoginCallbackHandler() {
    this.vaultService = new DefaultVaultService();
  }

  @Override
  public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {
    log.info("MAURICIO saslMechanism {}", saslMechanism);
    usersPathVault = JaasContext.configEntryOption(jaasConfigEntries, USERS_PATH, VaultLoginModule.class.getName());
    log.info("usersPathVault = {}", usersPathVault);
    if (usersPathVault == null || usersPathVault.isEmpty()) {
      throw new RuntimeException(String.format("Jaas file needs an entry %s to the path in vault where the users reside", USERS_PATH));
    }
  }

  @Override
  public void close() {
    log.info("CLOSED CALLLED");
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
      log.info("Trying authentication for {} with pwd {}", username, password);
      // check in vault
      // set authenticated in the callback
      // getting this secret per call can be expensive, but eases any updates on vault.
      // TODO (mauricio) use guava cache to keep this in memory with a TTL
      Map<String, String> usersMap = vaultService.getSecret(usersPathVault);
      // TODO (mauricio) do we need to use password encoders? if so, adjust accordingly.
      return username.contains(username) && Arrays.equals(usersMap.get(username).toCharArray(), password);
    }
  }
}
