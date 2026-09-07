package com.innersynapse.rideforecast.dto;

public record QuoteSaveResult(
        QuoteObservationResponse observation,
        boolean replayed
) {
}
