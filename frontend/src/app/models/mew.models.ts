export interface PaginatedResponse<T> {
  count: number;
  next: string | null;
  previous: string | null;
  results: T[];
}

export type MewViewpoint =
  | 'analytics'
  | 'readiness'
  | 'loadouts'
  | 'supply'
  | 'installations'
  | 'ledger';

export type OperationalDomainFilter = 'ALL' | 'Surface' | 'Subsurface' | 'Airborne' | 'Ground';

export interface InstallationBase {
  id: string;
  installation_id: string | null;
  installation_code: string | null;
  name: string;
  dodaac: string;
  location_display: string;
  region: string;
  mission_category: string;
}

export interface InstallationDashboard extends InstallationBase {
  city: string;
  state_or_country: string;
  latitude: number | string | null;
  longitude: number | string | null;
  primary_ew_focus: string;
  dsn_prefix: string;
  max_ew_billets: number;
  scif_accredited: boolean;
  rf_anechoic_chamber: boolean;
  readiness_rating: string;
  last_inspection_date: string | null;
  catalog_source_id?: string;
  catalog_source_url?: string;
  catalog_verified_on?: string | null;
  public_association_type?: string;
  catalog_confidence?: string;
  synthetic_operational_data?: boolean;
  personnel_count: number;
  housed_assets_count: number;
}

export interface PersonnelRecord {
  id: string;
  personnel_id: string;
  dod_id: string;
  first_name: string;
  middle_initial: string;
  last_name: string;
  personnel_category: string;
  paygrade: string;
  rank: string;
  rank_full: string;
  specialty_code: string;
  title: string;
  role: string;
  functional_role?: string;
  functional_role_display?: string;
  assigned_uic?: string;
  authorized_cage_codes?: string[];
  installation: string | null;
  installation_code: string;
  installation_deployed_at: string;
  installation_location: string;
  installation_mission_category: string;
  security_clearance: string;
  operational_status: string;
  is_medically_deployable: boolean;
  is_training_current: boolean;
  is_fully_deployable: boolean;
  date_assigned: string;
  projected_rotation_date: string;
  dsn_phone: string;
  official_email: string;
}

export interface WorkOrderPartDetail {
  id: string;
  nsn: string;
  cage_code: string;
  nomenclature: string;
  quantity: number;
  unit_price: number | string;
  defective_serial_number: string;
  replacement_serial_number: string;
  requires_dlr_turn_in: boolean;
  dlr_turn_in_status: string;
}

export interface WorkOrderSummary {
  id: string;
  jcn?: string;
  status: string;
  status_display?: string;
  priority_code?: number;
  priority_display?: string;
  casrep_number?: string;
  is_grounding_fault: boolean;
  asset_serial?: string;
  system_nomenclature?: string;
  hull_number?: string;
  vessel_name?: string;
  installation_code?: string;
  eic?: string;
  apl_ael?: string;
  when_discovered_code?: string;
  when_discovered_display?: string;
  equipment_status_code?: string;
  equipment_status_display?: string;
  malfunction_code?: string;
  malfunction_display?: string;
  description_of_fault: string;
  completed_work?: string;
  action_taken_code?: string;
  initial_diagnostics?: string;
  safety_tag_out?: string;
  isolation_inspection?: string;
  fault_identification?: string;
  repair_replacement?: string;
  restoration_testing?: string;
  final_equipment_status?: string;
  maintenance_hours?: number | string;
  maintenance_submitted_at?: string | null;
  work_center?: string;
  technician_name: string;
  technician_clearance?: string;
  qa_inspector_name?: string;
  org_man_hours?: number | string;
  depot_man_hours?: number | string;
  tag_out_verified?: boolean;
  esd_precautions_verified?: boolean;
  cwp_controlled_work_package?: boolean;
  requires_mdf_zeroization?: boolean;
  mdf_zeroization_verified?: boolean;
  date_opened: string;
  date_closed?: string | null;
  parts_used_details?: WorkOrderPartDetail[];
}

export interface WorkOrderLifecycleEvent {
  event_type: string;
  previous_status: string | null;
  new_status: string;
  actor_name: string;
  occurred_at: string;
  reason: string;
}

export interface RequisitionSummary {
  id: string;
  document_number: string;
  material_name: string;
  nsn: string;
  cage_code?: string;
  unit_price?: number | string;
  quantity: number;
  priority_display: string;
  ipd_code?: string;
  supply_status_code?: string;
  supply_status_display?: string;
  requires_dlr_turn_in?: boolean;
  work_order_jcn?: string;
  status: string;
  estimated_delivery_date: string | null;
}

export interface MewAsset {
  id: string;
  assignment_id: string | null;
  serial_number: string;
  system_id: string | null;
  nomenclature: string;
  common_name: string;
  ew_category: string;
  asset_class: string;
  manufacturer: string;
  cage_code?: string;
  hull_number: string | null;
  vessel_name: string | null;
  ship_class: string | null;
  domain: 'Surface' | 'Subsurface' | 'Airborne' | 'Ground' | string | null;
  service_branch?: 'USN' | 'USMC' | 'USA' | 'USAF' | string | null;
  emcon_posture?: string | null;
  mission_role_code: string | null;
  mission_role_name: string | null;
  quantity_installed: number;
  operational_status: string;
  installation_date: string | null;
  assigned_unit_name: string;
  current_location: InstallationBase | null;
  readiness_status: 'FMC' | 'PMC' | 'NMCM' | 'NMCS' | 'DLM' | string;
  readiness_status_display: string;
  engine_hours: number | string;
  last_status_change: string;
  mtbf_hours: number;
  mttr_hours: number;
  operational_availability_pct: number;
  active_work_orders: WorkOrderSummary[];
  pending_requisitions: RequisitionSummary[];
}

export interface UnitReadinessTrend {
  snapshot_date: string;
  total_assets: number;
  fmc_assets: number;
  pmc_assets: number;
  nmc_assets: number;
  operational_readiness_rate: number;
}

export interface MilitaryUnitDashboard {
  id: string;
  name: string;
  uic: string;
  command_type: string;
  installation_name: string;
  personnel_readiness_pct: number | string;
  equipment_readiness_pct: number | string;
  readiness_trends: UnitReadinessTrend[];
  down_assets: MewAsset[];
  fleet_avg_mtbf: number | null;
  fleet_avg_mttr: number | null;
  fleet_avg_ao: number | null;
}

export interface MewComponent {
  id: string;
  component_id: string;
  nsn: string;
  nomenclature: string;
  part_number: string;
  cage_code: string;
  manufacturer: string;
  component_category: string;
  repairability_code: string;
  unit_of_issue: string;
  unit_price: number | string;
  critical_item: boolean;
  lead_time_days: number;
  expected_mtbf_hours: number;
  compatible_system_ids: string[];
}

export interface InventoryBalance {
  id: string;
  installation: string;
  installation_code: string;
  installation_name: string;
  material: string;
  component_id: string;
  nsn: string;
  nomenclature: string;
  component_category: string;
  unit_price: number | string;
  critical_item: boolean;
  quantity_on_hand: number;
  reorder_point: number;
  safety_stock: number;
  needs_replenishment: boolean;
}

export interface InventoryTransactionRecord {
  id: string;
  created_at: string;
  installation_name: string;
  nsn: string;
  nomenclature: string;
  transaction_type: 'RCT' | 'DED' | 'ADJ' | string;
  transaction_type_display: string;
  quantity: number;
  previous_balance: number;
  new_balance: number;
  reference_document: string;
  performed_by_name: string;
}

export interface ReceiveShipmentPayload {
  document_number: string;
  received_quantity?: number | null;
}

export interface ReceiveShipmentResponse {
  message: string;
  document_number: string;
  material_received: string;
  quantity_added: number;
  backorder_notice?: string;
}

export interface RbacPersona {
  personnel_id: string;
  name: string;
  rank: string;
  title: string;
  functional_role: string;
  personnel_category: string;
  security_clearance: string;
  assigned_uic: string;
  installation_code: string;
  target_hull_number: string | null;
  authorized_cage_codes: string[];
  badge_label: string;
}

export interface RbacActiveContext {
  persona_id: string;
  actor_name: string;
  functional_role: string;
  personnel_category: string;
  security_clearance: string;
  assigned_uic: string;
  installation_code: string;
  subordinate_uics: string[];
  authorized_cage_codes: string[];
  active_redaction_policies: string[];
  can_export_csv: boolean;
  capabilities: {
    can_create_asset: boolean;
    can_update_asset: boolean;
    can_delete_asset: boolean;
    can_create_work_order: boolean;
    can_close_work_order: boolean;
    can_delete_work_order: boolean;
    can_create_requisition: boolean;
    can_delete_requisition: boolean;
  };
}

export interface AuthorizationContextPayload {
  active_context: RbacActiveContext;
}

export interface SecurityAuditLogRecord {
  id: string;
  created_at: string;
  actor_name: string;
  actor_role: string;
  actor_uic: string;
  actor_clearance: string;
  actor_cage_scope: string;
  event_type: string;
  event_type_display: string;
  decision: 'ALLOW' | 'MASKED' | 'DENY' | string;
  decision_display: string;
  resource_target: string;
  reason: string;
}

export interface AoSystemBreakdown {
  system_id: string;
  nomenclature: string;
  common_name: string;
  ew_category: string;
  domain: string;
  installed_count: number;
  fmc_count: number;
  pmc_count: number;
  nmc_count: number;
  mtbm_hours: number;
  mttr_hours: number;
  ldt_hours: number;
  mdt_hours: number;
  computed_ao_pct: number;
  total_operating_hours: number;
  swap_c_power_kw?: number;
  swap_c_cooling_gpm?: number;
  swap_c_weight_lbs?: number;
  vswr_ratio?: number;
  thermal_load_pct?: number;
}

export interface ComponentAnalyticsItem {
  component_id: string;
  nsn: string;
  nomenclature: string;
  component_category: string;
  manufacturer: string;
  cage_code: string;
  unit_price: number;
  critical_item: boolean;
  parent_lru_id: string | null;
  parent_lru_name: string;
  compatible_system_ids: string[];
  total_fleet_stock: number;
  total_reorder_point: number;
  active_faults: number;
  casrep_grounding_faults: number;
  lead_time_days: number;
  expected_mtbf_hours: number;
  monthly_demand_rate: number;
  days_to_depletion: number;
  depletion_status: 'CRITICAL_STOCKOUT_RISK' | 'REORDER_WARNING' | 'SUSTAINABLE' | string;
  casrep_impact_score: number;
}

export interface SupplyBottleneckStage {
  stage: string;
  count: number;
  severity: 'NOMINAL' | 'INFO' | 'WARN' | 'CRIT' | string;
}

export interface NecStaffingItem {
  nec_code: string;
  system_id: string;
  system_name: string;
  total_qualified: number;
  deployable_count: number;
  required_billets: number;
  coverage_pct: number;
  spof_risk: 'CRITICAL_SPOF_GAP' | 'MARGINAL_COVERAGE' | 'RESILIENT' | string;
}

export interface GeospatialNode {
  code: string;
  name: string;
  mission_category: string;
  lat: number;
  lon: number;
  readiness_rating: string;
  personnel_count: number;
}

export interface ProximityDepotRoute {
  hull_number: string;
  vessel_name: string;
  readiness_status: string;
  system_nomenclature: string;
  origin_homeport: string;
  origin_lat: number | null;
  origin_lon: number | null;
  needed_component_id: string;
  needed_component_name: string;
  nearest_depot_code: string;
  nearest_depot_name: string;
  depot_qty_available: number;
  distance_nm: number;
  estimated_transit_hours: number;
}

export interface MewAnalyticsPayload {
  ao_system_breakdown: AoSystemBreakdown[];
  component_analytics: ComponentAnalyticsItem[];
  supply_bottleneck_funnel: SupplyBottleneckStage[];
  nec_staffing_matrix: NecStaffingItem[];
  geospatial_nodes: GeospatialNode[];
  proximity_depot_routes: ProximityDepotRoute[];
  lifecycle_metrics?: { qa_pending: number; rework_events: number; returned_to_service: number; closed: number; transition_events: number; avg_qa_turnaround_hours: number; avg_closure_turnaround_hours: number; avg_qa_aging_hours: number; rework_rate_pct: number };
  supply_metrics?: { open_requisitions: number; fill_rate_pct: number; priority_one_open: number; avg_estimated_lead_days: number };
  supply_breakdown?: { installation_code: string; installation_name: string; total: number; received: number; open: number; priority_one_open: number; avg_lead_days: number }[];
}


