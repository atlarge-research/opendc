import PropTypes from 'prop-types'
import React from 'react'
import Link from 'next/link'
import { Button } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faPlay, faUsers, faTrash } from '@fortawesome/free-solid-svg-icons'

const ProjectActionButtons = ({ projectId, onViewUsers, onDelete }) => (
    <td className="text-right">
        <Link href={`/projects/${projectId}`}>
            <Button color="primary" outline size="sm" className="mr-2" title="Open this project">
                <FontAwesomeIcon icon={faPlay} />
            </Button>
        </Link>
        <Button
            color="success"
            outline
            size="sm"
            disabled
            className="mr-2"
            title="View and edit collaborators (not supported currently)"
            onClick={() => onViewUsers(projectId)}
        >
            <FontAwesomeIcon icon={faUsers} />
        </Button>
        <Button color="danger" outline size="sm" title="Delete this project" onClick={() => onDelete(projectId)}>
            <FontAwesomeIcon icon={faTrash} />
        </Button>
    </td>
)

ProjectActionButtons.propTypes = {
    projectId: PropTypes.string.isRequired,
    onViewUsers: PropTypes.func,
    onDelete: PropTypes.func,
}

export default ProjectActionButtons
