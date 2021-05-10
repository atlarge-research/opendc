import React from 'react'
import { useSelector } from 'react-redux'
import UnitListComponent from '../../../../../components/app/sidebars/topology/machine/UnitListComponent'

const UnitListContainer = (props) => {
    const unitIds = useSelector(
        (state) =>
            state.objects.machine[
                state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rackId].machineIds[
                    state.interactionLevel.position - 1
                ]
            ][props.unitType + 'Ids']
    )
    return <UnitListComponent {...props} unitIds={unitIds} />
}

export default UnitListContainer
