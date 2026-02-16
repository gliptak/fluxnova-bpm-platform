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

import org.finos.fluxnova.connect.ConnectorRequestException;
import org.finos.fluxnova.connect.impl.ConnectLogger;

/**
 * OpenAI connector logger implementation.
 *
 * @author Fluxnova Team
 */
public class OpenAIConnectorLogger extends ConnectLogger {

  public void sendingRequest(String baseUrl, String model) {
    logDebug("001", "Sending OpenAI request to '{}' with model '{}'", baseUrl, model);
  }

  public void receivedResponse(int contentLength, int totalTokens) {
    logDebug("002", "Received OpenAI response: {} chars, {} tokens", contentLength, totalTokens);
  }

  public void errorParsingChunk(String chunk, Exception cause) {
    logWarn("003", "Error parsing SSE chunk: {}", chunk, cause);
  }

  public ConnectorRequestException missingRequiredParameter(String paramName) {
    return new ConnectorRequestException(exceptionMessage("004", "Required parameter '{}' is missing or empty", paramName));
  }

  public ConnectorRequestException httpError(int statusCode, String errorBody) {
    return new ConnectorRequestException(exceptionMessage("005", "OpenAI API returned error: HTTP {} - {}", statusCode, errorBody));
  }

  public ConnectorRequestException executionError(Exception cause) {
    return new ConnectorRequestException(exceptionMessage("006", "Failed to execute OpenAI request: {}", cause.getMessage()), cause);
  }

  public ConnectorRequestException invalidMessageHistory(String messageHistory, Exception cause) {
    return new ConnectorRequestException(exceptionMessage("007", "Invalid message history JSON: {}", messageHistory), cause);
  }

}
