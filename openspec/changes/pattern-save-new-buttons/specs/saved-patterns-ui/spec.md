## MODIFIED Requirements

### Requirement: Apply Pattern button removed
The frontend SHALL NOT display an "Apply Pattern" button. The `ApplyPatternButton` component and its associated `handleApplyPattern` handler are removed.

#### Scenario: Apply Pattern button absent
- **WHEN** the Pattern tab controls are rendered
- **THEN** no Apply Pattern button is visible

### Requirement: Save and New buttons replace Apply Pattern button
The frontend SHALL display a **Save** button and a **New** button in place of the former Apply Pattern button.

#### Scenario: Save and New buttons present
- **WHEN** the Pattern tab controls are rendered
- **THEN** both a Save button and a New button are visible in the controls area
