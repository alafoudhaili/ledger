package de.foudhaili.ledger.domain;

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String iban) {
        super("Unzureichende Deckung auf Konto " + iban);
    }
}
