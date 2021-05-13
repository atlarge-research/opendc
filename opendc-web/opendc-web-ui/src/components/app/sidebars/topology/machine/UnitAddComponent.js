import PropTypes from 'prop-types'
import React, { useRef } from 'react'

function UnitAddComponent({ units, onAdd }) {
    const unitSelect = useRef(null)

    return (
        <div className="form-inline">
            <div className="form-group w-100">
                <select className="form-control w-70 mr-1" ref={unitSelect}>
                    {units.map((unit) => (
                        <option value={unit._id} key={unit._id}>
                            {unit.name}
                        </option>
                    ))}
                </select>
                <button
                    type="submit"
                    className="btn btn-outline-primary"
                    onClick={() => onAdd(unitSelect.current.value)}
                >
                    <span className="fa fa-plus mr-2" />
                    Add
                </button>
            </div>
        </div>
    )
}

UnitAddComponent.propTypes = {
    units: PropTypes.array.isRequired,
    onAdd: PropTypes.func.isRequired,
}

export default UnitAddComponent
