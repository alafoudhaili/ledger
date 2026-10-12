package de.foudhaili.ledger;

import de.foudhaili.ledger.domain.Account;
import de.foudhaili.ledger.repository.AccountRepository;
import de.foudhaili.ledger.repository.LedgerEntryRepository;
import de.foudhaili.ledger.service.LedgerService;
import de.foudhaili.ledger.service.TransferCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
@DisplayName("Hauptbuch - Buchung, Idempotenz und Nebenlaeufigkeit")
class LedgerServiceIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    private static final String ALICE = "DE89370400440532013000";
    private static final String BOB   = "GB29NWBK60161331926819";

    @Autowired LedgerService ledger;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository entries;

    @BeforeEach
    void setUp() {
        entries.deleteAll();
        accounts.deleteAll();
        accounts.save(new Account(ALICE, 100_00));
        accounts.save(new Account(BOB, 0));
    }

    @Test
    @DisplayName("eine Ueberweisung erzeugt genau zwei Buchungszeilen")
    void writesTwoEntriesPerTransfer() {
        var result = ledger.transfer(new TransferCommand(UUID.randomUUID().toString(), ALICE, BOB, 25_00));

        assertThat(result.replayed()).isFalse();
        assertThat(entries.findByTransferId(result.transferId())).hasSize(2);
        assertThat(accounts.findByIban(ALICE).orElseThrow().getBalanceMinor()).isEqualTo(75_00);
        assertThat(accounts.findByIban(BOB).orElseThrow().getBalanceMinor()).isEqualTo(25_00);
        assertThat(ledger.reconcile().balanced()).isTrue();
    }

    @Test
    @DisplayName("derselbe Idempotenzschluessel bucht kein zweites Mal")
    void replaysInsteadOfDoubleBooking() {
        String key = UUID.randomUUID().toString();

        var first  = ledger.transfer(new TransferCommand(key, ALICE, BOB, 40_00));
        var second = ledger.transfer(new TransferCommand(key, ALICE, BOB, 40_00));

        assertThat(second.replayed()).isTrue();
        assertThat(second.transferId()).isEqualTo(first.transferId());
        assertThat(accounts.findByIban(ALICE).orElseThrow().getBalanceMinor()).isEqualTo(60_00);
        assertThat(entries.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("Ueberweisung ohne Deckung wird abgelehnt")
    void rejectsOverdraft() {
        assertThatThrownBy(() -> ledger.transfer(
                new TransferCommand(UUID.randomUUID().toString(), ALICE, BOB, 500_00)))
                .hasMessageContaining("Unzureichende Deckung");

        assertThat(accounts.findByIban(ALICE).orElseThrow().getBalanceMinor()).isEqualTo(100_00);
        assertThat(entries.count()).isZero();
    }

    /**
     * Der interessante Test. Zehn Threads ueberweisen gleichzeitig je 20 Cent
     * von einem Konto mit 100 Euro Deckung. Optimistic Locking laesst einen Teil
     * scheitern - entscheidend ist: kein Saldo geht verloren und das Hauptbuch
     * bleibt am Ende ausgeglichen.
     */
    @Test
    @DisplayName("parallele Ueberweisungen verfaelschen den Saldo nicht")
    void concurrentTransfersKeepLedgerBalanced() throws Exception {
        int threads = 10;
        long amount = 20;
        var pool = Executors.newFixedThreadPool(threads);
        var startGate = new CountDownLatch(1);
        var succeeded = new AtomicInteger();

        List<Future<?>> futures = java.util.stream.IntStream.range(0, threads)
                .mapToObj(i -> pool.submit(() -> {
                    startGate.await();
                    try {
                        ledger.transfer(new TransferCommand(
                                UUID.randomUUID().toString(), ALICE, BOB, amount));
                        succeeded.incrementAndGet();
                    } catch (Exception expected) {
                        // Optimistic-Lock-Konflikt: der Aufrufer darf wiederholen
                    }
                    return null;
                }))
                .map(f -> (Future<?>) f)
                .toList();

        startGate.countDown();
        for (Future<?> f : futures) f.get(30, TimeUnit.SECONDS);
        pool.shutdown();

        long expectedMoved = succeeded.get() * amount;

        assertThat(succeeded.get()).isPositive();
        assertThat(accounts.findByIban(ALICE).orElseThrow().getBalanceMinor())
                .isEqualTo(100_00 - expectedMoved);
        assertThat(accounts.findByIban(BOB).orElseThrow().getBalanceMinor())
                .isEqualTo(expectedMoved);
        assertThat(entries.count()).isEqualTo(succeeded.get() * 2L);
        assertThat(ledger.reconcile().balanced()).isTrue();
    }
}
