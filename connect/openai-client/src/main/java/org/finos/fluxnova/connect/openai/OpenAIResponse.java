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

import org.finos.fluxnova.connect.spi.ConnectorResponse;

/**
 * OpenAI API response interface.
 * Contains the response data from the Chat Completions API.
 *
 * @author Fluxnova Team
 */
public interface OpenAIResponse extends ConnectorResponse {

  // Response parameter names
  String PARAM_CONTENT = "content";
  String PARAM_FINISH_REASON = "finishReason";
  String PARAM_PROMPT_TOKENS = "promptTokens";
  String PARAM_COMPLETION_TOKENS = "completionTokens";
  String PARAM_TOTAL_TOKENS = "totalTokens";
  String PARAM_MODEL = "model";
  String PARAM_RAW_RESPONSE = "rawResponse";

  /**
   * Gets the generated content/message from the assistant.
   *
   * @return the assistant's message content
   */
  String getContent();

  /**
   * Gets the reason the generation finished.
   * Values: "stop" (natural completion), "length" (max tokens reached), "content_filter" (filtered)
   *
   * @return the finish reason
   */
  String getFinishReason();

  /**
   * Gets the number of tokens in the prompt.
   *
   * @return prompt token count
   */
  Integer getPromptTokens();

  /**
   * Gets the number of tokens in the completion.
   *
   * @return completion token count
   */
  Integer getCompletionTokens();

  /**
   * Gets the total number of tokens used (prompt + completion).
   *
   * @return total token count
   */
  Integer getTotalTokens();

  /**
   * Gets the model that was used for generation.
   *
   * @return the model identifier
   */
  String getModel();

  /**
   * Gets the raw JSON response from the API.
   *
   * @return the raw JSON response
   */
  String getRawResponse();

}
