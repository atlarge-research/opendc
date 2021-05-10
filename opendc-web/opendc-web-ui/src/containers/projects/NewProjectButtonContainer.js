import React from 'react'
import { useDispatch } from 'react-redux'
import { openNewProjectModal } from '../../actions/modals/projects'
import NewProjectButtonComponent from '../../components/projects/NewProjectButtonComponent'

const NewProjectButtonContainer = (props) => {
    const dispatch = useDispatch()
    return <NewProjectButtonComponent {...props} onClick={() => dispatch(openNewProjectModal())} />
}

export default NewProjectButtonContainer
