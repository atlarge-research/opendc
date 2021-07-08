import React from 'react'
import ProjectActionButtons from '../../components/projects/ProjectActionButtons'
import { useMutation } from 'react-query'

const ProjectActions = (props) => {
    const { mutate: deleteProject } = useMutation('deleteProject')
    const actions = {
        onViewUsers: (id) => {}, // TODO implement user viewing
        onDelete: (id) => deleteProject(id),
    }
    return <ProjectActionButtons {...props} {...actions} />
}

export default ProjectActions
