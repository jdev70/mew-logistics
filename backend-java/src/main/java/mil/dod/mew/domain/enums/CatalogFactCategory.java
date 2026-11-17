package mil.dod.mew.domain.enums;

/** Distinguishes directly sourced catalog facts from analyst conclusions and demo-only data. */
public enum CatalogFactCategory {
    PUBLICLY_ATTRIBUTED_FACT,
    ANALYST_CURATED_INFERENCE,
    SYNTHETIC_OPERATIONAL_RECORD
}
