package com.bnfix.ubm.domains.shared.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class CNPJ {
    private String value;
    protected CNPJ() {}
    protected CNPJ(String value) { this.value = value; }
    public static CNPJ of(String value) { if (value == null || value.isEmpty()) throw new IllegalArgumentException("CNPJ cant be null"); validate(value); return new CNPJ(value); }
    private static void validate(String value) {
        if (!value.matches("\\d{14}")) throw new IllegalArgumentException("CNPJ must contain exactly 14 digits");
        if (value.matches("(\\d)\\1{13}")) throw new IllegalArgumentException("CNPJ numbers cannot be repeated");
        int[] a = {5,4,3,2,9,8,7,6,5,4,3,2}; int sum = 0;
        for (int i = 0; i < 12; i++) sum += Character.getNumericValue(value.charAt(i)) * a[i];
        int digit = sum % 11 < 2 ? 0 : 11 - sum % 11;
        if (digit != Character.getNumericValue(value.charAt(12))) throw new IllegalArgumentException("CNPJ is invalid");
        int[] b = {6,5,4,3,2,9,8,7,6,5,4,3,2}; sum = 0;
        for (int i = 0; i < 13; i++) sum += Character.getNumericValue(value.charAt(i)) * b[i];
        digit = sum % 11 < 2 ? 0 : 11 - sum % 11;
        if (digit != Character.getNumericValue(value.charAt(13))) throw new IllegalArgumentException("CNPJ is invalid");
    }
    public String getValue() { return value; }
    private void setValue(String value) { this.value = value; }
}
