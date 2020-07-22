import PropTypes from 'prop-types'
import React from 'react'

class UnitAddComponent extends React.Component {
    static propTypes = {
        units: PropTypes.array.isRequired,
        onAdd: PropTypes.func.isRequired,
    }

    render() {
        return (
            <div className="form-inline">
                <div className="form-group w-100">
                    <select className="form-control w-75 mr-1" ref={(unitSelect) => (this.unitSelect = unitSelect)}>
                        {this.props.units.map((unit) => (
                            <option value={unit._id} key={unit._id}>
                                {unit.name}
                            </option>
                        ))}
                    </select>
                    <button
                        type="submit"
                        className="btn btn-outline-primary"
                        onClick={() => this.props.onAdd(this.unitSelect.value)}
                    >
                        <span className="fa fa-plus mr-2" />
                        Add
                    </button>
                </div>
            </div>
        )
    }
}

export default UnitAddComponent
