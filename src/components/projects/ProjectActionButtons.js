import PropTypes from "prop-types";
import React from 'react';

const ProjectActionButtons = ({projectId, onOpen, onViewUsers, onDelete}) => (
    <div className="project-icons">
        <div className="open" title="Open this project" onClick={() => onOpen(projectId)}>
            <span className="fa fa-play"/>
        </div>
        <div className="users" title="View and edit collaborators on this project"
             onClick={() => onViewUsers(projectId)}>
            <span className="fa fa-users"/>
        </div>
        <div className="delete" title="Delete this project" onClick={() => onDelete(projectId)}>
            <span className="fa fa-trash"/>
        </div>
    </div>
);

ProjectActionButtons.propTypes = {
    projectId: PropTypes.number.isRequired,
    onOpen: PropTypes.func,
    onViewUsers: PropTypes.func,
    onDelete: PropTypes.func,
};

export default ProjectActionButtons;
