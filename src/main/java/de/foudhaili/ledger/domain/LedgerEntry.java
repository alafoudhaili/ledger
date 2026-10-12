package de.foudhaili.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Eine Buchungszeile. Append-only: bewusst keine Setter und kein Update-Pfad.
 * Eine Korrektur ist eine Gegenbuchung, keine Aenderung.
 * Jede Ueberweisung erzeugt genau zwei Zeilen - Soll und Haben.
 */
@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    private UUID id;

    @Column(name = "transfer_id", nullable = false)
    private UUID transferId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private EntryDirection direction;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    protected LedgerEntry() { }

    public LedgerEntry(UUID transferId, UUID accountId, EntryDirection direction, long amountMinor) {
        this.id = UUID.randomUUID();
        this.transferId = transferId;
        this.accountId = accountId;
        this.direction = direction;
        this.amountMinor = amountMinor;
        this.bookedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTransferId() { return transferId; }
    public UUID getAccountId() { return accountId; }
    public EntryDirection getDirection() { return direction; }
    public long getAmountMinor() { return amountMinor; }
    public Instant getBookedAt() { return bookedAt; }
}
