package dev.thomcgn.findly.listing;

import dev.thomcgn.findly.error.AnalysisErrorCode;
import dev.thomcgn.findly.error.AnalysisException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;
import org.apache.hc.client5.http.DnsResolver;

/**
 * Resolution used by the socket connector itself, never a preflight followed by a second lookup.
 */
public final class PublicAddressResolver implements DnsResolver {
  @FunctionalInterface
  public interface Lookup {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }

  private final Lookup lookup;

  public PublicAddressResolver(Lookup lookup) {
    this.lookup = lookup;
  }

  @Override
  public InetAddress[] resolve(String host) throws UnknownHostException {
    if (!Set.of("kleinanzeigen.de", "www.kleinanzeigen.de").contains(host)) {
      throw new AnalysisException(AnalysisErrorCode.LISTING_TARGET_BLOCKED);
    }
    InetAddress[] addresses = lookup.resolve(host);
    if (addresses == null || addresses.length == 0) throw new UnknownHostException("No addresses");
    for (InetAddress address : addresses) {
      if (!isPublic(address)) throw new AnalysisException(AnalysisErrorCode.LISTING_TARGET_BLOCKED);
    }
    return addresses.clone();
  }

  @Override
  public String resolveCanonicalHostname(String host) {
    return host;
  }

  static boolean isPublic(InetAddress address) {
    if (address == null
        || address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) return false;
    byte[] b = address.getAddress();
    int a = b[0] & 255;
    int c = b[1] & 255;
    if (b.length == 4) {
      return !(a == 0
          || a == 10
          || a == 127
          || a >= 224
          || (a == 100 && c >= 64 && c <= 127)
          || (a == 169 && c == 254)
          || (a == 172 && c >= 16 && c <= 31)
          || (a == 192 && (c == 0 || c == 168))
          || (a == 198 && (c == 18 || c == 19 || c == 51))
          || (a == 203 && c == 0 && (b[2] & 255) == 113));
    }
    // Only native global-unicast IPv6; exclude documentation and transition/tunnel ranges.
    return b.length == 16
        && (a & 0xe0) == 0x20
        && !(a == 0x20 && c == 0x02)
        && !(a == 0x20
            && c == 0x01
            && ((b[2] == 0 && (b[3] & 255) < 0x20)
                || ((b[2] & 255) == 0x0d && (b[3] & 255) == 0xb8)));
  }
}
