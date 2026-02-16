# Fluxnova OpenAI Connector

OpenAI API connector for Fluxnova BPM Platform. Enables BPMN processes to integrate with OpenAI Chat Completions API and compatible endpoints (Azure OpenAI, local LLMs).

## Features

- **Chat Completions API** support (OpenAI, Azure OpenAI, local models)
- **Fluent API** for easy request configuration
- **Streaming support** with automatic buffering (Server-Sent Events aggregation)
- **Multi-turn conversations** via message history
- **Flexible model selection** - supports any model identifier (GPT-4o, Claude, Llama3, etc.)
- **Optional parameters** with sensible defaults
- **Comprehensive error handling** and logging
- **WireMock-tested** with 15+ test scenarios

## Quick Start

### Basic Usage

```java
import org.finos.fluxnova.connect.Connectors;
import org.finos.fluxnova.connect.openai.OpenAIConnector;
import org.finos.fluxnova.connect.openai.OpenAIResponse;

// Get connector instance
OpenAIConnector connector = Connectors.getConnector("openai-connector");

// Simple request
OpenAIResponse response = connector.createRequest()
    .apiKey("sk-...")
    .userMessage("What is the capital of France?")
    .execute();

System.out.println(response.getContent());  // "Paris is the capital of France."
```

### Using in BPMN (Service Task)

#### Element Template Configuration

Use the `openai-connector.json` element template in Fluxnova Modeler:

1. Select a Service Task
2. Click "Change Type" → "OpenAI Connector"
3. Configure required fields:
   - **API Key**: `sk-...` (or process variable `${apiKey}`)
   - **User Message**: Your prompt text

Optional parameters:
- **Base URL**: Default `https://api.openai.com/v1`
- **Model**: Default `gpt-4o-mini`
- **System Message**: Guide AI behavior
- **Message History**: JSON array of previous messages
- **Temperature**: 0.0-2.0 (default 0.7)
- **Max Tokens**: Default 500
- **Streaming**: Default true

#### BPMN XML Configuration

```xml
<bpmn:serviceTask id="AskOpenAI" name="Ask OpenAI">
  <bpmn:extensionElements>
    <camunda:connector>
      <camunda:inputOutput>
        <camunda:inputParameter name="apiKey">${secrets.openaiKey}</camunda:inputParameter>
        <camunda:inputParameter name="userMessage">What is 2+2?</camunda:inputParameter>
        <camunda:inputParameter name="model">gpt-4o-mini</camunda:inputParameter>
        <camunda:outputParameter name="aiResponse">${content}</camunda:outputParameter>
        <camunda:outputParameter name="tokensUsed">${totalTokens}</camunda:outputParameter>
      </camunda:inputOutput>
      <camunda:connectorId>openai-connector</camunda:connectorId>
    </camunda:connector>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

## API Reference

### Request Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `apiKey` | String | ✅ | - | OpenAI API key |
| `userMessage` | String | ✅ | - | User's prompt |
| `baseUrl` | String | | `https://api.openai.com/v1` | API endpoint |
| `model` | String | | `gpt-4o-mini` | Model identifier |
| `systemMessage` | String | | - | System prompt |
| `messageHistory` | String | | - | JSON array of messages |
| `temperature` | Double | | `0.7` | Randomness (0.0-2.0) |
| `maxTokens` | Integer | | `500` | Max completion tokens |
| `streaming` | Boolean | | `true` | Use SSE streaming |

### Response Fields

| Field | Type | Description |
|-------|------|-------------|
| `content` | String | AI-generated message |
| `finishReason` | String | Completion reason: `stop`, `length`, `content_filter` |
| `promptTokens` | Integer | Input token count |
| `completionTokens` | Integer | Output token count |
| `totalTokens` | Integer | Total tokens used |
| `model` | String | Model that generated response |
| `rawResponse` | String | Complete JSON response (non-streaming only) |

## Advanced Usage

### Multi-Turn Conversations

```java
String history = "[" +
    "{\"role\":\"user\",\"content\":\"What is the capital of France?\"}," +
    "{\"role\":\"assistant\",\"content\":\"Paris is the capital of France.\"}" +
"]";

OpenAIResponse response = connector.createRequest()
    .apiKey(apiKey)
    .messageHistory(history)
    .userMessage("What about Germany?")
    .execute();
```

### System Message for Role Definition

```java
OpenAIResponse response = connector.createRequest()
    .apiKey(apiKey)
    .systemMessage("You are a concise technical assistant. Answer in 2 sentences or less.")
    .userMessage("Explain REST APIs")
    .execute();
```

### Azure OpenAI

```java
OpenAIResponse response = connector.createRequest()
    .apiKey(azureApiKey)
    .baseUrl("https://my-resource.openai.azure.com/openai/deployments/gpt-4")
    .model("gpt-4")  // deployment name
    .userMessage("Hello Azure!")
    .execute();
```

### Local Models (Ollama)

```java
OpenAIResponse response = connector.createRequest()
    .apiKey("not-needed")  // local models often don't require keys
    .baseUrl("http://localhost:11434/v1")
    .model("llama3")
    .userMessage("Hello local LLM!")
    .execute();
```

### BPMN Process Variable Conversation

Store conversation history in process variables:

```xml
<!-- First call -->
<camunda:inputParameter name="userMessage">What is 2+2?</camunda:inputParameter>
<camunda:outputParameter name="response1">${content}</camunda:outputParameter>
<camunda:outputParameter name="history">
  [{"role":"user","content":"What is 2+2?"},{"role":"assistant","content":"${content}"}]
</camunda:outputParameter>

<!-- Second call -->
<camunda:inputParameter name="messageHistory">${history}</camunda:inputParameter>
<camunda:inputParameter name="userMessage">What is 3+3?</camunda:inputParameter>
```

## Error Handling

### Common Errors

| HTTP Status | Error Type | Description | Solution |
|-------------|------------|-------------|----------|
| 401 | `invalid_api_key` | Invalid API key | Verify API key |
| 429 | `rate_limit_error` | Too many requests | Implement retry with backoff |
| 400 | `invalid_request_error` | Bad request | Check parameters |
| 500 | `server_error` | OpenAI service issue | Retry later |

### BPMN Error Handling

```xml
<bpmn:serviceTask id="AskAI">
  <!-- ... connector config ... -->
</bpmn:serviceTask>

<bpmn:boundaryEvent id="ErrorEvent" attachedToRef="AskAI">
  <bpmn:errorEventDefinition errorRef="Error_OpenAI" />
</bpmn:boundaryEvent>

<bpmn:sequenceFlow sourceRef="ErrorEvent" targetRef="Retry" />
```

## Testing

The connector includes comprehensive WireMock tests covering:
- Simple requests
- Streaming responses
- System messages
- Message history
- Parameter validation
- Error handling (401, 429, 500)
- Default value behavior

Run tests:
```bash
mvn test
```

## Configuration

### Environment Variables (Optional)

```bash
export OPENAI_API_KEY="sk-..."
export OPENAI_BASE_URL="https://api.openai.com/v1"
export OPENAI_MODEL="gpt-4o-mini"
```

Access in BPMN:
```xml
<camunda:inputParameter name="apiKey">${environment.get("OPENAI_API_KEY")}</camunda:inputParameter>
```

### Secrets Management

Use Fluxnova's secret management or external vaults:

```java
// Java delegate
String apiKey = execution.getVariable("secrets.openaiKey");
```

## Performance Considerations

- **Streaming**: Enabled by default, buffers full response before returning to BPMN
- **Max Tokens**: Limit set to 500 by default to control costs
- **Timeouts**: Standard HTTP client timeouts apply
- **Rate Limits**: OpenAI has rate limits (60 req/min for free tier)

## Compatibility

- **OpenAI API v1** ✅
- **Azure OpenAI** ✅
- **Ollama** (local) ✅
- **LM Studio** (local) ✅
- **Text Generation WebUI** ✅
- Any OpenAI-compatible endpoint with `/v1/chat/completions`

## Dependencies

- `fluxnova-connect-core`: 1.2.0-SNAPSHOT
- `httpclient`: Apache HttpComponents
- `jackson-databind`: JSON processing
- `wiremock`: Testing only

## License

Apache License 2.0

Copyright Camunda Services GmbH / Fluxnova Project

## Support

- Documentation: See `OPENAI_CONNECTOR_IMPLEMENTATION.md` for detailed architecture
- Element Template: `resources/element-templates/openai-connector.json`
- Examples: `resources/element-templates/openai-connector-examples.bpmn`
- Issues: Report via GitHub Issues

## Roadmap

Future enhancements (not in v1):
- Function calling support
- Vision API (image inputs)
- Audio/TTS integration
- Embeddings API
- Fine-tuning support
- Advanced streaming with partial updates
