/**
 * Copyright 2016 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.lib.security.http;

import com.google.common.annotations.VisibleForTesting;
import com.streamsets.datacollector.util.Configuration;
import com.streamsets.pipeline.api.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class RemoteSSOService extends AbstractSSOService {
  private static final Logger LOG = LoggerFactory.getLogger(RemoteSSOService.class);

  public static final String DPM_BASE_URL_CONFIG = "dpm.base.url";
  public static final String DPM_BASE_URL_DEFAULT = "http://localhost:18631";

  public static final String SECURITY_SERVICE_APP_AUTH_TOKEN_CONFIG = CONFIG_PREFIX + "appAuthToken";
  public static final String SECURITY_SERVICE_COMPONENT_ID_CONFIG = CONFIG_PREFIX + "componentId";
  public static final String SECURITY_SERVICE_CONNECTION_TIMEOUT_CONFIG = CONFIG_PREFIX + "connectionTimeout.millis";
  public static final int DEFAULT_SECURITY_SERVICE_CONNECTION_TIMEOUT = 10000;
  public static final String DPM_ENABLED = CONFIG_PREFIX + "enabled";
  public static final boolean DPM_ENABLED_DEFAULT = false;
  public static final String DPM_REGISTRATION_RETRY_ATTEMPTS = "registration.retry.attempts";
  public static final int DPM_REGISTRATION_RETRY_ATTEMPTS_DEFAULT = 5;

  RestClient.Builder registerClientBuilder;
  RestClient.Builder userAuthClientBuilder;
  RestClient.Builder appAuthClientBuilder;
  private String appToken;
  private String componentId;
  private volatile int connTimeout;
  private int dpmRegistrationMaxRetryAttempts;

  @Override
  public void setConfiguration(Configuration conf) {
    super.setConfiguration(conf);
    String dpmBaseUrl = getValidURL(conf.get(DPM_BASE_URL_CONFIG, DPM_BASE_URL_DEFAULT));
    String baseUrl = dpmBaseUrl + "security";

    Utils.checkArgument(
        baseUrl.toLowerCase().startsWith("http:") || baseUrl.toLowerCase().startsWith("https:"),
        Utils.formatL("Security service base URL must be HTTP/HTTPS '{}'", baseUrl)
    );
    if (baseUrl.toLowerCase().startsWith("http://")) {
      LOG.warn("Security service base URL is not secure '{}'", baseUrl);
    }
    setLoginPageUrl(baseUrl + "/login");
    setLogoutUrl(baseUrl + "/_logout");

    componentId = conf.get(SECURITY_SERVICE_COMPONENT_ID_CONFIG, null);
    appToken = conf.get(SECURITY_SERVICE_APP_AUTH_TOKEN_CONFIG, null);
    connTimeout = conf.get(SECURITY_SERVICE_CONNECTION_TIMEOUT_CONFIG, DEFAULT_SECURITY_SERVICE_CONNECTION_TIMEOUT);
    dpmRegistrationMaxRetryAttempts = conf.get(DPM_REGISTRATION_RETRY_ATTEMPTS,
        DPM_REGISTRATION_RETRY_ATTEMPTS_DEFAULT);

    registerClientBuilder = RestClient.builder(baseUrl)
        .csrf(true)
        .json(true)
        .path("public-rest/v1/components/registration")
        .timeout(connTimeout);
    userAuthClientBuilder = RestClient.builder(baseUrl)
        .csrf(true)
        .json(true)
        .path("rest/v1/validateAuthToken/user")
        .timeout(connTimeout);
    appAuthClientBuilder = RestClient.builder(baseUrl)
        .csrf(true)
        .json(true)
        .path("rest/v1/validateAuthToken/component")
        .timeout(connTimeout);
  }

  @VisibleForTesting
  public RestClient.Builder getRegisterClientBuilder() {
    return registerClientBuilder;
  }

  @VisibleForTesting
  public RestClient.Builder getUserAuthClientBuilder() {
    return userAuthClientBuilder;
  }

  @VisibleForTesting
  public RestClient.Builder getAppAuthClientBuilder() {
    return appAuthClientBuilder;
  }

  @VisibleForTesting
  void sleep(int secs) {
    try {
      Thread.sleep(secs * 1000);
    } catch (InterruptedException ex) {
      String msg = "Interrupted while attempting DPM registration";
      LOG.error(msg);
      throw new RuntimeException(msg);
    }
  }

  void updateConnectionTimeout(RestClient.Response response) {
    String timeout = response.getHeader(SSOConstants.X_APP_CONNECTION_TIMEOUT);
    connTimeout = (timeout == null) ? connTimeout: Integer.parseInt(timeout);
  }

  @Override
  public void register(Map<String, String> attributes) {
    if (appToken.isEmpty() || componentId.isEmpty()) {
      if (appToken.isEmpty()) {
        LOG.warn("Skipping component registration to DPM, application auth token is not set");
      }
      if (componentId.isEmpty()) {
        LOG.warn("Skipping component registration to DPM, component ID is not set");
      }
      throw new RuntimeException("Registration to DPM not done, missing component ID or app auth token");
    } else {
      LOG.debug("Doing component ID '{}' registration with DPM", componentId);

      Map<String, Object> registrationData = new HashMap<>();
      registrationData.put("authToken", appToken);
      registrationData.put("componentId", componentId);
      registrationData.put("attributes", attributes);

      int delaySecs = 1;
      int attempts = 0;
      boolean registered = false;

      //When Load Balancer(HAProxy or ELB) is used, it will take couple of seconds for load balancer to access
      //security service. So we are retrying registration couple of times until server is accessible via load balancer.
      while (attempts < dpmRegistrationMaxRetryAttempts) {
        if (attempts > 0) {
          delaySecs = delaySecs * 2;
          delaySecs = Math.min(delaySecs, 16);
          String msg = Utils.format("DPM registration attempt '{}', waiting for '{}' seconds before retrying ...",
              attempts,
              delaySecs
          );
          LOG.warn(msg);
          System.out.println(msg);
          sleep(delaySecs);
        }
        attempts++;
        try {
          RestClient restClient = getRegisterClientBuilder().build();
          RestClient.Response response = restClient.post(registrationData);
          if (response.getStatus() == HttpURLConnection.HTTP_OK) {
            updateConnectionTimeout(response);
            LOG.info("Registered with DPM");
            registered = true;
            break;
          } else if (response.getStatus() == HttpURLConnection.HTTP_UNAVAILABLE) {
            String msg = Utils.format("DPM Registration unavailable");
            LOG.warn(msg);
            System.out.println(msg);
          }  else {
            String msg = Utils.format("Failed to registered to DPM, HTTP status '{}': {}", response.getStatus(),
                response.getError());
            LOG.warn(msg);
            throw new RuntimeException(msg);
          }
        } catch (IOException ex) {
          String msg = Utils.format("DPM Registration failed: {}", ex.getMessage());
          LOG.warn(msg, ex);
          System.out.println(msg);
        }
      }
      if (!registered) {
        String msg = Utils.format("DPM registration failed after '{}' attempts", attempts);
        LOG.warn(msg);
        throw new RuntimeException(msg);
      }
    }
  }

  public void setComponentId(String componentId) {
    componentId = (componentId != null) ? componentId.trim() : null;
    Utils.checkArgument(componentId != null && !componentId.isEmpty(), "Component ID cannot be NULL or empty");
    this.componentId = componentId;
    registerClientBuilder.componentId(componentId);
    userAuthClientBuilder.componentId(componentId);
    appAuthClientBuilder.componentId(componentId);
  }

  public void setApplicationAuthToken(String appToken) {
    appToken = (appToken != null) ? appToken.trim() : null;
    this.appToken = appToken;
    registerClientBuilder.appAuthToken(appToken);
    userAuthClientBuilder.appAuthToken(appToken);
    appAuthClientBuilder.appAuthToken(appToken);
  }

  protected SSOPrincipal validateUserTokenWithSecurityService(String userAuthToken) {
    ValidateUserAuthTokenJson authTokenJson = new ValidateUserAuthTokenJson();
    authTokenJson.setAuthToken(userAuthToken);
    SSOPrincipalJson principal = null;
    try {
      RestClient restClient = getUserAuthClientBuilder().build();
      RestClient.Response response = restClient.post(authTokenJson);
      if (response.getStatus() == HttpURLConnection.HTTP_OK) {
        updateConnectionTimeout(response);
        principal = response.getData(SSOPrincipalJson.class);
      } else if (response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN) {
        principal = null;
      } else {
        throw new RuntimeException(Utils.format(
            "Could not validate user token '{}', HTTP status '{}' message: {}",
            null,
            response.getStatus(),
            response.getError()
        ));
      }
    } catch (IOException ex){
      throw new RuntimeException(Utils.format("Could not do user token validation: {}", ex.toString()), ex);
    }
    if (principal != null) {
      principal.setTokenStr(userAuthToken);
      principal.lock();
      LOG.debug("Validated user auth token for '{}'", principal.getPrincipalId());
    }
    return principal;
  }

  protected SSOPrincipal validateAppTokenWithSecurityService(String authToken, String componentId) {
    ValidateComponentAuthTokenJson authTokenJson = new ValidateComponentAuthTokenJson();
    authTokenJson.setComponentId(componentId);
    authTokenJson.setAuthToken(authToken);
    SSOPrincipalJson principal = null;
    try {
      RestClient restClient = getAppAuthClientBuilder().build();
      RestClient.Response response = restClient.post(authTokenJson);
      if (response.getStatus() == HttpURLConnection.HTTP_OK) {
        updateConnectionTimeout(response);
        principal = response.getData(SSOPrincipalJson.class);
      } else if (response.getStatus() == HttpURLConnection.HTTP_FORBIDDEN) {
        principal = null;
      } else {
        throw new RuntimeException(Utils.format(
            "Could not validate app token '{}', HTTP status '{}' message: {}",
            null,
            response.getStatus(),
            response.getError()
        ));
      }
    } catch (IOException ex){
      throw new RuntimeException(Utils.format("Could not do app token validation: {}", ex.toString(), ex));
    }
    if (principal != null) {
      principal.setTokenStr(authToken);
      principal.lock();
      LOG.debug("Validated app auth token for '{}'", principal.getPrincipalId());
    }
    return principal;
  }

  public static String getValidURL(String url) {
    if (!url.endsWith("/")) {
      url += "/";
    }
    return url;
  }

  @VisibleForTesting
  int getConnectionTimeout() {
    return connTimeout;
  }

}
