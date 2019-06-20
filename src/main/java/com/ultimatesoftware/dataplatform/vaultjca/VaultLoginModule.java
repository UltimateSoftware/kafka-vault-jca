package com.ultimatesoftware.dataplatform.vaultjca;

import com.ultimatesoftware.dataplatform.vaultjca.services.CacheDecoratorVaultService;
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

/**
 * Implementation of {@link LoginModule} that sets up Vault as a Login Module for Kafka.
 *
 * <p>The default implementation of SASL/PLAIN in Kafka specifies user names and passwords in the JAAS configuration file.
 * In order to avoid storing these plain in disk you need to create your own implementation of common JCA
 * {@link javax.security.auth.spi.LoginModule} and {@link javax.security.auth.callback.CallbackHandler} that obtains the username and
 * password from an external source; in this case Vault.</p>
 *
 * <p>More info here https://docs.confluent.io/current/kafka/authentication_sasl/authentication_sasl_plain.html#sasl-plain-overview.</p>
 *
 * <p>It can enable cache using an environment variable `CACHE_VAULT=true` check {@link CacheDecoratorVaultService} on how to
 * tweak further the TTL of the entries.</p>
 */
// http://kafka.apache.org/0100/documentation.html#security_sasl_plain_production
// https://cwiki.apache.org/confluence/display/KAFKA/KIP-86%3A+Configurable+SASL+callback+handlers
// https://issues.apache.org/jira/browse/KAFKA-4185
public class VaultLoginModule implements LoginModule {
  private static final Logger log = LoggerFactory.getLogger(VaultLoginModule.class);
  static final String ADMIN_PATH = "admin_path";
  static final String USERNAME_KEY = "username";
  static final String PASSWORD_KEY = "password";
  static final String ENV_CACHE_VAULT = "CACHE_VAULT";

  private final VaultService vaultService;

  static {
    PlainSaslServerProvider.initialize();
  }

  public VaultLoginModule() {
    if (System.getenv(ENV_CACHE_VAULT) != null && System.getenv(ENV_CACHE_VAULT).equalsIgnoreCase("true")) {
      log.debug("Cache vault enabled");
      vaultService = new CacheDecoratorVaultService(new DefaultVaultService());
    } else {
      vaultService = new DefaultVaultService();
    }
  }

  // For testing
  protected VaultLoginModule(VaultService vaultService) {
    this.vaultService = vaultService;
  }

  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
    String adminPath = (String) options.get(ADMIN_PATH);
    log.debug("Initializing VaultLoginModule - Admin path {}", adminPath);

    if (adminPath != null && !adminPath.isEmpty()) {
      // The difference is that user/passwd for admin comes from vault, the user-clients provide their user/pass in jaas file
      // I'm assuming that admin credentials are only reachable using specific vault creds
      final Map<String, String> adminCredentials = vaultService.getSecret(adminPath);
      if (adminCredentials != null && adminCredentials.size() > 0) {
        subject.getPublicCredentials().add(adminCredentials.get(USERNAME_KEY));
        subject.getPrivateCredentials().add(adminCredentials.get(PASSWORD_KEY));
        return;
      }

      throw new RuntimeException(String.format("Secret not found for path %s", adminPath));
    }

    if (!(isNullOrEmpty((String) options.get(USERNAME_KEY)) || isNullOrEmpty((String) options.get(PASSWORD_KEY)))) {
      subject.getPublicCredentials().add(options.get("username"));
      subject.getPrivateCredentials().add(options.get("password"));
      return;

    }

    throw new RuntimeException("Not a valid jaas file; specify username and password e.g.\n"
        + "KafkaClient {\n"
        + "  org.apache.kafka.common.security.plain.PlainLoginModule required\n"
        + "  username=\"alice\"\n"
        + "  password=\"alicepwd\";\n"
        + "};");

  }

  private boolean isNullOrEmpty(String string) {
    return string == null || string.isEmpty();
  }

  @Override
  public boolean login() throws LoginException {
    log.debug("Login Called");
    return true;
  }

  @Override
  public boolean commit() throws LoginException {
    log.debug("Commit called");
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
