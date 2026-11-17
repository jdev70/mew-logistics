package mil.dod.mew.web.dto;

import javax.validation.constraints.NotBlank;

/** Fleet QA disposition for a technician-submitted maintenance action. */
public class QaReviewRequest {
    @NotBlank(message = "decision is required.")
    private String decision;

    @NotBlank(message = "rationale is required.")
    private String rationale;

    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }
    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
