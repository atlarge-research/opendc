import PropTypes from 'prop-types'
import React from 'react'
import {
    Button,
    DataList,
    DataListAction,
    DataListCell,
    DataListItem,
    DataListItemCells,
    DataListItemRow,
    DescriptionList,
    DescriptionListDescription,
    DescriptionListGroup,
    DescriptionListTerm,
    EmptyState,
    EmptyStateBody,
    EmptyStateIcon,
    Popover,
    Title,
} from '@patternfly/react-core'
import { CubesIcon, InfoIcon, TrashIcon } from '@patternfly/react-icons'
import { ProcessingUnit, StorageUnit } from '../../../../shapes'
import UnitType from './UnitType'

function UnitInfo({ unit, unitType }) {
    if (unitType === 'cpus' || unitType === 'gpus') {
        return (
            <DescriptionList>
                <DescriptionListGroup>
                    <DescriptionListTerm>Clock Frequency</DescriptionListTerm>
                    <DescriptionListDescription>{unit.clockRateMhz} MHz</DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                    <DescriptionListTerm>Number of Cores</DescriptionListTerm>
                    <DescriptionListDescription>{unit.numberOfCores}</DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                    <DescriptionListTerm>Energy Consumption</DescriptionListTerm>
                    <DescriptionListDescription>{unit.energyConsumptionW} W</DescriptionListDescription>
                </DescriptionListGroup>
            </DescriptionList>
        )
    }

    return (
        <DescriptionList>
            <DescriptionListGroup>
                <DescriptionListTerm>Speed</DescriptionListTerm>
                <DescriptionListDescription>{unit.speedMbPerS} Mb/s</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
                <DescriptionListTerm>Capacity</DescriptionListTerm>
                <DescriptionListDescription>{unit.sizeMb} MB</DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
                <DescriptionListTerm>Energy Consumption</DescriptionListTerm>
                <DescriptionListDescription>{unit.energyConsumptionW} W</DescriptionListDescription>
            </DescriptionListGroup>
        </DescriptionList>
    )
}

UnitInfo.propTypes = {
    unitType: UnitType.isRequired,
    unit: PropTypes.oneOfType([ProcessingUnit, StorageUnit]).isRequired,
}

function UnitListComponent({ unitType, units, onDelete }) {
    if (units.length === 0) {
        return (
            <EmptyState>
                <EmptyStateIcon icon={CubesIcon} />
                <Title headingLevel="h5" size="lg">
                    No units found
                </Title>
                <EmptyStateBody>You have not configured any units yet. Add some with the menu above!</EmptyStateBody>
            </EmptyState>
        )
    }

    return (
        <DataList aria-label="Machine Units" isCompact>
            {units.map((unit, index) => (
                <DataListItem key={index}>
                    <DataListItemRow>
                        <DataListItemCells dataListCells={[<DataListCell key="unit">{unit.name}</DataListCell>]} />
                        <DataListAction id="goto" aria-label="Goto Machine" aria-labelledby="goto">
                            <Popover
                                headerContent="Unit Information"
                                bodyContent={<UnitInfo unitType={unitType} unit={unit} />}
                            >
                                <Button isSmall variant="plain" className="pf-u-p-0">
                                    <InfoIcon />
                                </Button>
                            </Popover>
                            <Button isSmall variant="plain" className="pf-u-p-0" onClick={() => onDelete(units[index])}>
                                <TrashIcon />
                            </Button>
                        </DataListAction>
                    </DataListItemRow>
                </DataListItem>
            ))}
        </DataList>
    )
}

UnitListComponent.propTypes = {
    unitType: UnitType.isRequired,
    units: PropTypes.arrayOf(PropTypes.oneOfType([ProcessingUnit, StorageUnit])).isRequired,
    onDelete: PropTypes.func,
}

export default UnitListComponent
