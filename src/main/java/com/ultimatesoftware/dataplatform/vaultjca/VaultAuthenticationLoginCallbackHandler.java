package com.ultimatesoftware.dataplatform.vaultjca;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import org.apache.kafka.common.security.auth.AuthenticateCallbackHandler;

// https://strimzi.io/2018/11/16/using-vault-with-strimzi.html
public class VaultAuthenticationLoginCallbackHandler implements AuthenticateCallbackHandler {
  @Override
  public void configure(Map<String, ?> configs, String saslMechanism, List<AppConfigurationEntry> jaasConfigEntries) {

  }

  @Override
  public void close() {

  }

  @Override
  public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

  }
}
