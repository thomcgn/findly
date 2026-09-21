package dev.thomcgn.findly.listing;

import java.net.URI;

public interface ListingTransport {
  record Response(int status, String location, String html) {}

  Response get(URI uri);
}
