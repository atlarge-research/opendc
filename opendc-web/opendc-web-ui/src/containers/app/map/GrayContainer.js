import React from 'react'
import { useDispatch } from 'react-redux'
import { goDownOneInteractionLevel } from '../../../actions/interaction-level'
import GrayLayer from '../../../components/app/map/elements/GrayLayer'

const GrayContainer = () => {
    const dispatch = useDispatch()
    const onClick = () => dispatch(goDownOneInteractionLevel())
    return <GrayLayer onClick={onClick} />
}

export default GrayContainer
