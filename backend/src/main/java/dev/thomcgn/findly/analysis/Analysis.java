package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.listing.Listing;
import dev.thomcgn.findly.price.PriceSource;
import dev.thomcgn.findly.product.IdentifiedProduct;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "analysis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Analysis {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(
      mappedBy = "analysis",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Listing listing;

  @OneToOne(
      mappedBy = "analysis",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private IdentifiedProduct identifiedProduct;

  @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<PriceSource> priceSources = new ArrayList<>();

  @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
  @jakarta.persistence.OrderBy("position ASC")
  @Builder.Default
  private List<dev.thomcgn.findly.product.ProductMatch> matches = new ArrayList<>();

  @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<dev.thomcgn.findly.product.ExtractedAttribute> attributes = new ArrayList<>();

  @OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<dev.thomcgn.findly.price.PriceEvidence> priceEvidence = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "analysis_warnings", joinColumns = @JoinColumn(name = "analysis_id"))
  @Column(name = "warning", nullable = false, length = 2048)
  @Builder.Default
  private List<String> warnings = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AnalysisStatus status = AnalysisStatus.CREATED;

  @Version
  @Column(nullable = false)
  private long version;

  @Builder.Default
  @Column(nullable = false)
  private int progress = 0;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  @Column(length = 2048)
  private String sourceUrl;

  private Instant deadlineAt;
  private Instant startedAt;
  private Instant completedAt;
  private Instant failedAt;

  @Column(length = 128)
  private String errorCode;

  @Column(length = 2048)
  private String errorMessage;
}
