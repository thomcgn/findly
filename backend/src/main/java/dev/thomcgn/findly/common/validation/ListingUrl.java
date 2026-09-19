package dev.thomcgn.findly.common.validation;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.net.IDN;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

/** Syntactic allowlist only. DNS and connection validation belong to the listing client. */
public final class ListingUrl {
  private static final Set<String> HOSTS = Set.of("kleinanzeigen.de", "www.kleinanzeigen.de");

  private ListingUrl() {}

  public static URI parse(String input) {
    if (input == null || input.isBlank() || input.length() > 2048) {
      throw new AnalysisException(AnalysisErrorCode.INVALID_URL);
    }
    try {
      URI uri = URI.create(input.trim());
      if (!"https".equalsIgnoreCase(uri.getScheme())
          || uri.getHost() == null
          || uri.getRawUserInfo() != null
          || uri.getRawFragment() != null
          || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)) {
        throw new AnalysisException(AnalysisErrorCode.INVALID_URL);
      }
      String host = IDN.toASCII(uri.getHost(), IDN.USE_STD3_ASCII_RULES).toLowerCase(Locale.ROOT);
      if (!HOSTS.contains(host) || uri.getRawPath() == null || !uri.getRawPath().startsWith("/")) {
        throw new AnalysisException(AnalysisErrorCode.INVALID_URL);
      }
      return URI.create(
              "https://"
                  + host
                  + (uri.getPort() == -1 ? "" : ":" + uri.getPort())
                  + uri.getRawPath()
                  + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery()))
          .normalize();
    } catch (IllegalArgumentException ex) {
      throw new AnalysisException(AnalysisErrorCode.INVALID_URL);
    }
  }
}
