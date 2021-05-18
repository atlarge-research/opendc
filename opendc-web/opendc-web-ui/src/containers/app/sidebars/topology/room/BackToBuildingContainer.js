import React from 'react'
import { useDispatch } from 'react-redux'
import { goDownOneInteractionLevel } from '../../../../../redux/actions/interaction-level'
import BackToBuildingComponent from '../../../../../components/app/sidebars/topology/room/BackToBuildingComponent'

const BackToBuildingContainer = () => {
    const dispatch = useDispatch()
    const onClick = () => dispatch(goDownOneInteractionLevel())
    return <BackToBuildingComponent onClick={onClick} />
}

export default BackToBuildingContainer
