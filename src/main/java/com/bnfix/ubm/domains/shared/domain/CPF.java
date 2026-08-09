package com.bnfix.ubm.domains.shared.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class CPF {
    private String value;

    protected CPF() {}

    protected CPF(String value) {
        this.value = value;
    }

    public static CPF of(String value) {
        if (value == null || value.isEmpty()) throw new IllegalArgumentException("CPF cant be null");
        validate(value);
        return new CPF(value);
    }

    private static void validate(String value) {
        if (!value.matches("\\d{11}")) throw new IllegalArgumentException("CPF is invalid");
        if (value.matches("(\\d)\\1{10}")) throw new IllegalArgumentException("CPF numbers cannot be repeated");
        int sum = 0;
        for (int i = 0, weight = 10; i < 9; i++) sum += Character.getNumericValue(value.charAt(i)) * weight--;
        int r = 11 - sum % 11, digit = r == 10 || r == 11 ? 0 : r;
        if (digit != Character.getNumericValue(value.charAt(9))) throw new IllegalArgumentException("CPF is invalid");
        sum = 0;
        for (int i = 0, weight = 11; i < 10; i++) sum += Character.getNumericValue(value.charAt(i)) * weight--;
        r = 11 - sum % 11;
        digit = r == 10 || r == 11 ? 0 : r;
        if (digit != Character.getNumericValue(value.charAt(10))) throw new IllegalArgumentException("CPF is invalid");
    }

    public String getValue() {
        return value;
    }

    private void setValue(String value) {
        this.value = value;
    }
}
