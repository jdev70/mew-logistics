# Work Order Design Considerations
For a Django, PostgreSQL, and Angular logistics architecture, your database schema should capture five core data domains to mirror real-world fleet readiness, supply chain requisitions, and depot-level repairs.

1. Work Order & Tracking Identifiers

Every maintenance action is tracked in the Current Ship's Maintenance Project (CSMP) using strict alphanumeric identifiers.
• Job Control Number (JCN). A unique 11-character identifier combining the 5-character Unit Identification Code (UIC), the 2-character Work Center code, and a 4-digit serial number.
• Maintenance Action Form (MAF) Status. Tracks lifecycle states such as OPEN, DEFERRED, AWAITING_PARTS (AWP), IN_WORK, QA_PENDING, and CLOSED.
• Type Availability & Priority Codes. Standard DoD urgency indicators (e.g., 1 - Mandatory/CASREP, 2 - Essential, 3 - Routine, 4 - Desirable).
• CASREP (Casualty Report) Link. A foreign key or flag indicating if the component failure degrades the ship's primary warfare mission area (Electronic Attack/Electronic Support).

2. Configuration & Component Metadata

To tie a work order directly to your Line Replaceable Units (LRUs) and shipboard installations, the record requires exact logistics catalog keys.
• Equipment Identification Code (EIC). A 7-character code mapping the specific parent system and subsystem (e.g., D123456 for an AN/SLQ-32 antenna array or receiver).
• Allowance Parts List / Allowance Equipage List (APL/AEL). An 11-character logistic index linking the installed component to its authorized repair parts.
• National Stock Number (NSN) & CAGE Code. The 13-digit supply number and 5-character Commercial and Government Entity code for the defective LRU.
• Serial Number (Old vs. New). Tracks the serial number of the failed unit removed and the replacement unit installed for Mean Time Between Failures (MTBF) calculations.
• Hull / Installation ID. Foreign key linking the work order to a specific naval vessel (e.g., DDG-118) or shore test facility.

3. Failure & Diagnostic Codes

Fleet readiness analytics rely on structured failure codes rather than free text alone.
• When Discovered Code. Indicates how the fault was found (e.g., B - During Preventive Maintenance, C - Operational Testing, D - Normal Operation).
• Status Code. Describes equipment operability upon discovery (1 - Operational, 2 - Reduced Capability, 3 - Non-Operational).
• Malfunction Description Code. A 3-character standardized code categorizing the root cause (e.g., 020 - Burned Out, 160 - Low Gain/Output, 374 - Waveguide Arcing).
• Narrative / Discrepancy Text. Plain-text technician notes detailing built-in-test (BIT) fault codes, RF power loss readings, or cooling loop leaks.

4. Supply & Requisition Linkage

If a component requires replacement parts, the work order generates a MILSTRIP requisition record.
• Document Number. A 14-character requisition tracker (Service/UIC + Julian Date + Serial Number).
• Supply Status & Priority. Tracks requisition codes (e.g., BB - Backordered, BV - Shipped) alongside Issue Priority Designator (IPD) codes (01 through 15).
• Unit Price & Turn-In Requirement. Flags whether the failed EW component is a Depot Level Repairable (DLR) requiring carcass turn-in (e.g., a Traveling Wave Tube Amplifier) to avoid financial penalties.

5. Labor, QA, & Security Sign-Off

Because MEW systems process sensitive electronic intelligence and emit high-power RF, sign-offs require strict role and clearance validation.
• Work Center & Rate/Rank. Identifies the assigned shop (e.g., CE02 - Electronic Warfare Division) and lead technician (e.g., CTT1, ET2).
• Man-Hours Expended. Tracks both organizational-level (shipboard) and intermediate/depot-level labor hours.
• Quality Assurance (QA) & Safety Checklists. Flags for TAG-OUT (electrical/RF radiation isolation), ESD (Electrostatic Discharge), and Controlled Work Packages (CWP).
• Classification / COMSEC Handling. Flags whether the component contains volatile cryptographic keys or classified threat libraries (MDF - Mission Data Files) requiring zeroization prior to depot shipment.