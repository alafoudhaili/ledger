package de.foudhaili.ledger.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Ein Ueberweisungsauftrag. Der Idempotenzschluessel ist eindeutig indiziert -
 * die Datenbank, nicht der Anwendungscode, verhindert die Doppelbuchung.
 */
@Entity
@Table(name = "transfers",
       uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "source_iban", nullable = false, length = 34)
    private String sourceIban;

    @Column(name = "target_iban", nullable = false, length = 34)
    private String targetIban;

    @Column(name = "amount_minor", nullable = false)
    private long amountMinor;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Transfer() { }

    public Transfer(String idempotencyKey, String sourceIban, String targetIban, long amountMinor) {
        this.id = UUID.randomUUID();
        this.idempotencyKey = idempotencyKey;
        this.sourceIban = sourceIban;
        this.targetIban = targetIban;
        this.amountMinor = amountMinor;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getSourceIban() { return sourceIban; }
    public String getTargetIban() { return targetIban; }
    public long getAmountMinor() { return amountMinor; }
    public Instant getCreatedAt() { return createdAt; }
}
