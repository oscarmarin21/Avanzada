package com.avanzada.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    private boolean enabled = false;
    private String apiKey = "";
    private String endpoint = "https://api.openai.com/v1/chat/completions";
    private String model = "gpt-3.5-turbo";
    private int timeoutSeconds = 10;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank() && endpoint != null && !endpoint.isBlank();
    }
}
