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
package org.finos.fluxnova.connect.openai;

import org.finos.fluxnova.connect.spi.ConnectorRequest;
import java.util.List;

/**
 * OpenAI API request interface.
 * Provides a fluent API for configuring OpenAI Chat Completions requests.
 *
 * @author Fluxnova Team
 */
public interface OpenAIRequest extends ConnectorRequest<OpenAIResponse> {

  // Required parameters
  String PARAM_API_KEY = "apiKey";
  String PARAM_USER_MESSAGE = "userMessage";
  
  // Optional parameters
  String PARAM_BASE_URL = "baseUrl";
  String PARAM_MODEL = "model";
  String PARAM_SYSTEM_MESSAGE = "systemMessage";
  String PARAM_MESSAGE_HISTORY = "messageHistory";
  String PARAM_TEMPERATURE = "temperature";
  String PARAM_MAX_TOKENS = "maxTokens";
  String PARAM_STREAMING = "streaming";
  
  // Default values
  String DEFAULT_BASE_URL = "https://api.openai.com/v1";
  String DEFAULT_MODEL = "gpt-4o-mini";
  Double DEFAULT_TEMPERATURE = 0.7;
  Integer DEFAULT_MAX_TOKENS = 500;
  Boolean DEFAULT_STREAMING = true;

  /**
   * Sets the OpenAI API key (required).
   * For Azure OpenAI, this is the Azure resource key.
   *
   * @param apiKey the API key
   * @return this request
   */
  OpenAIRequest apiKey(String apiKey);

  /**
   * Sets the user message to send to the API (required).
   *
   * @param userMessage the user's message
   * @return this request
   */
  OpenAIRequest userMessage(String userMessage);

  /**
   * Sets the base URL for the OpenAI API endpoint.
   * Default: https://api.openai.com/v1
   * For Azure OpenAI: https://<resource-name>.openai.azure.com/openai/deployments/<deployment-id>
   * For local models: http://localhost:11434/v1 (Ollama) or http://localhost:1234/v1 (LM Studio)
   *
   * @param baseUrl the base URL
   * @return this request
   */
  OpenAIRequest baseUrl(String baseUrl);

  /**
   * Sets the model identifier.
   * Default: gpt-4o-mini
   * Examples: gpt-4o, gpt-4-turbo, claude-3-sonnet (via Azure), llama3 (local)
   *
   * @param model the model identifier
   * @return this request
   */
  OpenAIRequest model(String model);

  /**
   * Sets the system message to guide the AI's behavior.
   *
   * @param systemMessage the system message
   * @return this request
   */
  OpenAIRequest systemMessage(String systemMessage);

  /**
   * Sets the message history for multi-turn conversations.
   * Format: JSON array of message objects with "role" and "content" fields.
   * Example: [{"role":"user","content":"Hello"},{"role":"assistant","content":"Hi!"}]
   *
   * @param messageHistory the message history as JSON string
   * @return this request
   */
  OpenAIRequest messageHistory(String messageHistory);

  /**
   * Sets the message history for multi-turn conversations.
   *
   * @param messages the list of messages
   * @return this request
   */
  OpenAIRequest messageHistory(List<OpenAIMessage> messages);

  /**
   * Sets the temperature (0.0 to 2.0).
   * Higher values (e.g., 1.5) make output more random, lower values (e.g., 0.2) make it more deterministic.
   * Default: 0.7
   *
   * @param temperature the temperature
   * @return this request
   */
  OpenAIRequest temperature(Double temperature);

  /**
   * Sets the maximum number of tokens to generate.
   * Default: 500
   *
   * @param maxTokens the maximum tokens
   * @return this request
   */
  OpenAIRequest maxTokens(Integer maxTokens);

  /**
   * Enables or disables streaming.
   * When true, the connector will aggregate SSE chunks into a complete response.
   * Default: true
   *
   * @param streaming whether to use streaming
   * @return this request
   */
  OpenAIRequest streaming(Boolean streaming);

  /**
   * Executes the request and returns the response.
   *
   * @return the OpenAI response
   */
  @Override
  OpenAIResponse execute();

}
