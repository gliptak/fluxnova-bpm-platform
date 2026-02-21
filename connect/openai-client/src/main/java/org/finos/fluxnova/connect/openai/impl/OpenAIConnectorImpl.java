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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.finos.fluxnova.connect.impl.AbstractConnector;
import org.finos.fluxnova.connect.openai.OpenAIConnector;
import org.finos.fluxnova.connect.openai.OpenAIRequest;
import org.finos.fluxnova.connect.openai.OpenAIResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the OpenAI connector.
 *
 * @author Fluxnova Team
 */
public class OpenAIConnectorImpl extends AbstractConnector<OpenAIRequest, OpenAIResponse> implements OpenAIConnector {

  private static final OpenAIConnectorLogger LOG = OpenAILogger.OPENAI_LOGGER;
  private final CloseableHttpClient httpClient;
  private final ObjectMapper objectMapper;

  public OpenAIConnectorImpl() {
    super(OpenAIConnector.ID);
    this.httpClient = createHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  public OpenAIConnectorImpl(String connectorId) {
    super(connectorId);
    this.httpClient = createHttpClient();
    this.objectMapper = new ObjectMapper();
  }

  protected CloseableHttpClient createHttpClient() {
    return HttpClients.createSystem();
  }

  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  @Override
  public OpenAIRequest createRequest() {
    return new OpenAIRequestImpl(this);
  }

  @Override
  public OpenAIResponse execute(OpenAIRequest request) {
    try {
      // Get request parameters
      String apiKey = request.getRequestParameter(OpenAIRequest.PARAM_API_KEY);
      String userMessage = request.getRequestParameter(OpenAIRequest.PARAM_USER_MESSAGE);
      String baseUrl = getOrDefault(request, OpenAIRequest.PARAM_BASE_URL, OpenAIRequest.DEFAULT_BASE_URL);
      String model = getOrDefault(request, OpenAIRequest.PARAM_MODEL, OpenAIRequest.DEFAULT_MODEL);
      String systemMessage = request.getRequestParameter(OpenAIRequest.PARAM_SYSTEM_MESSAGE);
      String messageHistory = request.getRequestParameter(OpenAIRequest.PARAM_MESSAGE_HISTORY);
      Double temperature = getOrDefault(request, OpenAIRequest.PARAM_TEMPERATURE, OpenAIRequest.DEFAULT_TEMPERATURE);
      Integer maxTokens = getOrDefault(request, OpenAIRequest.PARAM_MAX_TOKENS, OpenAIRequest.DEFAULT_MAX_TOKENS);
      Boolean streaming = getOrDefault(request, OpenAIRequest.PARAM_STREAMING, OpenAIRequest.DEFAULT_STREAMING);

      // Validate required parameters
      if (apiKey == null || apiKey.trim().isEmpty()) {
        throw LOG.missingRequiredParameter(OpenAIRequest.PARAM_API_KEY);
      }
      if (userMessage == null || userMessage.trim().isEmpty()) {
        throw LOG.missingRequiredParameter(OpenAIRequest.PARAM_USER_MESSAGE);
      }

      // Build messages array
      List<Map<String, String>> messages = buildMessages(systemMessage, messageHistory, userMessage);

      // Build request body
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("model", model);
      requestBody.put("messages", messages);
      requestBody.put("temperature", temperature);
      requestBody.put("max_tokens", maxTokens);
      requestBody.put("stream", streaming);

      String requestJson = objectMapper.writeValueAsString(requestBody);
      LOG.sendingRequest(baseUrl, model);

      // Create HTTP request
      String endpoint = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
      HttpPost httpPost = new HttpPost(endpoint);
      httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
      httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
      httpPost.setEntity(new StringEntity(requestJson, StandardCharsets.UTF_8));

      // Execute request
      try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
        int statusCode = httpResponse.getStatusLine().getStatusCode();

        if (statusCode >= 400) {
          String errorBody = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
          throw LOG.httpError(statusCode, errorBody);
        }

        if (streaming) {
          // Handle streaming response (SSE)
          return handleStreamingResponse(httpResponse);
        } else {
          // Handle non-streaming response
          return handleNonStreamingResponse(httpResponse);
        }
      }

    } catch (Exception e) {
      throw LOG.executionError(e);
    }
  }

  /**
   * Handles streaming SSE responses by aggregating chunks into a complete response.
   */
  protected OpenAIResponse handleStreamingResponse(CloseableHttpResponse httpResponse) throws IOException {
    StringBuilder contentBuilder = new StringBuilder();
    String finishReason = null;
    String model = null;
    int promptTokens = 0;
    int completionTokens = 0;

    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(httpResponse.getEntity().getContent(), StandardCharsets.UTF_8))) {

      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("data: ")) {
          String data = line.substring(6).trim();

          // Check for end marker
          if ("[DONE]".equals(data)) {
            break;
          }

          try {
            JsonNode chunk = objectMapper.readTree(data);
            JsonNode choices = chunk.get("choices");

            if (choices != null && choices.isArray() && !choices.isEmpty()) {
              JsonNode firstChoice = choices.get(0);
              JsonNode delta = firstChoice.get("delta");

              // Extract content
              if (delta != null && delta.has("content")) {
                String content = delta.get("content").asText();
                contentBuilder.append(content);
              }

              // Extract finish reason
              if (firstChoice.has("finish_reason") && !firstChoice.get("finish_reason").isNull()) {
                finishReason = firstChoice.get("finish_reason").asText();
              }
            }

            // Extract model (from any chunk)
            if (model == null && chunk.has("model")) {
              model = chunk.get("model").asText();
            }

            // Extract usage (usually in last chunk)
            if (chunk.has("usage")) {
              JsonNode usage = chunk.get("usage");
              if (usage.has("prompt_tokens")) {
                promptTokens = usage.get("prompt_tokens").asInt();
              }
              if (usage.has("completion_tokens")) {
                completionTokens = usage.get("completion_tokens").asInt();
              }
            }
          } catch (Exception e) {
            LOG.errorParsingChunk(data, e);
          }
        }
      }
    }

    String content = contentBuilder.toString();
    int totalTokens = promptTokens + completionTokens;

    LOG.receivedResponse(content.length(), totalTokens);

    return new OpenAIResponseImpl(
        content,
        finishReason != null ? finishReason : "stop",
        promptTokens,
        completionTokens,
        totalTokens,
        model,
        null // rawResponse not available for streaming
    );
  }

  /**
   * Handles non-streaming JSON responses.
   */
  protected OpenAIResponse handleNonStreamingResponse(CloseableHttpResponse httpResponse) throws IOException {
    String responseBody = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
    JsonNode responseJson = objectMapper.readTree(responseBody);

    // Extract response fields
    String content = responseJson.at("/choices/0/message/content").asText("");
    String finishReason = responseJson.at("/choices/0/finish_reason").asText("stop");
    String model = responseJson.get("model").asText("");

    int promptTokens = responseJson.at("/usage/prompt_tokens").asInt(0);
    int completionTokens = responseJson.at("/usage/completion_tokens").asInt(0);
    int totalTokens = responseJson.at("/usage/total_tokens").asInt(0);

    LOG.receivedResponse(content.length(), totalTokens);

    return new OpenAIResponseImpl(
        content,
        finishReason,
        promptTokens,
        completionTokens,
        totalTokens,
        model,
        responseBody
    );
  }

  /**
   * Builds the messages array from system message, history, and user message.
   */
  protected List<Map<String, String>> buildMessages(String systemMessage, String messageHistory, String userMessage) throws IOException {
    List<Map<String, String>> messages = new ArrayList<>();

    // Add system message if provided
    if (systemMessage != null && !systemMessage.trim().isEmpty()) {
      Map<String, String> systemMsg = new HashMap<>();
      systemMsg.put("role", "system");
      systemMsg.put("content", systemMessage);
      messages.add(systemMsg);
    }

    // Add message history if provided
    if (messageHistory != null && !messageHistory.trim().isEmpty()) {
      try {
        JsonNode historyJson = objectMapper.readTree(messageHistory);
        if (historyJson.isArray()) {
          for (JsonNode msg : historyJson) {
            Map<String, String> historyMsg = new HashMap<>();
            historyMsg.put("role", msg.get("role").asText());
            historyMsg.put("content", msg.get("content").asText());
            messages.add(historyMsg);
          }
        }
      } catch (Exception e) {
        throw LOG.invalidMessageHistory(messageHistory, e);
      }
    }

    // Add current user message
    Map<String, String> userMsg = new HashMap<>();
    userMsg.put("role", "user");
    userMsg.put("content", userMessage);
    messages.add(userMsg);

    return messages;
  }

  /**
   * Helper method to get parameter value or default.
   */
  @SuppressWarnings("unchecked")
  protected <T> T getOrDefault(OpenAIRequest request, String paramName, T defaultValue) {
    Object value = request.getRequestParameter(paramName);
    return value != null ? (T) value : defaultValue;
  }

}
