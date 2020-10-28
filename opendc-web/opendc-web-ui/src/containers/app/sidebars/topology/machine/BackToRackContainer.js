import React from 'react'
import { useDispatch } from 'react-redux'
import { goDownOneInteractionLevel } from '../../../../../actions/interaction-level'
import BackToRackComponent from '../../../../../components/app/sidebars/topology/machine/BackToRackComponent'

const BackToRackContainer = (props) => {
    const dispatch = useDispatch()
    return <BackToRackComponent {...props} onClick={() => dispatch(goDownOneInteractionLevel())} />
}

export default BackToRackContainer
