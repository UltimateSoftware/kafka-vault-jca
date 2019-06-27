package com.ultimatesoftware.dataplatform.vaultjca.services;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default Vault implementation of {@link VaultService} that uses vault java client by
 * <a href="https://github.com/BetterCloud/vault-java-driver">vault driver bettercloud</a>.
 *
 */
public class DefaultVaultService implements VaultService {
  private static final Logger log = LoggerFactory.getLogger(DefaultVaultService.class);

  private final Vault vault;

  public DefaultVaultService() {
    try {
      this.vault = new Vault(new VaultConfig().build());
    } catch (VaultException e) {
      log.error("Error creating Vault service", e);
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  DefaultVaultService(String vaultAddr, String token) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(vaultAddr));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token));
    try {
      this.vault = new Vault(new VaultConfig().address(vaultAddr).token(token).build());
    } catch (VaultException e) {
      log.error("Error building Vault", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public Map<String, String> getSecret(String path) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
    try {
      return vault.logical().read(path).getData();
    } catch (VaultException e) {
      if (e.getHttpStatusCode() == 404) {
        return Collections.EMPTY_MAP;
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void writeSecret(String path, Map<String, String> value) {
    try {
      vault.logical().write(path, Collections.unmodifiableMap(value));
    } catch (VaultException e) {
      throw new RuntimeException(e);
    }
  }
}
