package dev.thomcgn.findly.price;

import dev.thomcgn.findly.analysis.Analysis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "price_source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceSource {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "analysis_id", nullable = false)
  private Analysis analysis;

  @Column(name = "source_name", nullable = false)
  private String sourceName;

  @Column(name = "product_title", nullable = false)
  private String productTitle;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal price;

  @Column(nullable = false, length = 10)
  private String currency;

  @Column(nullable = false, length = 2048)
  private String url;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  @Builder.Default
  private PriceSourceCondition condition = PriceSourceCondition.UNKNOWN;

  @CreationTimestamp
  @Column(nullable = false, updatable = false)
  private Instant createdAt;
}
