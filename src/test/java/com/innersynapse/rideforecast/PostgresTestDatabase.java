package com.innersynapse.rideforecast;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

final class PostgresTestDatabase {
    private PostgresTestDatabase() {}

    static void assertRequiredDatabase(DataSource dataSource) throws SQLException {
        if (!Boolean.getBoolean("rf027.postgres.required")) return;
        try (var connection = dataSource.getConnection()) {
            var metadata = connection.getMetaData();
            assertThat(metadata.getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(metadata.getDatabaseMajorVersion()).isEqualTo(18);
            assertThat(metadata.getURL()).isEqualTo("jdbc:postgresql://127.0.0.1:55432/rf027_ci");
            try (var statement = connection.createStatement();
                 var result = statement.executeQuery("select current_database()")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getString(1)).isEqualTo("rf027_ci");
            }
            System.out.println("RF027 database verified: " + metadata.getDatabaseProductName()
                    + " " + metadata.getDatabaseProductVersion() + "; driver " + metadata.getDriverVersion());
        }
    }
}
