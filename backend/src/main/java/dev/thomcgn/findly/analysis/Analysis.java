package dev.thomcgn.findly.analysis;

import dev.thomcgn.findly.listing.Listing;
import dev.thomcgn.findly.price.PriceSource;
import dev.thomcgn.findly.product.IdentifiedProduct;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private AnalysisStatus status = AnalysisStatus.PENDING;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private Instant updatedAt;

  private Instant completedAt;
}
