/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.finos.fluxnova.connect.openai.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.connect.impl.AbstractConnectorRequest;
import org.finos.fluxnova.connect.openai.OpenAIConnector;
import org.finos.fluxnova.connect.openai.OpenAIMessage;
import org.finos.fluxnova.connect.openai.OpenAIRequest;
import org.finos.fluxnova.connect.openai.OpenAIResponse;

import java.util.List;

/**
 * Implementation of OpenAI request.
 *
 * @author Fluxnova Team
 */
public class OpenAIRequestImpl extends AbstractConnectorRequest<OpenAIResponse> implements OpenAIRequest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public OpenAIRequestImpl(OpenAIConnector connector) {
    super(connector);
  }

  @Override
  public OpenAIRequest apiKey(String apiKey) {
    setRequestParameter(PARAM_API_KEY, apiKey);
    return this;
  }

  @Override
  public OpenAIRequest userMessage(String userMessage) {
    setRequestParameter(PARAM_USER_MESSAGE, userMessage);
    return this;
  }

  @Override
  public OpenAIRequest baseUrl(String baseUrl) {
    setRequestParameter(PARAM_BASE_URL, baseUrl);
    return this;
  }

  @Override
  public OpenAIRequest model(String model) {
    setRequestParameter(PARAM_MODEL, model);
    return this;
  }

  @Override
  public OpenAIRequest systemMessage(String systemMessage) {
    setRequestParameter(PARAM_SYSTEM_MESSAGE, systemMessage);
    return this;
  }

  @Override
  public OpenAIRequest messageHistory(String messageHistory) {
    setRequestParameter(PARAM_MESSAGE_HISTORY, messageHistory);
    return this;
  }

  @Override
  public OpenAIRequest messageHistory(List<OpenAIMessage> messages) {
    try {
      String messageHistoryJson = objectMapper.writeValueAsString(messages);
      setRequestParameter(PARAM_MESSAGE_HISTORY, messageHistoryJson);
    } catch (Exception e) {
      throw new RuntimeException("Failed to serialize message history", e);
    }
    return this;
  }

  @Override
  public OpenAIRequest temperature(Double temperature) {
    setRequestParameter(PARAM_TEMPERATURE, temperature);
    return this;
  }

  @Override
  public OpenAIRequest maxTokens(Integer maxTokens) {
    setRequestParameter(PARAM_MAX_TOKENS, maxTokens);
    return this;
  }

  @Override
  public OpenAIRequest streaming(Boolean streaming) {
    setRequestParameter(PARAM_STREAMING, streaming);
    return this;
  }

  @Override
  protected boolean isRequestValid() {
    // Validate required parameters
    String apiKey = getRequestParameter(PARAM_API_KEY);
    String userMessage = getRequestParameter(PARAM_USER_MESSAGE);

    return apiKey != null && !apiKey.trim().isEmpty()
        && userMessage != null && !userMessage.trim().isEmpty();
  }

}
