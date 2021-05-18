import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { startRackConstruction, stopRackConstruction } from '../../../../../redux/actions/topology/room'
import RackConstructionComponent from '../../../../../components/app/sidebars/topology/room/RackConstructionComponent'

const RackConstructionContainer = (props) => {
    const isRackConstructionMode = useSelector((state) => state.construction.inRackConstructionMode)
    const isEditingRoom = useSelector((state) => state.construction.currentRoomInConstruction !== '-1')

    const dispatch = useDispatch()
    const onStart = () => dispatch(startRackConstruction())
    const onStop = () => dispatch(stopRackConstruction())
    return (
        <RackConstructionComponent
            {...props}
            inRackConstructionMode={isRackConstructionMode}
            isEditingRoom={isEditingRoom}
            onStart={onStart}
            onStop={onStop}
        />
    )
}

export default RackConstructionContainer
