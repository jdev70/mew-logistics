package java.data_transfer_objects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record AssetReadinessUpdateRequest(
                @NotBlank @Pattern(regexp = "FMC|PMC-M|PMC-S|NMCM", message = "Status must be FMC, PMC-M, PMC-S, or NMCM") String readinessStatus,

                @NotBlank String gcssEroWorkOrder,

                @NotNull Boolean cryptoKeyLoaded) {
}
