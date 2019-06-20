package com.ultimatesoftware.dataplatform.vaultjca.services;

import java.util.Map;

public interface VaultService {

  Map<String, String> getSecret(String path);
  void writeSecret(String path, Map<String, String> value);
}
