package dev.thomcgn.findly.product;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "product_match")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
public class ProductMatch {
  @jakarta.persistence.Id
  @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
  private java.util.UUID id;

  @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY, optional = false)
  @jakarta.persistence.JoinColumn(name = "analysis_id", nullable = false)
  private dev.thomcgn.findly.analysis.Analysis analysis;

  @jakarta.persistence.OneToOne(cascade = jakarta.persistence.CascadeType.ALL, optional = false)
  @jakarta.persistence.JoinColumn(name = "product_id", nullable = false, unique = true)
  private Product product;

  @jakarta.persistence.Column(nullable = false)
  private boolean selected;

  @jakarta.persistence.Column(nullable = false)
  private int position;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @jakarta.persistence.Column(nullable = false, columnDefinition = "jsonb")
  private dev.thomcgn.findly.matching.ScoredCandidate score;
}
