package dev.thomcgn.findly.listing;

import java.net.InetAddress;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ListingClientProperties.class)
public class ListingHttpConfiguration {
  @Bean
  PublicAddressResolver publicAddressResolver() {
    return new PublicAddressResolver(InetAddress::getAllByName);
  }
}
