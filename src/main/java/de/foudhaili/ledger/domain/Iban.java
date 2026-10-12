package de.foudhaili.ledger.domain;

import java.math.BigInteger;

/**
 * IBAN-Pruefung nach ISO 13616 (Modulo-97-Verfahren).
 * Bewusst ohne Fremdbibliothek, damit der Algorithmus sichtbar bleibt.
 */
public final class Iban {

    private Iban() { }

    public static boolean isValid(String raw) {
        if (raw == null) return false;
        String iban = raw.replaceAll("\\s", "").toUpperCase();
        if (iban.length() < 15 || iban.length() > 34) return false;
        if (!iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]+")) return false;

        String rearranged = iban.substring(4) + iban.substring(0, 4);
        StringBuilder numeric = new StringBuilder();
        for (char c : rearranged.toCharArray()) {
            numeric.append(Character.isDigit(c) ? String.valueOf(c) : (c - 'A' + 10));
        }
        return new BigInteger(numeric.toString()).mod(BigInteger.valueOf(97)).intValue() == 1;
    }

    public static String normalize(String raw) {
        return raw.replaceAll("\\s", "").toUpperCase();
    }
}
