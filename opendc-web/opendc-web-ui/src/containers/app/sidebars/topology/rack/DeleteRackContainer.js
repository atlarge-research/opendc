import React from 'react'
import { useDispatch } from 'react-redux'
import { openDeleteRackModal } from '../../../../../actions/modals/topology'
import DeleteRackComponent from '../../../../../components/app/sidebars/topology/rack/DeleteRackComponent'

const DeleteRackContainer = (props) => {
    const dispatch = useDispatch()
    return <DeleteRackComponent {...props} onClick={() => dispatch(openDeleteRackModal())} />
}

export default DeleteRackContainer
