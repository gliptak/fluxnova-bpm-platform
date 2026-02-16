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

import org.finos.fluxnova.connect.openai.OpenAIResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of OpenAI response.
 *
 * @author Fluxnova Team
 */
public class OpenAIResponseImpl implements OpenAIResponse {

  protected final String content;
  protected final String finishReason;
  protected final Integer promptTokens;
  protected final Integer completionTokens;
  protected final Integer totalTokens;
  protected final String model;
  protected final String rawResponse;
  protected final Map<String, Object> responseParameters;

  public OpenAIResponseImpl(String content, String finishReason, Integer promptTokens,
                            Integer completionTokens, Integer totalTokens, String model, String rawResponse) {
    this.content = content;
    this.finishReason = finishReason;
    this.promptTokens = promptTokens;
    this.completionTokens = completionTokens;
    this.totalTokens = totalTokens;
    this.model = model;
    this.rawResponse = rawResponse;

    // Build response parameters map
    this.responseParameters = new HashMap<>();
    this.responseParameters.put(PARAM_CONTENT, content);
    this.responseParameters.put(PARAM_FINISH_REASON, finishReason);
    this.responseParameters.put(PARAM_PROMPT_TOKENS, promptTokens);
    this.responseParameters.put(PARAM_COMPLETION_TOKENS, completionTokens);
    this.responseParameters.put(PARAM_TOTAL_TOKENS, totalTokens);
    this.responseParameters.put(PARAM_MODEL, model);
    this.responseParameters.put(PARAM_RAW_RESPONSE, rawResponse);
  }

  @Override
  public String getContent() {
    return content;
  }

  @Override
  public String getFinishReason() {
    return finishReason;
  }

  @Override
  public Integer getPromptTokens() {
    return promptTokens;
  }

  @Override
  public Integer getCompletionTokens() {
    return completionTokens;
  }

  @Override
  public Integer getTotalTokens() {
    return totalTokens;
  }

  @Override
  public String getModel() {
    return model;
  }

  @Override
  public String getRawResponse() {
    return rawResponse;
  }

  @Override
  public Map<String, Object> getResponseParameters() {
    return responseParameters;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getResponseParameter(String name) {
    return (V) responseParameters.get(name);
  }

  @Override
  public String toString() {
    return "OpenAIResponse{" +
        "content='" + content + '\'' +
        ", finishReason='" + finishReason + '\'' +
        ", promptTokens=" + promptTokens +
        ", completionTokens=" + completionTokens +
        ", totalTokens=" + totalTokens +
        ", model='" + model + '\'' +
        '}';
  }

}
