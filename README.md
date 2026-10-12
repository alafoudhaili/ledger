# Ledger — double-entry payment platform

[![CI](https://github.com/alafoudhaili/ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/alafoudhaili/ledger/actions)

A payment and ledger service built with Spring Boot 3, Java 17 and PostgreSQL. Every transfer writes exactly two entries — debit and credit — into an append-only ledger.

---

## Quickstart

```bash
git clone https://github.com/alafoudhaili/ledger.git
cd ledger
docker compose up
```

The API is then available at `http://localhost:8080`.

```bash
# Transfer — the idempotency key is passed as a header
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Idempotency-Key: 7f3c1e90-abc1-4d2e-9f11-2a4c8b6e5d10" \
  -H "Content-Type: application/json" \
  -d '{"sourceIban":"DE89370400440532013000","targetIban":"GB29NWBK60161331926819","amountMinor":2500}'

# Same call a second time → 200 instead of 201, no second booking

# Reconciliation: sum of debits against sum of credits
curl http://localhost:8080/api/v1/transfers/reconciliation
```

## What this project demonstrates

**Double-entry bookkeeping.** The account balance is a derived value; the truth lives in the ledger entries. `LedgerEntry` deliberately has no setters and no update path — a correction is a reversing entry, not a change. The reconciliation endpoint proves at any time that debits and credits match.

**Idempotency.** The idempotency key sits on a unique database index, not on an application-level check. That means the database prevents double-booking even when two instances of the service process the same retry at the same time. A repeated call returns the same transfer ID with `replayed: true`.

**Optimistic locking.** Accounts carry an `@Version` field. Two concurrent transfers from the same account can't overwrite each other — one wins, the other gets HTTP 409 and can safely retry. The `concurrentTransfersKeepLedgerBalanced` test fires ten threads against the same account and checks that not a single cent is lost at the end.

**IBAN validation per ISO 13616.** The modulo-97 algorithm is deliberately implemented without a third-party library, so the logic stays visible in the code. Tested table-driven against known valid and invalid cases.

**Amounts as `long` in cents.** Never `double` for money. The smallest currency unit avoids rounding errors entirely.

## Architecture

```
  HTTP
    │
    ▼
  TransferController ──── Idempotency-Key as a header, ProblemDetail as the error format
    │
    ▼
  LedgerService ───────── @Transactional: validation, booking, balance updates
    │
    ├── AccountRepository        accounts with @Version (optimistic locking)
    ├── TransferRepository       unique index on idempotency_key
    └── LedgerEntryRepository    append-only ledger entries
    │
    ▼
  PostgreSQL
```

## Tests

```bash
mvn verify
```

Integration tests start PostgreSQL via Testcontainers — no locally installed database is needed, just a running Docker daemon. The coverage report lands at `target/site/jacoco/index.html` after the run.

| Test | Verifies |
|---|---|
| `IbanTest` | modulo-97 checksum, valid and invalid cases |
| `writesTwoEntriesPerTransfer` | exactly two entries, correct balances, balanced ledger |
| `replaysInsteadOfDoubleBooking` | the same key doesn't book a second time |
| `rejectsOverdraft` | no booking without sufficient funds, no partial booking |
| `concurrentTransfersKeepLedgerBalanced` | ten parallel transfers, no balance loss |

## Next steps

- [ ] Kafka: publish `TransferInitiated`, consume the booking asynchronously in the ledger service
- [ ] Kubernetes manifests including ingress and secrets, running locally on k3s
- [ ] Angular dashboard: accounts, transactions, reconciliation view
- [ ] SEPA export to `pain.001.001.09`
- [ ] Architecture decisions recorded as ADRs under `docs/adr/`

## License

MIT
