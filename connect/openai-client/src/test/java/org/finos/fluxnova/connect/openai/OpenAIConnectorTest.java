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

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.finos.fluxnova.connect.Connectors;
import org.finos.fluxnova.connect.openai.impl.OpenAIConnectorImpl;
import org.finos.fluxnova.connect.spi.Connector;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

/**
 * Test suite for OpenAI connector.
 *
 * @author Fluxnova Team
 */
public class OpenAIConnectorTest {

  private static final int PORT = 51235;
  private static final String BASE_URL = "http://localhost:" + PORT + "/v1";
  private static final String API_KEY = "test-api-key";

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(
      WireMockConfiguration.wireMockConfig().port(PORT));

  private OpenAIConnector connector;

  @Before
  public void createConnector() {
    connector = new OpenAIConnectorImpl();
  }

  @Test
  public void shouldDiscoverConnector() {
    Connector openai = Connectors.getConnector(OpenAIConnector.ID);
    assertThat(openai).isNotNull();
    assertThat(openai).isInstanceOf(OpenAIConnector.class);
  }

  @Test
  public void shouldExecuteSimpleChatCompletionRequest() {
    // Given
    String mockResponse = "{"
        + "\"id\":\"chatcmpl-123\","
        + "\"object\":\"chat.completion\","
        + "\"created\":1677652288,"
        + "\"model\":\"gpt-4o-mini\","
        + "\"choices\":[{"
        + "  \"index\":0,"
        + "  \"message\":{"
        + "    \"role\":\"assistant\","
        + "    \"content\":\"Hello! How can I help you today?\""
        + "  },"
        + "  \"finish_reason\":\"stop\""
        + "}],"
        + "\"usage\":{"
        + "  \"prompt_tokens\":10,"
        + "  \"completion_tokens\":8,"
        + "  \"total_tokens\":18"
        + "}"
        + "}";

    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(mockResponse)));

    // When
    OpenAIResponse response = connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .streaming(false)
        .execute();

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEqualTo("Hello! How can I help you today?");
    assertThat(response.getFinishReason()).isEqualTo("stop");
    assertThat(response.getModel()).isEqualTo("gpt-4o-mini");
    assertThat(response.getPromptTokens()).isEqualTo(10);
    assertThat(response.getCompletionTokens()).isEqualTo(8);
    assertThat(response.getTotalTokens()).isEqualTo(18);

    verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withHeader("Authorization", equalTo("Bearer " + API_KEY))
        .withHeader("Content-Type", equalTo("application/json")));
  }

  @Test
  public void shouldHandleStreamingResponse() {
    // Given
    String mockStreamResponse = "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1677652288,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\"Hello\"},\"finish_reason\":null}]}\n\n"
        + "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1677652288,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{\"content\":\" there!\"},\"finish_reason\":null}]}\n\n"
        + "data: {\"id\":\"chatcmpl-123\",\"object\":\"chat.completion.chunk\",\"created\":1677652288,\"model\":\"gpt-4o-mini\",\"choices\":[{\"index\":0,\"delta\":{},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}\n\n"
        + "data: [DONE]\n\n";

    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/event-stream")
            .withBody(mockStreamResponse)));

    // When
    OpenAIResponse response = connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .streaming(true)
        .execute();

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getContent()).isEqualTo("Hello there!");
    assertThat(response.getFinishReason()).isEqualTo("stop");
    assertThat(response.getModel()).isEqualTo("gpt-4o-mini");
    assertThat(response.getPromptTokens()).isEqualTo(10);
    assertThat(response.getCompletionTokens()).isEqualTo(5);
    assertThat(response.getTotalTokens()).isEqualTo(15);
  }

  @Test
  public void shouldIncludeSystemMessage() {
    // Given
    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"test\",\"model\":\"gpt-4o-mini\",\"choices\":[{\"message\":{\"content\":\"Response\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}")));

    // When
    connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .systemMessage("You are a helpful assistant.")
        .userMessage("Hello!")
        .streaming(false)
        .execute();

    // Then
    verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withRequestBody(containing("\"role\":\"system\""))
        .withRequestBody(containing("\"content\":\"You are a helpful assistant.\""))
        .withRequestBody(containing("\"role\":\"user\""))
        .withRequestBody(containing("\"content\":\"Hello!\"")));
  }

  @Test
  public void shouldIncludeMessageHistory() {
    // Given
    String messageHistory = "["
        + "{\"role\":\"user\",\"content\":\"What is the capital of France?\"},"
        + "{\"role\":\"assistant\",\"content\":\"Paris is the capital of France.\"}"
        + "]";

    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"test\",\"model\":\"gpt-4o-mini\",\"choices\":[{\"message\":{\"content\":\"Response\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}")));

    // When
    connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .messageHistory(messageHistory)
        .userMessage("What about Germany?")
        .streaming(false)
        .execute();

    // Then
    verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withRequestBody(containing("\"role\":\"user\""))
        .withRequestBody(containing("\"content\":\"What is the capital of France?\""))
        .withRequestBody(containing("\"role\":\"assistant\""))
        .withRequestBody(containing("\"content\":\"Paris is the capital of France.\""))
        .withRequestBody(containing("\"content\":\"What about Germany?\"")));
  }

  @Test
  public void shouldIncludeTemperatureAndMaxTokens() {
    // Given
    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"test\",\"model\":\"gpt-4o-mini\",\"choices\":[{\"message\":{\"content\":\"Response\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}")));

    // When
    connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .temperature(0.9)
        .maxTokens(1000)
        .streaming(false)
        .execute();

    // Then
    verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withRequestBody(containing("\"temperature\":0.9"))
        .withRequestBody(containing("\"max_tokens\":1000")));
  }

  @Test
  public void shouldUseCustomModel() {
    // Given
    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"test\",\"model\":\"gpt-4o\",\"choices\":[{\"message\":{\"content\":\"Response\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}")));

    // When
    OpenAIResponse response = connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .model("gpt-4o")
        .userMessage("Hello!")
        .streaming(false)
        .execute();

    // Then
    assertThat(response.getModel()).isEqualTo("gpt-4o");
    verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withRequestBody(containing("\"model\":\"gpt-4o\"")));
  }

  @Test(expected = RuntimeException.class)
  public void shouldFailWithoutApiKey() {
    // When/Then
    connector.createRequest()
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .execute();
  }

  @Test(expected = RuntimeException.class)
  public void shouldFailWithoutUserMessage() {
    // When/Then
    connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .execute();
  }

  @Test(expected = RuntimeException.class)
  public void shouldHandleApiError() {
    // Given
    String errorResponse = "{"
        + "\"error\":{"
        + "  \"message\":\"Invalid API key\","
        + "  \"type\":\"invalid_request_error\","
        + "  \"code\":\"invalid_api_key\""
        + "}"
        + "}";

    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(401)
            .withHeader("Content-Type", "application/json")
            .withBody(errorResponse)));

    // When/Then
    connector.createRequest()
        .apiKey("invalid-key")
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .execute();
  }

  @Test(expected = RuntimeException.class)
  public void shouldHandleRateLimitError() {
    // Given
    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(429)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"error\":{\"message\":\"Rate limit exceeded\",\"type\":\"rate_limit_error\"}}")));

    // When/Then
    connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .execute();
  }

  @Test
  public void shouldUseDefaultValues() {
    // Given
    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody("{\"id\":\"test\",\"model\":\"gpt-4o-mini\",\"choices\":[{\"message\":{\"content\":\"Response\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}}")));

    // When
    connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .streaming(false)
        .execute();

    // Then - verify defaults are applied
    verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
        .withRequestBody(containing("\"model\":\"gpt-4o-mini\""))
        .withRequestBody(containing("\"temperature\":0.7"))
        .withRequestBody(containing("\"max_tokens\":500")));
  }

  @Test
  public void shouldAccessResponseViaParameterMap() {
    // Given
    String mockResponse = "{"
        + "\"id\":\"test\","
        + "\"model\":\"gpt-4o-mini\","
        + "\"choices\":[{\"message\":{\"content\":\"Test response\"},\"finish_reason\":\"stop\"}],"
        + "\"usage\":{\"prompt_tokens\":10,\"completion_tokens\":5,\"total_tokens\":15}"
        + "}";

    stubFor(post(urlEqualTo("/v1/chat/completions"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(mockResponse)));

    // When
    OpenAIResponse response = connector.createRequest()
        .apiKey(API_KEY)
        .baseUrl(BASE_URL)
        .userMessage("Hello!")
        .streaming(false)
        .execute();

    // Then - verify response parameters are accessible
    assertThat(response.getContent()).isEqualTo("Test response");
    assertThat(response.getFinishReason()).isEqualTo("stop");
    assertThat(response.getModel()).isEqualTo("gpt-4o-mini");
    assertThat(response.getTotalTokens()).isEqualTo(15);
  }

}
