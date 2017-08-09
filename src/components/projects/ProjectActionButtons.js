import PropTypes from "prop-types";
import React from 'react';

const ProjectActionButtons = ({onOpen, onViewUsers, onDelete}) => (
    <div className="project-icons">
        <div className="open" title="Open this project" onClick={onOpen}>
            <span className="fa fa-play"/>
        </div>
        <div className="users" title="View and edit collaborators on this project" onClick={onViewUsers}>
            <span className="fa fa-users"/>
        </div>
        <div className="delete" title="Delete this project" onClick={onDelete}>
            <span className="fa fa-trash"/>
        </div>
    </div>
);

ProjectActionButtons.propTypes = {
    onOpen: PropTypes.func.isRequired,
    onViewUsers: PropTypes.func.isRequired,
    onDelete: PropTypes.func.isRequired,
};

export default ProjectActionButtons;
