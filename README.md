# Ledger — Zahlungsplattform mit doppelter Buchführung

[![CI](https://github.com/alafoudhaili/ledger/actions/workflows/ci.yml/badge.svg)](https://github.com/alafoudhaili/ledger/actions)

Ein Zahlungs- und Hauptbuchdienst mit Spring Boot 3, Java 17 und PostgreSQL. Jede Überweisung erzeugt genau zwei Buchungszeilen — Soll und Haben — in einem Hauptbuch, das ausschließlich angehängt und niemals verändert wird.

*A payment and ledger service in Spring Boot 3, Java 17 and PostgreSQL. Every transfer writes exactly two entries — debit and credit — into an append-only ledger.*

---

## Schnellstart

```bash
git clone https://github.com/alafoudhaili/ledger.git
cd ledger
docker compose up
```

Die API liegt anschließend auf `http://localhost:8080`.

```bash
# Überweisung — der Idempotenzschlüssel kommt als Header
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Idempotency-Key: 7f3c1e90-abc1-4d2e-9f11-2a4c8b6e5d10" \
  -H "Content-Type: application/json" \
  -d '{"sourceIban":"DE89370400440532013000","targetIban":"GB29NWBK60161331926819","amountMinor":2500}'

# Derselbe Aufruf ein zweites Mal → 200 statt 201, keine zweite Buchung

# Abstimmung: Summe Soll gegen Summe Haben
curl http://localhost:8080/api/v1/transfers/reconciliation
```

## Was dieses Projekt zeigt

**Doppelte Buchführung.** Der Kontosaldo ist eine abgeleitete Größe; die Wahrheit liegt in den Buchungszeilen. `LedgerEntry` hat bewusst keine Setter und keinen Update-Pfad — eine Korrektur ist eine Gegenbuchung, keine Änderung. Der Abstimmungs-Endpunkt beweist jederzeit, dass Soll und Haben übereinstimmen.

**Idempotenz.** Der Idempotenzschlüssel liegt auf einem eindeutigen Index in der Datenbank, nicht in einer Prüfung im Anwendungscode. Damit verhindert die Datenbank die Doppelbuchung auch dann, wenn zwei Instanzen des Dienstes denselben Retry gleichzeitig verarbeiten. Ein wiederholter Aufruf liefert dieselbe Transfer-ID mit `replayed: true`.

**Optimistic Locking.** Konten tragen ein `@Version`-Feld. Zwei gleichzeitige Überweisungen vom selben Konto können sich nicht gegenseitig überschreiben — eine gewinnt, die andere erhält HTTP 409 und darf gefahrlos wiederholt werden. Der Test `concurrentTransfersKeepLedgerBalanced` startet zehn Threads gegen dasselbe Konto und prüft, dass am Ende kein Cent verloren gegangen ist.

**IBAN-Prüfung nach ISO 13616.** Das Modulo-97-Verfahren ist bewusst ohne Fremdbibliothek implementiert, damit der Algorithmus im Code sichtbar bleibt. Getestet tabellengetrieben gegen bekannte Gut- und Schlechtfälle.

**Beträge als `long` in Cent.** Niemals `double` für Geld. Die kleinste Währungseinheit vermeidet Rundungsfehler vollständig.

## Architektur

```
  HTTP
    │
    ▼
  TransferController ──── Idempotency-Key als Header, ProblemDetail als Fehlerformat
    │
    ▼
  LedgerService ───────── @Transactional: Validierung, Buchung, Saldenfortschreibung
    │
    ├── AccountRepository        Konten mit @Version (Optimistic Locking)
    ├── TransferRepository       eindeutiger Index auf idempotency_key
    └── LedgerEntryRepository    append-only Buchungszeilen
    │
    ▼
  PostgreSQL
```

## Tests

```bash
mvn verify
```

Die Integrationstests starten PostgreSQL über Testcontainers — es ist keine lokal installierte Datenbank nötig, nur ein laufender Docker-Daemon. Der Abdeckungsbericht liegt nach dem Lauf unter `target/site/jacoco/index.html`.

| Test | Prüft |
|---|---|
| `IbanTest` | Modulo-97-Prüfsumme, Gut- und Schlechtfälle |
| `writesTwoEntriesPerTransfer` | genau zwei Zeilen, Salden korrekt, Hauptbuch ausgeglichen |
| `replaysInsteadOfDoubleBooking` | derselbe Schlüssel bucht nicht erneut |
| `rejectsOverdraft` | keine Buchung ohne Deckung, keine Teilbuchung |
| `concurrentTransfersKeepLedgerBalanced` | zehn parallele Überweisungen, kein Saldoverlust |

## Nächste Schritte

- [ ] Kafka: `TransferInitiated` publizieren, Buchung asynchron im Ledger-Service konsumieren
- [ ] Kubernetes-Manifeste inklusive Ingress und Secrets, lokal auf k3s
- [ ] Angular-Dashboard: Konten, Umsätze, Abstimmungsansicht
- [ ] SEPA-Export nach `pain.001.001.09`
- [ ] Architekturentscheidungen als ADRs unter `docs/adr/`

## Lizenz

MIT
