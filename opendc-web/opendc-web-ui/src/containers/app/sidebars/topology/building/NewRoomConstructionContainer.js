import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import {
    cancelNewRoomConstruction,
    finishNewRoomConstruction,
    startNewRoomConstruction,
} from '../../../../../actions/topology/building'
import StartNewRoomConstructionComponent from '../../../../../components/app/sidebars/topology/building/NewRoomConstructionComponent'

const NewRoomConstructionButton = (props) => {
    const currentRoomInConstruction = useSelector((state) => state.construction.currentRoomInConstruction)

    const dispatch = useDispatch()
    const actions = {
        onStart: () => dispatch(startNewRoomConstruction()),
        onFinish: () => dispatch(finishNewRoomConstruction()),
        onCancel: () => dispatch(cancelNewRoomConstruction()),
    }
    return (
        <StartNewRoomConstructionComponent
            {...props}
            {...actions}
            currentRoomInConstruction={currentRoomInConstruction}
        />
    )
}

export default NewRoomConstructionButton
