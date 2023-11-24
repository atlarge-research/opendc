import PropTypes from 'prop-types'
import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import NameComponent from '../NameComponent'
import { editRackName } from '../../../../redux/actions/topology/rack'

const RackNameContainer = ({ tileId }) => {
    const { name: rackName, id } = useSelector((state) => state.topology.racks[state.topology.tiles[tileId].rack])
    const dispatch = useDispatch()
    const callback = (name) => {
        if (name) {
            dispatch(editRackName(id, name))
        }
    }
    return <NameComponent name={rackName} onEdit={callback} />
}

RackNameContainer.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default RackNameContainer
