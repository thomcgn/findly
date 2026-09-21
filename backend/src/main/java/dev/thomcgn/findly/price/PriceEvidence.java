package dev.thomcgn.findly.price;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "price_evidence")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
public class PriceEvidence {
  @jakarta.persistence.Id
  @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
  private java.util.UUID id;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
  @jakarta.persistence.JoinColumn(name = "analysis_id", nullable = false)
  private dev.thomcgn.findly.analysis.Analysis analysis;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @jakarta.persistence.Column(nullable = false, columnDefinition = "jsonb")
  private dev.thomcgn.findly.provider.PriceQuote quote;
}
