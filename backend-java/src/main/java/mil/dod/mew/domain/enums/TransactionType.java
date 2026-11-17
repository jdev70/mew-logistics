package mil.dod.mew.domain.enums;

public enum TransactionType {
    RCT("Receipt (+)"),
    DED("Deduction (-)"),
    ADJ("Adjustment (+/-)");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
