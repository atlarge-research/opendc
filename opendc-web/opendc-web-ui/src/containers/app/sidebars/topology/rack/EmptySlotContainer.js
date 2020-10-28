import React from 'react'
import { useDispatch } from 'react-redux'
import { addMachine } from '../../../../../actions/topology/rack'
import EmptySlotComponent from '../../../../../components/app/sidebars/topology/rack/EmptySlotComponent'

const EmptySlotContainer = (props) => {
    const dispatch = useDispatch()
    const onAdd = () => dispatch(addMachine(props.position))
    return <EmptySlotComponent {...props} onAdd={onAdd} />
}

export default EmptySlotContainer
