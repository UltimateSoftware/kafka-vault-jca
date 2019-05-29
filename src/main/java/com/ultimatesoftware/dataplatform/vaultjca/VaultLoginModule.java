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

// http://kafka.apache.org/0100/documentation.html#security_sasl_plain_production
// https://cwiki.apache.org/confluence/display/KAFKA/KIP-86%3A+Configurable+SASL+callback+handlers
// https://issues.apache.org/jira/browse/KAFKA-4185
// TODO (mauricio) put a sequence diagram to show possibilities of password encoder
public class VaultLoginModule implements LoginModule {
  private static final Logger log = LoggerFactory.getLogger(VaultLoginModule.class);
  protected static final String ADMIN_PATH = "admin_path";
  protected static final String USERNAME_KEY = "username";
  protected static final String PASSWORD_KEY = "password";

  private final VaultService vaultService;

  static {
    PlainSaslServerProvider.initialize();
  }

  public VaultLoginModule() {
    vaultService = new DefaultVaultService();
  }

  // For testing
  protected VaultLoginModule(VaultService vaultService) {
    this.vaultService = vaultService;
  }

  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
    String adminPath = (String) options.get(ADMIN_PATH);
    log.debug("Initializing VaultLoginModule - Admin path {}", adminPath);

    // TODO (mauricio) separate this into two LoginModules?
    if (adminPath != null && !adminPath.isEmpty()) {
      // The difference is that user/passwd for admin comes from vault, the user-clients provide their user/pass in jaas file
      // I'm assuming that admin credentials are only reachable using specific vault creds
      final Map<String, String> adminCredentials = vaultService.getSecret(adminPath);
      if (adminCredentials != null && adminCredentials.size() > 0) {
        subject.getPublicCredentials().add(adminCredentials.get(USERNAME_KEY));
        subject.getPrivateCredentials().add(adminCredentials.get(PASSWORD_KEY));
      } else {
        throw new RuntimeException(String.format("Secret not found for path %s", adminPath));
      }
    } else if (!(isNullOrEmpty((String) options.get(USERNAME_KEY)) || isNullOrEmpty((String) options.get(PASSWORD_KEY)))) {
      subject.getPublicCredentials().add(options.get("username"));
      subject.getPrivateCredentials().add(options.get("password"));
    } else {
      // TODO (mauricio) add more info below.
      throw new RuntimeException("Not a valid jaas file; specify username and password e.g.\n"
          + "KafkaClient {\n"
          + "  com.ultimatesoftware.dataplatform.vaultjca.VaultLoginModule required\n"
          + "  username=\"alice\"\n"
          + "  password=\"alicepwd\";\n"
          + "};");
    }
  }

  private boolean isNullOrEmpty(String string) {
    return string == null || string.isEmpty();
  }

  @Override
  public boolean login() throws LoginException {
    log.debug("LOGIN CALLED");
    return true;
  }

  @Override
  public boolean commit() throws LoginException {
    log.debug("COMMIT CALLED");
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
