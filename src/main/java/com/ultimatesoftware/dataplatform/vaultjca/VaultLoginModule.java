package com.ultimatesoftware.dataplatform.vaultjca;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.ultimatesoftware.dataplatform.vaultjca.services.CacheDecoratorVaultService;
import com.ultimatesoftware.dataplatform.vaultjca.services.HttpVaultService;
import com.ultimatesoftware.dataplatform.vaultjca.services.VaultService;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.apache.kafka.common.security.plain.internals.PlainSaslServerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link LoginModule} that sets up Vault as a Login Module for Kafka.
 *
 * <p>The default implementation of SASL/PLAIN in Kafka specifies user names and passwords in the JAAS configuration file.
 * In order to avoid storing these plain in disk you need to create your own implementation of common JCA {@link javax.security.auth.spi.LoginModule}
 * and {@link javax.security.auth.callback.CallbackHandler} that obtains the username and password from an external source; in this case Vault.</p>
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

  /**
   * Upon instantiation can enable cache implementation if {@code CACHE_VAULT} is true.
   */
  public VaultLoginModule() {
    if ("true".equalsIgnoreCase(System.getenv(ENV_CACHE_VAULT))) {
      log.debug("Cache vault enabled");
      vaultService = new CacheDecoratorVaultService(new HttpVaultService());
      return;
    }
    vaultService = new HttpVaultService();
  }

  @VisibleForTesting
  protected VaultLoginModule(VaultService vaultService) {
    this.vaultService = Preconditions.checkNotNull(vaultService);
  }

  /**
   * Initialize VaultLoginModule.
   *
   * <p>Makes simple checks that the expected configuration read by jaas configuration is present.
   * it expects an entry for `admin_path`</p>
   *
   * <p>This method is called as part of the JCA dance from {@link LoginContext}</p>
   * {@inheritDoc}
   */
  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options) {
    String adminPath = (String) options.get(ADMIN_PATH);
    log.debug("Initializing VaultLoginModule - Admin path {}", adminPath);

    if (!Strings.isNullOrEmpty(adminPath)) {
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

    if (!(Strings.isNullOrEmpty((String) options.get(USERNAME_KEY)) || Strings.isNullOrEmpty((String) options.get(PASSWORD_KEY)))) {
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


  /**
   * {@inheritDoc}
   */
  @Override
  public boolean login() throws LoginException {
    log.debug("Login Called");
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean commit() throws LoginException {
    log.debug("Commit called");
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean abort() throws LoginException {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean logout() throws LoginException {
    return true;
  }
}
