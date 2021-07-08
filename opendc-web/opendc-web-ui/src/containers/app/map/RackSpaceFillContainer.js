import React from 'react'
import { useSelector } from 'react-redux'
import RackFillBar from '../../../components/app/map/elements/RackFillBar'

const RackSpaceFillContainer = (props) => {
    const state = useSelector((state) => {
        const machineIds = state.objects.rack[state.objects.tile[props.tileId].rack].machines
        return {
            type: 'space',
            fillFraction: machineIds.filter((id) => id !== null).length / machineIds.length,
        }
    })
    return <RackFillBar {...props} {...state} />
}

export default RackSpaceFillContainer
