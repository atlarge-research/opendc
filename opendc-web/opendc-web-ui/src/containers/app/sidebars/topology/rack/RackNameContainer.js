import React, { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import NameComponent from '../../../../../components/app/sidebars/topology/NameComponent'
import TextInputModal from '../../../../../components/modals/TextInputModal'
import { editRackName } from '../../../../../redux/actions/topology/rack'

const RackNameContainer = () => {
    const [isVisible, setVisible] = useState(false)
    const rackName = useSelector(
        (state) => state.objects.rack[state.objects.tile[state.interactionLevel.tileId].rack].name
    )
    const dispatch = useDispatch()
    const callback = (name) => {
        if (name) {
            dispatch(editRackName(name))
        }
        setVisible(false)
    }
    return (
        <>
            <NameComponent name={rackName} onEdit={() => setVisible(true)} />
            <TextInputModal
                title="Edit rack name"
                label="Rack name"
                show={isVisible}
                initialValue={rackName}
                callback={callback}
            />
        </>
    )
}

export default RackNameContainer
