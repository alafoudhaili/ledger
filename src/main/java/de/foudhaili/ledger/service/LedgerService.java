package de.foudhaili.ledger.service;

import de.foudhaili.ledger.domain.*;
import de.foudhaili.ledger.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class LedgerService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository entries;

    public LedgerService(AccountRepository accounts, TransferRepository transfers,
                         LedgerEntryRepository entries) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.entries = entries;
    }

    /**
     * Bucht eine Ueberweisung nach doppelter Buchfuehrung.
     *
     * Ablauf:
     *  1. Idempotenz  - bekannter Schluessel gibt das alte Ergebnis zurueck, ohne zu buchen
     *  2. Validierung - IBAN-Pruefsumme, positiver Betrag, keine Selbstueberweisung
     *  3. Buchung     - genau zwei Zeilen, Soll und Haben, in derselben Transaktion
     *
     * Die Salden werden ueber @Version optimistisch gesperrt. Bei einem parallelen
     * Zugriff wirft JPA eine OptimisticLockingFailureException und die Transaktion
     * wird zurueckgerollt - eine der beiden Buchungen gewinnt, keine geht halb durch.
     */
    @Transactional
    public TransferResult transfer(TransferCommand cmd) {
        var existing = transfers.findByIdempotencyKey(cmd.idempotencyKey());
        if (existing.isPresent()) {
            return new TransferResult(existing.get().getId(), true);
        }

        String source = Iban.normalize(cmd.sourceIban());
        String target = Iban.normalize(cmd.targetIban());

        if (!Iban.isValid(source)) throw new IllegalArgumentException("Ungueltige Quell-IBAN");
        if (!Iban.isValid(target)) throw new IllegalArgumentException("Ungueltige Ziel-IBAN");
        if (source.equals(target)) throw new IllegalArgumentException("Quell- und Zielkonto sind identisch");
        if (cmd.amountMinor() <= 0) throw new IllegalArgumentException("Betrag muss positiv sein");

        Account from = accounts.findByIban(source)
                .orElseThrow(() -> new NoSuchElementException("Konto nicht gefunden: " + source));
        Account to = accounts.findByIban(target)
                .orElseThrow(() -> new NoSuchElementException("Konto nicht gefunden: " + target));

        from.debit(cmd.amountMinor());
        to.credit(cmd.amountMinor());

        Transfer transfer = transfers.save(
                new Transfer(cmd.idempotencyKey(), source, target, cmd.amountMinor()));

        entries.save(new LedgerEntry(transfer.getId(), from.getId(), EntryDirection.DEBIT, cmd.amountMinor()));
        entries.save(new LedgerEntry(transfer.getId(), to.getId(), EntryDirection.CREDIT, cmd.amountMinor()));

        accounts.save(from);
        accounts.save(to);

        return new TransferResult(transfer.getId(), false);
    }

    /**
     * Abstimmung: in einem korrekten Hauptbuch ist die Summe aller Soll-Buchungen
     * gleich der Summe aller Haben-Buchungen. Weicht das ab, ist eine Buchung
     * halb durchgelaufen - genau das soll dieser Endpunkt sichtbar machen.
     */
    @Transactional(readOnly = true)
    public Reconciliation reconcile() {
        long debits = entries.sumDebits();
        long credits = entries.sumCredits();
        return new Reconciliation(debits, credits, debits == credits);
    }

    public record Reconciliation(long totalDebitsMinor, long totalCreditsMinor, boolean balanced) { }
}
