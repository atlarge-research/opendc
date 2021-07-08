import PropTypes from 'prop-types'
import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import UnitListComponent from '../../../../../components/app/sidebars/topology/machine/UnitListComponent'
import { deleteUnit } from '../../../../../redux/actions/topology/machine'

const unitMapping = {
    cpu: 'cpus',
    gpu: 'gpus',
    memory: 'memories',
    storage: 'storages',
}

const UnitListContainer = ({ unitType, ...props }) => {
    const dispatch = useDispatch()
    const units = useSelector((state) => {
        const machine =
            state.objects.machine[
                state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rack].machines[
                    state.interactionLevel.position - 1
                ]
            ]
        return machine[unitMapping[unitType]].map((id) => state.objects[unitType][id])
    })
    const onDelete = (unit, unitType) => dispatch(deleteUnit(unitType, unit._id))

    return <UnitListComponent {...props} units={units} unitType={unitType} onDelete={onDelete} />
}

UnitListContainer.propTypes = {
    unitType: PropTypes.string.isRequired,
}

export default UnitListContainer
