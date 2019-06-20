package com.ultimatesoftware.dataplatform.vaultjca.services;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;

public class CacheDecoratorVaultServiceTest {

  private VaultService vaultService = mock(VaultService.class);
  private CacheDecoratorVaultService cacheDecoratorVaultService = new CacheDecoratorVaultService(vaultService);
  private String KEY = "password";
  private String VALUE = "secret";
  private ImmutableMap<String, String> entry = ImmutableMap.of(KEY, VALUE);

  @Test
  public void shouldCacheValuesForSpecificPath() {
    String path = "some/vault/path";
    when(vaultService.getSecret(path)).thenReturn(entry);
    Map<String, String> secret = cacheDecoratorVaultService.getSecret(path);
    assertThat(secret, hasEntry(KEY, VALUE));
    cacheDecoratorVaultService.getSecret(path);
    verify(vaultService, times(1)).getSecret(eq(path));
  }

  @Test
  public void shouldWriteAndCacheValue() {
    String path = "some/other/path";
    cacheDecoratorVaultService.writeSecret(path, entry);
    verify(vaultService).writeSecret(eq(path), eq(entry));
    assertThat(cacheDecoratorVaultService.cache.size(), greaterThan(0L));
    cacheDecoratorVaultService.getSecret(path);
    verify(vaultService, never()).getSecret(eq(path));
  }

}

