package com.innersynapse.rideforecast.repository;

import com.innersynapse.rideforecast.model.QuoteObservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.SQLExceptionSubclassTranslator;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Timestamp;

/** A standalone insert: expected unique conflicts must not enter Hibernate's error logger. */
public class QuoteObservationInsertImpl implements QuoteObservationInsert {
    private static final Logger LOG = LoggerFactory.getLogger(QuoteObservationInsertImpl.class);
    private static final String SQL = """
            insert into quote_observations
            (id, provider, ride_type, quoted_price, currency, origin_latitude, origin_longitude,
             destination_latitude, destination_longitude, market_city, market_region, market_country,
             market_key, origin_zone, destination_zone, road_distance_miles, travel_minutes,
             observed_at, created_at, source, submission_key, submission_fingerprint, owner_id)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private final DataSource dataSource;

    public QuoteObservationInsertImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public QuoteObservation insert(QuoteObservation q) {
        // Own the connection so the failed statement is complete before the service's replay lookup.
        // Do not join a caller transaction that a PostgreSQL constraint error would leave aborted.
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(true);
            try (var statement = connection.prepareStatement(SQL)) {
                Object[] values = {q.getId(), q.getProvider(), q.getRideType(), q.getQuotedPrice(),
                        q.getCurrency(), q.getOriginLatitude(), q.getOriginLongitude(),
                        q.getDestinationLatitude(), q.getDestinationLongitude(), q.getMarketCity(),
                        q.getMarketRegion(), q.getMarketCountry(), q.getMarketKey(), q.getOriginZone(),
                        q.getDestinationZone(), q.getRoadDistanceMiles(), q.getTravelMinutes(),
                        Timestamp.from(q.getObservedAt()), Timestamp.from(q.getCreatedAt()), q.getSource(),
                        q.getSubmissionKey(), q.getSubmissionFingerprint(), q.getOwnerId()};
                for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
                statement.executeUpdate();
                return q;
            }
        } catch (SQLException error) {
            if ("23505".equals(error.getSQLState())) {
                // The service must still find the stored key and check owner AND fingerprint.
                // Do not retain the driver exception: its message/detail can contain the raw key.
                LOG.info("QUOTE_INSERT_UNIQUE_CONFLICT sqlState=23505");
                throw new QuoteInsertConflictException();
            }
            var translated = new SQLExceptionSubclassTranslator().translate("Insert quote observation", SQL, error);
            if (translated != null) throw translated;
            throw new org.springframework.jdbc.UncategorizedSQLException("Insert quote observation", SQL, error);
        }
    }
}
