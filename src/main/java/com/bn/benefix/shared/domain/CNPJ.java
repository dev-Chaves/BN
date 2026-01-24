package com.bn.benefix.shared.domain;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.regex.Pattern;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter(AccessLevel.PROTECTED)
@EqualsAndHashCode
public class CNPJ {

    private String value;

    private static final Pattern FORMAT_PATTERN = Pattern.compile("\\d{14}");
    private static final Pattern REPEATED_DIGITS_PATTERN = Pattern.compile("(\\d)\\1{13}");

    public static CNPJ of(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("CNPJ cant be null");
        validate(value);
        return new CNPJ(value);
    }

    private static void validate(String value) {
        if (value == null || !FORMAT_PATTERN.matcher(value).matches()) throw new IllegalArgumentException("CNPJ must contain exactly 14 digits");

        if (REPEATED_DIGITS_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("CNPJ numbers cannot be repeated");
        }

        int[] weight1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum1 = 0;
        for (int i = 0; i < 12; i++) {
            sum1 += Character.getNumericValue(value.charAt(i)) * weight1[i];
        }
        int r1 = sum1 % 11;
        int digit1 = (r1 < 2) ? 0 : 11 - r1;

        if (digit1 != Character.getNumericValue(value.charAt(12))) {
            throw new IllegalArgumentException("CNPJ is invalid");
        }

        int[] weight2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum2 = 0;
        for (int i = 0; i < 13; i++) {
            sum2 += Character.getNumericValue(value.charAt(i)) * weight2[i];
        }
        int r2 = sum2 % 11;
        int digit2 = (r2 < 2) ? 0 : 11 - r2;

        if (digit2 != Character.getNumericValue(value.charAt(13))) {
            throw new IllegalArgumentException("CNPJ is invalid");
        }
    }
}

