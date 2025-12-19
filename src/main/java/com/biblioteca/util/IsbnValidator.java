package com.biblioteca.util;

/**
 * Validador de ISBN (International Standard Book Number)
 * Suporta tanto ISBN-10 quanto ISBN-13
 */
public class IsbnValidator {

    /**
     * Valida se o ISBN fornecido é válido (ISBN-10 ou ISBN-13)
     */
    public static boolean isValidIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }

        // Remove espaços, hífens e outros caracteres não numéricos (exceto X para ISBN-10)
        String cleanIsbn = isbn.replaceAll("[\\s\\-]", "").toUpperCase();

        // Verifica se é ISBN-10 ou ISBN-13
        if (cleanIsbn.length() == 10) {
            return isValidIsbn10(cleanIsbn);
        } else if (cleanIsbn.length() == 13) {
            return isValidIsbn13(cleanIsbn);
        }

        return false;
    }

    /**
     * Valida ISBN-10
     */
    private static boolean isValidIsbn10(String isbn) {
        if (isbn.length() != 10) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            char digit = isbn.charAt(i);
            if (!Character.isDigit(digit)) {
                return false;
            }
            sum += Character.getNumericValue(digit) * (10 - i);
        }

        // O último dígito pode ser um número (0-9) ou X (que representa 10)
        char lastDigit = isbn.charAt(9);
        int checkDigit;
        if (lastDigit == 'X') {
            checkDigit = 10;
        } else if (Character.isDigit(lastDigit)) {
            checkDigit = Character.getNumericValue(lastDigit);
        } else {
            return false;
        }

        sum += checkDigit;
        return sum % 11 == 0;
    }

    /**
     * Valida ISBN-13
     */
    private static boolean isValidIsbn13(String isbn) {
        if (isbn.length() != 13) {
            return false;
        }

        // ISBN-13 deve começar com 978 ou 979
        if (!isbn.startsWith("978") && !isbn.startsWith("979")) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 13; i++) {
            char digit = isbn.charAt(i);
            if (!Character.isDigit(digit)) {
                return false;
            }
            int value = Character.getNumericValue(digit);
            if (i % 2 == 0) {
                sum += value; // Posições ímpares (0, 2, 4, ...)
            } else {
                sum += value * 3; // Posições pares (1, 3, 5, ...)
            }
        }

        return sum % 10 == 0;
    }

    /**
     * Formata o ISBN para exibição (adiciona hífens)
     */
    public static String formatIsbn(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return isbn;
        }

        String cleanIsbn = isbn.replaceAll("[\\s\\-]", "").toUpperCase();

        if (cleanIsbn.length() == 10) {
            // Format: 0-306-40615-2
            return String.format("%s-%s-%s-%s",
                    cleanIsbn.substring(0, 1),
                    cleanIsbn.substring(1, 4),
                    cleanIsbn.substring(4, 9),
                    cleanIsbn.substring(9, 10));
        } else if (cleanIsbn.length() == 13) {
            // Format: 978-0-306-40615-7
            return String.format("%s-%s-%s-%s-%s",
                    cleanIsbn.substring(0, 3),
                    cleanIsbn.substring(3, 4),
                    cleanIsbn.substring(4, 7),
                    cleanIsbn.substring(7, 12),
                    cleanIsbn.substring(12, 13));
        }

        return isbn; // Retorna original se não conseguir formatar
    }

    /**
     * Remove formatação do ISBN (remove hífens e espaços)
     */
    public static String cleanIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        return isbn.replaceAll("[\\s\\-]", "").toUpperCase();
    }
} 