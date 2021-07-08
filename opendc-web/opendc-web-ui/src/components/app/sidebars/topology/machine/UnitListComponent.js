import PropTypes from 'prop-types'
import React from 'react'
import { ProcessingUnit, StorageUnit } from '../../../../../shapes'
import UnitComponent from './UnitComponent'

const UnitListComponent = ({ unitType, units, onDelete }) => (
    <ul className="list-group mt-1">
        {units.length !== 0 ? (
            units.map((unit, index) => (
                <UnitComponent
                    unitType={unitType}
                    unit={unit}
                    onDelete={() => onDelete(unit, unitType)}
                    index={index}
                    key={index}
                />
            ))
        ) : (
            <div className="alert alert-info">
                <span>
                    <strong>No units...</strong> Add some with the menu above!
                </span>
            </div>
        )}
    </ul>
)

UnitListComponent.propTypes = {
    unitType: PropTypes.string.isRequired,
    units: PropTypes.arrayOf(PropTypes.oneOfType([ProcessingUnit, StorageUnit])).isRequired,
    onDelete: PropTypes.func,
}

export default UnitListComponent
