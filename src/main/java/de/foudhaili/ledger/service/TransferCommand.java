package de.foudhaili.ledger.service;

public record TransferCommand(
        String idempotencyKey,
        String sourceIban,
        String targetIban,
        long amountMinor) { }
