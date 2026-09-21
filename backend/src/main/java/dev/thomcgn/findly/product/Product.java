package dev.thomcgn.findly.product;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "product")
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
public class Product {
  @jakarta.persistence.Id
  @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
  private java.util.UUID id;

  @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
  @jakarta.persistence.Column(nullable = false, columnDefinition = "jsonb")
  private dev.thomcgn.findly.provider.ProductCandidate candidate;
}
