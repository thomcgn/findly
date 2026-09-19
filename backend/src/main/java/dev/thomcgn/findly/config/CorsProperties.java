package dev.thomcgn.findly.config;

import jakarta.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.cors")
@Validated
public record CorsProperties(
    @DefaultValue("http://localhost:3000") @NotEmpty List<String> allowedOrigins) {
  public CorsProperties {
    allowedOrigins = List.copyOf(allowedOrigins);
    for (String origin : allowedOrigins) {
      URI uri = URI.create(origin);
      if (!("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))
          || uri.getHost() == null
          || uri.getUserInfo() != null
          || uri.getRawQuery() != null
          || uri.getRawFragment() != null
          || !uri.getRawPath().isEmpty()
          || origin.contains("*")) {
        throw new IllegalArgumentException(
            "CORS origins must be explicit HTTP(S) origins without paths");
      }
    }
  }
}
