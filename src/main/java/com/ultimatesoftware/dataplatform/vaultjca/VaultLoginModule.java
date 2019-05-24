package com.ultimatesoftware.dataplatform.vaultjca;

import com.ultimatesoftware.dataplatform.vaultjca.services.DefaultVaultService;
import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.apache.kafka.common.security.plain.internals.PlainSaslServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VaultLoginModule implements LoginModule {
  private static final Logger log = LoggerFactory.getLogger(VaultLoginModule.class);
  private static final String ADMIN_PATH = "admin_path";
  private static final String USERNAME_KEY = "username";
  private static final String PASSWORD_KEY = "password";

  private final VaultService vaultService;

  static {
    PlainSaslServerProvider.initialize();
  }

  public VaultLoginModule() {
    vaultService = new DefaultVaultService();
  }

  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
    options.forEach((k, v) -> log.info("Key={}\tvalue={}", k, v));
    log.info("Subject {}", subject);
    log.info("CallbackHandler {}", callbackHandler);
    sharedState.forEach((k, v) -> log.info("Shared State k={}\tv={}", k, v));

    String adminPath = (String) options.get(ADMIN_PATH);
    log.debug("Initializing VaultLoginModule - Admin path {}", adminPath);

    // TODO (mauricio) separate this into two LoginModules?
    if (adminPath != null && !adminPath.isEmpty()) {
      final Map<String, String> adminCredentials = vaultService.getSecret(adminPath);
      if (adminCredentials != null && adminCredentials.size() > 0) {
        subject.getPublicCredentials().add(adminCredentials.get(USERNAME_KEY));
        subject.getPrivateCredentials().add(adminCredentials.get(PASSWORD_KEY));
      } else {
        throw new RuntimeException(String.format("Secret not found for path %s", adminPath));
      }
    } else {
      // try user
      String username = (String) options.get("username");
      String password = (String) options.get("password");

      subject.getPublicCredentials().add(username);
      subject.getPrivateCredentials().add(password);
    }
  }

  @Override
  public boolean login() throws LoginException {
    log.info("LOGIN CALLED");
    return true;
  }

  @Override
  public boolean commit() throws LoginException {
    log.info("COMMIT CALLED");
    return true;
  }

  @Override
  public boolean abort() throws LoginException {
    return false;
  }

  @Override
  public boolean logout() throws LoginException {
    return true;
  }
}
