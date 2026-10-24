package com.innersynapse.rideforecast.provider;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.innersynapse.rideforecast.dto.PriceRequest;

/** Only reviewed direct or licensed partner integrations may implement this contract. */
public interface ProviderAdapter {
    String providerId();
    List<Fare> fetch(Query query);

    record Query(ProviderPolicy.Coverage coverage, PriceRequest trip) {}
    enum Source { DIRECT_API, LICENSED_PARTNER_API }
    record Fare(String providerId, String product, String market, String currency,
                BigDecimal amount, Source source, String sourceReference,
                Instant fetchedAt, Instant expiresAt) {}
}
