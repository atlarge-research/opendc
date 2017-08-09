import PropTypes from 'prop-types';
import React from 'react';
import Shapes from "../../shapes/index";
import NoProjectsAlert from "./NoProjectsAlert";
import ProjectAuth from "./ProjectAuth";
import "./ProjectAuthList.css";

const ProjectAuthList = ({authorizations}) => {
    if (authorizations.length === 0) {
        return <NoProjectsAlert/>;
    }

    return (
        <div className="project-list">
            <div className="list-head">
                <div>Project name</div>
                <div>Last edited</div>
                <div>Access rights</div>
            </div>
            <div className="list-body">
                {authorizations.map(authorization => (
                    <ProjectAuth projectAuth={authorization} key={authorization.simulation.id}/>
                ))}
            </div>
        </div>
    );
};

ProjectAuthList.propTypes = {
    authorizations: PropTypes.arrayOf(Shapes.Authorization).isRequired,
    onProjectClick: PropTypes.func.isRequired,
};

export default ProjectAuthList;
