package mil.dod.mew.domain.enums;

public enum ReadinessLevel {
    FMC("Fully Mission Capable"),
    PMC("Partially Mission Capable"),
    NMCM("Non-Mission Capable (Maintenance)"),
    NMCS("Non-Mission Capable (Supply)"),
    DLM("Depot Level Maintenance");

    private final String label;

    ReadinessLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
