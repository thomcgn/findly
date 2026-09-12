package dev.thomcgn.findly.product;

import dev.thomcgn.findly.analysis.Analysis;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "identified_product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentifiedProduct {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "analysis_id", nullable = false, unique = true)
  private Analysis analysis;

  @Column(nullable = false)
  private String brand;

  @Column(nullable = false)
  private String name;

  @Column private String model;

  @Column(name = "model_number")
  private String modelNumber;

  @Column(nullable = false)
  private String category;

  @Column(nullable = false, precision = 5, scale = 2)
  private BigDecimal confidence;
}
