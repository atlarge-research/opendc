import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { closeNewProjectModal } from '../../actions/modals/projects'
import { addProject } from '../../actions/projects'
import TextInputModal from '../../components/modals/TextInputModal'

const NewProjectModal = (props) => {
    const visible = useSelector((state) => state.modals.newProjectModalVisible)
    const dispatch = useDispatch()
    const callback = (text) => {
        if (text) {
            dispatch(addProject(text))
        }
        dispatch(closeNewProjectModal())
    }
    return <TextInputModal title="New Project" label="Project title" show={visible} callback={callback} {...props} />
}

export default NewProjectModal
