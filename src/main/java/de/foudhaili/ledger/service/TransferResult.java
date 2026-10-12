package de.foudhaili.ledger.service;

import java.util.UUID;

/**
 * replayed = true bedeutet: dieser Idempotenzschluessel war bereits bekannt,
 * es wurde nichts erneut gebucht. Der Aufrufer bekommt dieselbe Transfer-ID.
 */
public record TransferResult(UUID transferId, boolean replayed) { }
