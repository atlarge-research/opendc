import PropTypes from 'prop-types'
import React from 'react'
import UnitContainer from '../../../../../containers/app/sidebars/topology/machine/UnitContainer'

const UnitListComponent = ({ unitType, unitIds }) => (
    <ul className="list-group mt-1">
        {unitIds.length !== 0 ? (
            unitIds.map((unitId, index) => (
                <UnitContainer unitType={unitType} unitId={unitId} index={index} key={index} />
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
    unitType: PropTypes.string,
    unitIds: PropTypes.array,
}

export default UnitListComponent
