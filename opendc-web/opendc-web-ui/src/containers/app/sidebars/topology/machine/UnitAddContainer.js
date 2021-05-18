import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { addUnit } from '../../../../../redux/actions/topology/machine'
import UnitAddComponent from '../../../../../components/app/sidebars/topology/machine/UnitAddComponent'

const UnitAddContainer = (props) => {
    const units = useSelector((state) => Object.values(state.objects[props.unitType]))
    const dispatch = useDispatch()

    const onAdd = (id) => dispatch(addUnit(props.unitType, id))

    return <UnitAddComponent {...props} onAdd={onAdd} units={units} />
}

export default UnitAddContainer
