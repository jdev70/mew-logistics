package mil.dod.mew.security.model;
public record MewResource(String resourceId, String installationCode, MilitaryBranch ownerBranch, ClearanceLevel requiredClearance, String cageCode) { }
