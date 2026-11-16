package mil.navy.nswc.mew.security.model;
public record MewResource(String resourceId, String installationCode, MilitaryBranch ownerBranch, ClearanceLevel requiredClearance, String cageCode) { }
