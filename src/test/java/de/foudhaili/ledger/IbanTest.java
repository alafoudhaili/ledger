package de.foudhaili.ledger;

import de.foudhaili.ledger.domain.Iban;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tabellengetriebene Pruefung des Modulo-97-Verfahrens - so testen Banken
 * IBAN-Validierung ueblicherweise auch: bekannte Gut- und Schlechtfaelle.
 */
class IbanTest {

    @ParameterizedTest
    @DisplayName("gueltige IBANs werden akzeptiert")
    @ValueSource(strings = {
            "DE89370400440532013000",
            "DE 89 3704 0044 0532 0130 00",
            "FR1420041010050500013M02606",
            "GB29NWBK60161331926819"
    })
    void acceptsValidIbans(String iban) {
        assertThat(Iban.isValid(iban)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("fehlerhafte Pruefsumme und Formatfehler werden abgelehnt")
    @ValueSource(strings = {
            "DE89370400440532013001",
            "DE00370400440532013000",
            "XX89370400440532013000000000000000000",
            "DE89",
            "370400440532013000"
    })
    void rejectsInvalidIbans(String iban) {
        assertThat(Iban.isValid(iban)).isFalse();
    }
}
