package de.foudhaili.ledger.repository;

import de.foudhaili.ledger.domain.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransferId(UUID transferId);

    List<LedgerEntry> findByAccountIdOrderByBookedAtDesc(UUID accountId);

    @Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e where e.direction = 'DEBIT'")
    long sumDebits();

    @Query("select coalesce(sum(e.amountMinor), 0) from LedgerEntry e where e.direction = 'CREDIT'")
    long sumCredits();
}
