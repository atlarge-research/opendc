import PropTypes from "prop-types";
import React from "react";

class UnitAddComponent extends React.Component {
    static propTypes = {
        units: PropTypes.array.isRequired,
        onAdd: PropTypes.func.isRequired,
    };

    render() {
        return (
            <div className="form-inline">
                <div className="form-group w-100">
                    <select
                        className="form-control w-75 mr-1"
                        ref={unitSelect => this.unitSelect = unitSelect}
                    >
                        {this.props.units.map(unit => (
                            <option value={unit.id} key={unit.id}>
                                {unit.manufacturer + " " + unit.family + " " + unit.model + " " + unit.generation}
                            </option>
                        ))}
                    </select>
                    <button
                        type="submit"
                        className="btn btn-primary"
                        onClick={() => this.props.onAdd(parseInt(this.unitSelect.value, 10))}
                    >
                        <span className="fa fa-plus mr-2"/>
                        Add
                    </button>
                </div>
            </div>
        );
    }
}

export default UnitAddComponent;
