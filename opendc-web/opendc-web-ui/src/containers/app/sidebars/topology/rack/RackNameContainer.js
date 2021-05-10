import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { openEditRackNameModal } from '../../../../../actions/modals/topology'
import RackNameComponent from '../../../../../components/app/sidebars/topology/rack/RackNameComponent'

const RackNameContainer = (props) => {
    const rackName = useSelector(
        (state) => state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rackId].name
    )
    const dispatch = useDispatch()
    return <RackNameComponent {...props} rackName={rackName} onEdit={() => dispatch(openEditRackNameModal())} />
}

export default RackNameContainer
