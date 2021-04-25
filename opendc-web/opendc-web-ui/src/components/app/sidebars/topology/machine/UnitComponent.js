import React from 'react'
import { UncontrolledPopover, PopoverHeader, PopoverBody, Button } from 'reactstrap'

function UnitComponent({ index, unitType, unit, onDelete }) {
    let unitInfo
    if (unitType === 'cpu' || unitType === 'gpu') {
        unitInfo = (
            <>
                <strong>Clockrate: </strong>
                <code>{unit.clockRateMhz}</code>
                <br />
                <strong>Num. Cores: </strong>
                <code>{unit.numberOfCores}</code>
                <br />
                <strong>Energy Cons.: </strong>
                <code>{unit.energyConsumptionW} W</code>
                <br />
            </>
        )
    } else if (unitType === 'memory' || unitType === 'storage') {
        unitInfo = (
            <>
                <strong>Speed:</strong>
                <code>{unit.speedMbPerS} Mb/s</code>
                <br />
                <strong>Size:</strong>
                <code>{unit.sizeMb} MB</code>
                <br />
                <strong>Energy Cons.:</strong>
                <code>{unit.energyConsumptionW} W</code>
                <br />
            </>
        )
    }

    return (
        <li className="d-flex list-group-item justify-content-between align-items-center">
            <span style={{ maxWidth: '60%' }}>{unit.name}</span>
            <span>
                <Button outline={true} color="info" className="mr-1 fa fa-info-circle" id={`unit-${index}`} />
                <UncontrolledPopover trigger="focus" placement="left" target={`unit-${index}`}>
                    <PopoverHeader>Unit Information</PopoverHeader>
                    <PopoverBody>{unitInfo}</PopoverBody>
                </UncontrolledPopover>

                <span className="btn btn-outline-danger fa fa-trash" onClick={onDelete} />
            </span>
        </li>
    )
}

export default UnitComponent
