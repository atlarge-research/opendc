import React from "react";
import UnitContainer from "../../../../containers/sidebars/topology/machine/UnitContainer";

const UnitListComponent = ({unitType, unitIds}) => (
    <ul className="list-group mt-1">
        {unitIds.length !== 0 ?
            unitIds.map((unitId, index) => (
                <UnitContainer unitType={unitType} unitId={unitId} index={index} key={index}/>
            )) :
            <div className="alert alert-info">
                <strong>No units...</strong> Add some with the menu above!
            </div>
        }
    </ul>
);

export default UnitListComponent;
