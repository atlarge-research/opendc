import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { deleteUnit } from '../../../../../redux/actions/topology/machine'
import UnitComponent from '../../../../../components/app/sidebars/topology/machine/UnitComponent'

const UnitContainer = ({ unitId, unitType }) => {
    const dispatch = useDispatch()
    const unit = useSelector((state) => state.objects[unitType][unitId])
    const onDelete = () => dispatch(deleteUnit(unitType, unitId))

    return <UnitComponent index={unitId} unit={unit} unitType={unitType} onDelete={onDelete} />
}

export default UnitContainer
