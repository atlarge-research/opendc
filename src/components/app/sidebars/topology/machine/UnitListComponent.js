import React from "react";
import UnitContainer from "../../../../../containers/app/sidebars/topology/machine/UnitContainer";

const UnitListComponent = ({unitType, unitIds, inSimulation}) => (
    <ul className="list-group mt-1">
        {unitIds.length !== 0 ?
            unitIds.map((unitId, index) => (
                <UnitContainer unitType={unitType} unitId={unitId} index={index} key={index}/>
            )) :
            <div className="alert alert-info">
                {inSimulation ?
                    <strong>No units of this type in this machine</strong> :
                    <span><strong>No units...</strong> Add some with the menu above!</span>
                }
            </div>
        }
    </ul>
);

export default UnitListComponent;
