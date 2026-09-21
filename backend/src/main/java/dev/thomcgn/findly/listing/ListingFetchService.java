package dev.thomcgn.findly.listing;

import dev.thomcgn.findly.common.validation.ListingUrl;
import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.net.URI;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ListingFetchService {
  private final ListingTransport transport;
  private final ListingParser parser;

  public ListingFetchService(ListingTransport transport, ListingParser parser) {
    this.transport = transport;
    this.parser = parser;
  }

  public ListingFetchResult fetch(String url) {
    return parser.parse(fetchHtml(url));
  }

  public String fetchHtml(String url) {
    URI uri = ListingUrl.parse(url);
    for (int redirects = 0; ; redirects++) {
      if (Thread.currentThread().isInterrupted())
        throw new AnalysisException(AnalysisErrorCode.ANALYSIS_TIMEOUT);
      var response = transport.get(uri);
      if (response.status() == 200) return response.html();
      if (!Set.of(301, 302, 303, 307, 308).contains(response.status())
          || redirects == 3
          || response.location() == null)
        throw new AnalysisException(AnalysisErrorCode.LISTING_REDIRECT_FAILED);
      try {
        uri = ListingUrl.parse(uri.resolve(response.location()).toString());
      } catch (IllegalArgumentException | AnalysisException ex) {
        throw new AnalysisException(AnalysisErrorCode.LISTING_REDIRECT_FAILED);
      }
    }
  }
}
