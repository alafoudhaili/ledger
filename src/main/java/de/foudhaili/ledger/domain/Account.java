package de.foudhaili.ledger.domain;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Konto mit Saldo in kleinster Waehrungseinheit (Cent).
 * Der Saldo ist eine abgeleitete Groesse - die Wahrheit liegt in den Buchungen.
 * @Version aktiviert Optimistic Locking: zwei parallele Ueberweisungen auf
 * dasselbe Konto koennen sich nicht gegenseitig ueberschreiben.
 */
@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 34)
    private String iban;

    @Column(name = "balance_minor", nullable = false)
    private long balanceMinor;

    @Version
    private long version;

    protected Account() { }

    public Account(String iban, long initialBalanceMinor) {
        this.id = UUID.randomUUID();
        this.iban = iban;
        this.balanceMinor = initialBalanceMinor;
    }

    public void debit(long amountMinor) {
        if (amountMinor <= 0) throw new IllegalArgumentException("Betrag muss positiv sein");
        if (balanceMinor < amountMinor) throw new InsufficientFundsException(iban);
        this.balanceMinor -= amountMinor;
    }

    public void credit(long amountMinor) {
        if (amountMinor <= 0) throw new IllegalArgumentException("Betrag muss positiv sein");
        this.balanceMinor += amountMinor;
    }

    public UUID getId() { return id; }
    public String getIban() { return iban; }
    public long getBalanceMinor() { return balanceMinor; }
    public long getVersion() { return version; }
}
