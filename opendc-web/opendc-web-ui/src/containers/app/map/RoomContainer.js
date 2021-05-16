import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { goFromBuildingToRoom } from '../../../redux/actions/interaction-level'
import RoomGroup from '../../../components/app/map/groups/RoomGroup'

const RoomContainer = (props) => {
    const state = useSelector((state) => {
        return {
            interactionLevel: state.interactionLevel,
            currentRoomInConstruction: state.construction.currentRoomInConstruction,
            room: state.objects.room[props.roomId],
        }
    })
    const dispatch = useDispatch()
    return <RoomGroup {...props} {...state} onClick={() => dispatch(goFromBuildingToRoom(props.roomId))} />
}

export default RoomContainer
