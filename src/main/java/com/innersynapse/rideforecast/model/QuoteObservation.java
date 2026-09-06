package com.innersynapse.rideforecast.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quote_observations", indexes = {
        @Index(
                name = "idx_quote_market_provider_type_time",
                columnList = "market_key,provider,ride_type,observed_at"
        )
})
public class QuoteObservation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String provider;

    @Column(name = "ride_type", nullable = false)
    private String rideType;

    @Column(name = "quoted_price", nullable = false)
    private double quotedPrice;

    @Column(nullable = false)
    private String currency;

    @Column(name = "origin_latitude", nullable = false)
    private double originLatitude;

    @Column(name = "origin_longitude", nullable = false)
    private double originLongitude;

    @Column(name = "destination_latitude", nullable = false)
    private double destinationLatitude;

    @Column(name = "destination_longitude", nullable = false)
    private double destinationLongitude;

    @Column(name = "market_city", nullable = false)
    private String marketCity;

    @Column(name = "market_region", nullable = false)
    private String marketRegion;

    @Column(name = "market_country", nullable = false)
    private String marketCountry;

    @Column(name = "market_key", nullable = false)
    private String marketKey;

    @Column(name = "origin_zone")
    private String originZone;

    @Column(name = "destination_zone")
    private String destinationZone;

    @Column(name = "road_distance_miles", nullable = false)
    private double roadDistanceMiles;

    @Column(name = "travel_minutes", nullable = false)
    private double travelMinutes;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private String source;

    protected QuoteObservation() {
    }

    public QuoteObservation(
            UUID id,
            String provider,
            String rideType,
            double quotedPrice,
            String currency,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude,
            String marketCity,
            String marketRegion,
            String marketCountry,
            String marketKey,
            String originZone,
            String destinationZone,
            double roadDistanceMiles,
            double travelMinutes,
            Instant observedAt,
            Instant createdAt,
            String source
    ) {
        this.id = id;
        this.provider = provider;
        this.rideType = rideType;
        this.quotedPrice = quotedPrice;
        this.currency = currency;
        this.originLatitude = originLatitude;
        this.originLongitude = originLongitude;
        this.destinationLatitude = destinationLatitude;
        this.destinationLongitude = destinationLongitude;
        this.marketCity = marketCity;
        this.marketRegion = marketRegion;
        this.marketCountry = marketCountry;
        this.marketKey = marketKey;
        this.originZone = originZone;
        this.destinationZone = destinationZone;
        this.roadDistanceMiles = roadDistanceMiles;
        this.travelMinutes = travelMinutes;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
        this.source = source;
    }

    public UUID getId() { return id; }
    public String getProvider() { return provider; }
    public String getRideType() { return rideType; }
    public double getQuotedPrice() { return quotedPrice; }
    public String getCurrency() { return currency; }
    public double getOriginLatitude() { return originLatitude; }
    public double getOriginLongitude() { return originLongitude; }
    public double getDestinationLatitude() { return destinationLatitude; }
    public double getDestinationLongitude() { return destinationLongitude; }
    public String getMarketCity() { return marketCity; }
    public String getMarketRegion() { return marketRegion; }
    public String getMarketCountry() { return marketCountry; }
    public String getMarketKey() { return marketKey; }
    public String getOriginZone() { return originZone; }
    public String getDestinationZone() { return destinationZone; }
    public double getRoadDistanceMiles() { return roadDistanceMiles; }
    public double getTravelMinutes() { return travelMinutes; }
    public Instant getObservedAt() { return observedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public String getSource() { return source; }
}
