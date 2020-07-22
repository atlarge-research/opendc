import PropTypes from 'prop-types'
import React from 'react'
import { Link } from 'react-router-dom'

const ProjectActionButtons = ({ projectId, onViewUsers, onDelete }) => (
    <td className="text-right">
        <Link to={'/projects/' + projectId} className="btn btn-outline-primary btn-sm mr-2" title="Open this project">
            <span className="fa fa-play" />
        </Link>
        <div
            className="btn btn-outline-success btn-sm disabled mr-2"
            title="View and edit collaborators (not supported currently)"
            onClick={() => onViewUsers(projectId)}
        >
            <span className="fa fa-users" />
        </div>
        <div className="btn btn-outline-danger btn-sm" title="Delete this project" onClick={() => onDelete(projectId)}>
            <span className="fa fa-trash" />
        </div>
    </td>
)

ProjectActionButtons.propTypes = {
    projectId: PropTypes.string.isRequired,
    onViewUsers: PropTypes.func,
    onDelete: PropTypes.func,
}

export default ProjectActionButtons
