package com.ultimatesoftware.dataplatform.vaultjca.services;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Decorator Cache implementation based on Google's Guava Cache that delegates call to any
 * other {@link VaultService} implementation
 *
 * <p>Keeps an in memory a maximum of 100 entries that expire after 2 Min by default or
 * the user can set its own value using an environment variable VAULT_CACHE_TTL_MIN.</p>
 */
public class CacheDecoratorVaultService implements VaultService {

  private static final String VAULT_CACHE_TTL_MIN = "VAULT_CACHE_TTL_MIN";
  protected final Cache<String, Map<String, String>> cache;
  private final VaultService vaultService;

  public CacheDecoratorVaultService(VaultService vaultService) {
    Preconditions.checkArgument(!(vaultService instanceof CacheDecoratorVaultService), "Use any other implementation of VaultService as a delegator");
    long cacheTtl = (System.getenv(VAULT_CACHE_TTL_MIN) != null) ? Long.parseLong(System.getenv(VAULT_CACHE_TTL_MIN)) : 2;
    this.vaultService = vaultService;
    this.cache = CacheBuilder.newBuilder()
        .maximumSize(100)
        .expireAfterAccess(cacheTtl, TimeUnit.MINUTES)
        .build();
  }

  @Override
  public Map<String, String> getSecret(String path) {
    try {
      return cache.get(path, () -> vaultService.getSecret(path));
    } catch (ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeSecret(String path, Map<String, String> value) {
    vaultService.writeSecret(path, value);
    cache.put(path, value);
  }
}
