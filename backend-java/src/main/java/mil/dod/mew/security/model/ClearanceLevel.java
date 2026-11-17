package mil.dod.mew.security.model;
public enum ClearanceLevel { UNCLASSIFIED(1), CUI(2), SECRET(3), TOP_SECRET(4), TS_SCI(5); private final int level; ClearanceLevel(int level) { this.level = level; } public boolean meetsOrExceeds(ClearanceLevel required) { return level >= required.level; } }
