package de.foudhaili.ledger.api;

import de.foudhaili.ledger.service.LedgerService;
import de.foudhaili.ledger.service.TransferCommand;
import de.foudhaili.ledger.service.TransferResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final LedgerService ledger;

    public TransferController(LedgerService ledger) {
        this.ledger = ledger;
    }

    /**
     * Der Idempotenzschluessel kommt als Header, nicht im Body - so wie es
     * Zahlungsdienstleister ueblicherweise handhaben. Ein Retry mit demselben
     * Schluessel gibt 200 statt 201 zurueck und bucht nicht erneut.
     */
    @PostMapping
    public ResponseEntity<TransferResult> transfer(
            @RequestHeader("Idempotency-Key") @NotBlank String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        TransferResult result = ledger.transfer(new TransferCommand(
                idempotencyKey, request.sourceIban(), request.targetIban(), request.amountMinor()));

        return ResponseEntity
                .status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(result);
    }

    @GetMapping("/reconciliation")
    public LedgerService.Reconciliation reconciliation() {
        return ledger.reconcile();
    }
}
