import React from 'react'
import ProjectActionButtons from '../../components/projects/ProjectActionButtons'
import { useMutation, useQueryClient } from 'react-query'
import { useAuth } from '../../auth'
import { deleteProject } from '../../api/projects'

const ProjectActions = (props) => {
    const auth = useAuth()
    const queryClient = useQueryClient()
    const mutation = useMutation((projectId) => deleteProject(auth, projectId), {
        onSuccess: () => queryClient.invalidateQueries('projects'),
    })
    const actions = {
        onViewUsers: (id) => {}, // TODO implement user viewing
        onDelete: (id) => mutation.mutate(id),
    }
    return <ProjectActionButtons {...props} {...actions} />
}

export default ProjectActions
