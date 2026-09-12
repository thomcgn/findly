package dev.thomcgn.findly.listing;

import dev.thomcgn.findly.analysis.Analysis;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "listing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "analysis_id", nullable = false, unique = true)
  private Analysis analysis;

  @Column(name = "external_url", nullable = false, length = 2048)
  private String externalUrl;

  @Column(nullable = false)
  private String title;

  @Column(length = 10000)
  private String description;

  @Column(name = "listing_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal listingPrice;

  @Column(nullable = false, length = 10)
  @Builder.Default
  private String currency = "EUR";

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "listing_image_urls", joinColumns = @JoinColumn(name = "listing_id"))
  @Column(name = "image_url", length = 2048)
  @Builder.Default
  private List<String> imageUrls = new ArrayList<>();

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
}
