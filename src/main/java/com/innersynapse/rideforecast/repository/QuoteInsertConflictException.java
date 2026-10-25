package com.innersynapse.rideforecast.repository;

import org.springframework.dao.DataIntegrityViolationException;

/** A value-free unique violation; only this failure is eligible for replay lookup. */
public class QuoteInsertConflictException extends DataIntegrityViolationException {
    public QuoteInsertConflictException() {
        super("Quote insert unique conflict (SQLSTATE 23505)");
    }
}
