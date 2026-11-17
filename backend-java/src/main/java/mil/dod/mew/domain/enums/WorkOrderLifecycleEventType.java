package mil.dod.mew.domain.enums;

/** Named transitions in the maintenance lifecycle ledger. */
public enum WorkOrderLifecycleEventType {
    TECHNICIAN_SUBMITTED_FOR_QA,
    QA_APPROVED_FOR_RETURN_TO_SERVICE,
    RETURNED_TO_SERVICE,
    REWORK_REQUESTED,
    WORK_ORDER_CLOSED
}
