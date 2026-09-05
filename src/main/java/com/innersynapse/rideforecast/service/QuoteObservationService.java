package com.innersynapse.rideforecast.service;

import com.innersynapse.rideforecast.dto.MarketBenchmarkResponse;
import com.innersynapse.rideforecast.dto.QuoteObservationRequest;
import com.innersynapse.rideforecast.dto.QuoteObservationResponse;
import com.innersynapse.rideforecast.model.QuoteObservation;
import com.innersynapse.rideforecast.repository.QuoteObservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class QuoteObservationService {

    private final QuoteObservationRepository repository;

    public QuoteObservationService(QuoteObservationRepository repository) {
        this.repository = repository;
    }

    public QuoteObservationResponse save(QuoteObservationRequest request) {
        String marketKey = marketKey(request.marketCity(), request.marketRegion(), request.marketCountry());
        Instant observedAt = request.observedAt() == null ? Instant.now() : request.observedAt();
        String source = request.source() == null || request.source().isBlank()
                ? "user-observed"
                : request.source().trim();

        QuoteObservation observation = new QuoteObservation(
                UUID.randomUUID(),
                normalizeDisplay(request.provider()),
                normalizeDisplay(request.rideType()),
                money(request.quotedPrice()),
                request.currency().trim().toUpperCase(Locale.ROOT),
                request.originLatitude(),
                request.originLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                normalizeDisplay(request.marketCity()),
                normalizeDisplay(request.marketRegion()),
                normalizeDisplay(request.marketCountry()),
                marketKey,
                normalizeNullable(request.originZone()),
                normalizeNullable(request.destinationZone()),
                request.roadDistanceMiles(),
                request.travelMinutes(),
                observedAt,
                Instant.now(),
                source
        );

        return toResponse(repository.save(observation));
    }

    public List<QuoteObservationResponse> recent(
            String marketCity,
            String marketRegion,
            String marketCountry
    ) {
        String marketKey = marketKey(marketCity, marketRegion, marketCountry);
        return repository.findTop100ByMarketKeyOrderByObservedAtDesc(marketKey)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MarketBenchmarkResponse benchmark(
            String marketCity,
            String marketRegion,
            String marketCountry,
            String provider,
            String rideType,
            int days
    ) {
        int boundedDays = Math.max(1, Math.min(days, 365));
        String marketKey = marketKey(marketCity, marketRegion, marketCountry);
        Instant cutoff = Instant.now().minus(boundedDays, ChronoUnit.DAYS);

        List<QuoteObservation> observations = repository
                .findByMarketKeyAndProviderIgnoreCaseAndRideTypeIgnoreCaseOrderByObservedAtDesc(
                        marketKey,
                        provider,
                        rideType
                )
                .stream()
                .filter(item -> !item.getObservedAt().isBefore(cutoff))
                .toList();

        if (observations.isEmpty()) {
            return new MarketBenchmarkResponse(
                    normalizeDisplay(marketCity),
                    normalizeDisplay(marketRegion),
                    normalizeDisplay(marketCountry),
                    marketKey,
                    normalizeDisplay(provider),
                    normalizeDisplay(rideType),
                    boundedDays,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "NO_DATA"
            );
        }

        List<Double> prices = new ArrayList<>(observations.stream().map(QuoteObservation::getQuotedPrice).toList());
        prices.sort(Comparator.naturalOrder());

        double average = prices.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double minimum = prices.getFirst();
        double maximum = prices.getLast();
        double median = percentile(prices, 0.50);
        double q1 = percentile(prices, 0.25);
        double q3 = percentile(prices, 0.75);

        return new MarketBenchmarkResponse(
                observations.getFirst().getMarketCity(),
                observations.getFirst().getMarketRegion(),
                observations.getFirst().getMarketCountry(),
                marketKey,
                observations.getFirst().getProvider(),
                observations.getFirst().getRideType(),
                boundedDays,
                observations.size(),
                money(minimum),
                money(q1),
                money(average),
                money(median),
                money(q3),
                money(maximum),
                confidence(observations.size())
        );
    }

    private QuoteObservationResponse toResponse(QuoteObservation item) {
        return new QuoteObservationResponse(
                item.getId(),
                item.getProvider(),
                item.getRideType(),
                item.getQuotedPrice(),
                item.getCurrency(),
                item.getMarketCity(),
                item.getMarketRegion(),
                item.getMarketCountry(),
                item.getMarketKey(),
                item.getOriginZone(),
                item.getDestinationZone(),
                item.getRoadDistanceMiles(),
                item.getTravelMinutes(),
                item.getObservedAt(),
                item.getSource()
        );
    }

    private String marketKey(String city, String region, String country) {
        return "%s|%s|%s".formatted(
                normalizeKey(country),
                normalizeKey(region),
                normalizeKey(city)
        );
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }

    private String normalizeDisplay(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return normalizeDisplay(value);
    }

    private double percentile(List<Double> sorted, double percentile) {
        if (sorted.size() == 1) {
            return sorted.getFirst();
        }
        double index = percentile * (sorted.size() - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = index - lower;
        return sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight;
    }

    private String confidence(long count) {
        if (count >= 50) return "HIGH";
        if (count >= 15) return "MEDIUM";
        if (count >= 5) return "LOW";
        return "VERY_LOW";
    }

    private double money(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
