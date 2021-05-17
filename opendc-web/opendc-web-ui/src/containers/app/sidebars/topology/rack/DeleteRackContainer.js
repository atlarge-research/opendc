import React, { useState } from 'react'
import { useDispatch } from 'react-redux'
import ConfirmationModal from '../../../../../components/modals/ConfirmationModal'
import { deleteRack } from '../../../../../redux/actions/topology/rack'
import { Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faTrash } from '@fortawesome/free-solid-svg-icons'

const DeleteRackContainer = () => {
    const dispatch = useDispatch()
    const [isVisible, setVisible] = useState(false)
    const callback = (isConfirmed) => {
        if (isConfirmed) {
            dispatch(deleteRack())
        }
        setVisible(false)
    }
    return (
        <>
            <Button color="danger" outline block onClick={() => setVisible(true)}>
                <FontAwesomeIcon icon={faTrash} className="mr-2" />
                Delete this rack
            </Button>
            <ConfirmationModal
                title="Delete this rack"
                message="Are you sure you want to delete this rack?"
                show={isVisible}
                callback={callback}
            />
        </>
    )
}

export default DeleteRackContainer
