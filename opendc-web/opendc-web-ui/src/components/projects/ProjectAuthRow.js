import classNames from 'classnames'
import React from 'react'
import ProjectActions from '../../containers/projects/ProjectActions'
import { Authorization } from '../../shapes/index'
import { AUTH_DESCRIPTION_MAP, AUTH_ICON_MAP } from '../../util/authorizations'
import { parseAndFormatDateTime } from '../../util/date-time'

const ProjectAuthRow = ({ projectAuth }) => (
    <tr>
        <td className="pt-3">{projectAuth.project.name}</td>
        <td className="pt-3">{parseAndFormatDateTime(projectAuth.project.datetimeLastEdited)}</td>
        <td className="pt-3">
            <span className={classNames('fa', 'fa-' + AUTH_ICON_MAP[projectAuth.authorizationLevel], 'mr-2')} />
            {AUTH_DESCRIPTION_MAP[projectAuth.authorizationLevel]}
        </td>
        <ProjectActions projectId={projectAuth.project._id} />
    </tr>
)

ProjectAuthRow.propTypes = {
    projectAuth: Authorization.isRequired,
}

export default ProjectAuthRow
