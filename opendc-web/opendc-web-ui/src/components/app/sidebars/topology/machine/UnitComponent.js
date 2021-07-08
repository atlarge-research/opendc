import PropTypes from 'prop-types'
import React from 'react'
import { UncontrolledPopover, PopoverHeader, PopoverBody, Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTrash, faInfoCircle } from '@fortawesome/free-solid-svg-icons'
import { ProcessingUnit, StorageUnit } from '../../../../../shapes'

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
                <Button outline={true} color="info" className="mr-1" id={`unit-${index}`}>
                    <FontAwesomeIcon icon={faInfoCircle} />
                </Button>
                <UncontrolledPopover trigger="focus" placement="left" target={`unit-${index}`}>
                    <PopoverHeader>Unit Information</PopoverHeader>
                    <PopoverBody>{unitInfo}</PopoverBody>
                </UncontrolledPopover>

                <Button outline color="danger" onClick={onDelete}>
                    <FontAwesomeIcon icon={faTrash} />
                </Button>
            </span>
        </li>
    )
}

UnitComponent.propTypes = {
    index: PropTypes.number,
    unitType: PropTypes.string,
    unit: PropTypes.oneOfType([ProcessingUnit, StorageUnit]),
    onDelete: PropTypes.func,
}

export default UnitComponent
