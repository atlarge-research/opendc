import React from 'react'
import ProjectActions from '../../containers/projects/ProjectActions'
import { Project } from '../../shapes'
import { AUTH_DESCRIPTION_MAP, AUTH_ICON_MAP } from '../../util/authorizations'
import { parseAndFormatDateTime } from '../../util/date-time'
import { useAuth } from '../../auth'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'

const ProjectRow = ({ project }) => {
    const { user } = useAuth()
    const { level } = project.authorizations.find((auth) => auth.userId === user.sub)
    return (
        <tr>
            <td className="pt-3">{project.name}</td>
            <td className="pt-3">{parseAndFormatDateTime(project.datetimeLastEdited)}</td>
            <td className="pt-3">
                <FontAwesomeIcon icon={AUTH_ICON_MAP[level]} className="mr-2" />
                {AUTH_DESCRIPTION_MAP[level]}
            </td>
            <ProjectActions projectId={project._id} />
        </tr>
    )
}

ProjectRow.propTypes = {
    project: Project.isRequired,
}

export default ProjectRow
