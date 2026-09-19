package dev.thomcgn.findly;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class AnalysisMigrationTest {

  @Container
  private static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine");

  @Test
  void upgradesExistingAnalysesWithoutChangingTheirResults() throws SQLException {
    var configuration =
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    configuration.target("1").load().migrate();

    try (var connection =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        var statement = connection.createStatement()) {
      statement.executeUpdate(
          "INSERT INTO analysis (status) VALUES ('PENDING'), ('COMPLETED'), ('FAILED')");

      var flyway = configuration.target("latest").load();
      assertEquals(1, flyway.migrate().migrationsExecuted);
      flyway.validate();
      assertEquals(0, flyway.migrate().migrationsExecuted);

      try (var rows =
          statement.executeQuery(
              "SELECT status, version, progress, started_at, failed_at, error_code, error_message FROM analysis ORDER BY status")) {
        for (String status : new String[] {"COMPLETED", "FAILED", "PENDING"}) {
          assertTrue(rows.next());
          assertEquals(status, rows.getString("status"));
          assertEquals(0, rows.getLong("version"));
          assertEquals(status.equals("PENDING") ? 0 : 100, rows.getInt("progress"));
          assertNull(rows.getObject("started_at"));
          assertNull(rows.getObject("failed_at"));
          assertNull(rows.getString("error_code"));
          assertNull(rows.getString("error_message"));
        }
        assertFalse(rows.next());
      }
      assertThrows(
          SQLException.class, () -> statement.executeUpdate("UPDATE analysis SET progress = 101"));
      assertThrows(
          SQLException.class, () -> statement.executeUpdate("UPDATE analysis SET progress = -1"));
    }
  }
}
