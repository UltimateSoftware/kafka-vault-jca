package com.ultimatesoftware.dataplatform.vaultjca.services;

import java.util.Map;

/**
 * Interface specifying a basic set of VAULT operations.
 *
 * Implemented by {@link HttpVaultService} and {@link CacheDecoratorVaultService}
 */
public interface VaultService {

  /**
   * Retrieve a secret in a key-value format from the specified path.
   *
   * The response (if any) otherwise an empty map
   * @param path the path in vault where to retrieve the secret
   * @return the key-value map secret or an empty map when no secret is found at the specified path
   */
  Map<String, String> getSecret(String path);

  /**
   * Creates/Updates a secret defined by the Map at the specified path.
   *
   * @param path the path to store to use as coordinates for the secret.
   * @param value entry to be created/updated.
   */
  void writeSecret(String path, Map<String, String> value);
}
