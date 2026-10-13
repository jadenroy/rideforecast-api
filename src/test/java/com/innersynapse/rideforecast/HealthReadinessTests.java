package com.innersynapse.rideforecast;

import com.innersynapse.rideforecast.controller.HealthController;
import org.junit.jupiter.api.Test;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class HealthReadinessTests {
    @Test void emptyQuoteTableIsReadyAndResourcesAreClosed() throws Exception {
        var dataSource = mock(DataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        var result = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1 FROM quote_observations LIMIT 1")).thenReturn(result);
        var response = new HealthController(dataSource).health();
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).containsEntry("database", "ready");
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        verify(statement).setQueryTimeout(2);
        verify(result).close();
        verify(statement).close();
        verify(connection).close();
    }

    @Test void unavailableDatabaseDoesNotReturnHealthyOrExposeConnectionDetails() throws Exception {
        var dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("private connection detail"));
        var response = new HealthController(dataSource).health();
        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).containsEntry("status", "unavailable");
        assertThat(response.getBody().toString()).doesNotContain("private");
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("5");
    }

    @Test void missingQuoteTableIsNotReadyEvenWhenDatabaseConnectionWorks() throws Exception {
        var dataSource = mock(DataSource.class);
        var connection = mock(Connection.class);
        var statement = mock(Statement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("table missing"));
        assertThat(new HealthController(dataSource).health().getStatusCode().value()).isEqualTo(503);
        verify(connection).close();
        verify(statement).close();
    }
}
