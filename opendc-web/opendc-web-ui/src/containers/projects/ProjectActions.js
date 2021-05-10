import React from 'react'
import { useDispatch } from 'react-redux'
import { deleteProject } from '../../actions/projects'
import ProjectActionButtons from '../../components/projects/ProjectActionButtons'

const ProjectActions = (props) => {
    const dispatch = useDispatch()
    const actions = {
        onViewUsers: (id) => {}, // TODO implement user viewing
        onDelete: (id) => dispatch(deleteProject(id)),
    }
    return <ProjectActionButtons {...props} {...actions} />
}

export default ProjectActions
