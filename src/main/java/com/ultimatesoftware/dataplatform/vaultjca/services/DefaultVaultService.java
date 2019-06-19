package com.ultimatesoftware.dataplatform.vaultjca.services;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import java.util.Collections;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  protected DefaultVaultService(String vaultAddr, String token) {
    try {
      this.vault = new Vault(new VaultConfig().address(vaultAddr).token(token).build());
    } catch (VaultException e) {
      log.error("Error building Vault", e);
      throw new RuntimeException(e);
    }
  }

  @Override
  public Map<String, String> getSecret(String path) {
    try {
      return vault.logical().read(path).getData();
    } catch (VaultException e) {
      if (e.getHttpStatusCode() == 404) {
        return Collections.EMPTY_MAP;
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeSecret(String path, Map<String, String> value) {
    try {
      vault.logical().write(path, Collections.unmodifiableMap(value));
    } catch (VaultException e) {
      throw new RuntimeException(e);
    }
  }
}
