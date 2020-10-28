import React from 'react'
import { useDispatch } from 'react-redux'
import { goDownOneInteractionLevel } from '../../../../../actions/interaction-level'
import BackToRoomComponent from '../../../../../components/app/sidebars/topology/rack/BackToRoomComponent'

const BackToRoomContainer = (props) => {
    const dispatch = useDispatch()
    return <BackToRoomComponent {...props} onClick={() => dispatch(goDownOneInteractionLevel())} />
}

export default BackToRoomContainer
