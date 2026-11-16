export interface PaginatedResponse<T> {
  count: number;
  next: string | null;
  previous: string | null;
  results: T[];
}

export type MewViewpoint =
  | 'readiness'
  | 'loadouts'
  | 'supply'
  | 'installations'
  | 'ledger';

export type OperationalDomainFilter = 'ALL' | 'Surface' | 'Subsurface' | 'Airborne';

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

export interface WorkOrderSummary {
  id: string;
  description_of_fault: string;
  technician_name: string;
  date_opened: string;
  is_grounding_fault: boolean;
  status: string;
}

export interface RequisitionSummary {
  id: string;
  document_number: string;
  material_name: string;
  nsn: string;
  quantity: number;
  priority_display: string;
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
  hull_number: string | null;
  vessel_name: string | null;
  ship_class: string | null;
  domain: 'Surface' | 'Subsurface' | 'Airborne' | string | null;
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
