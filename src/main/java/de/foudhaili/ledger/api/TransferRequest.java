package de.foudhaili.ledger.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransferRequest(
        @NotBlank String sourceIban,
        @NotBlank String targetIban,
        @Positive long amountMinor) { }
