package com.ultimatesoftware.dataplatform.vaultjca.services;

import java.util.Map;

public interface VaultService {

  public Map<String, String> getSecret(String path);
  public void writeSecret(String path, Map<String, String> value);
}
