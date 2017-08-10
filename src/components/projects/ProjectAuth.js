import classNames from 'classnames';
import React from 'react';
import ProjectActions from "../../containers/projects/ProjectActions";
import Shapes from "../../shapes/index";
import {AUTH_DESCRIPTION_MAP, AUTH_ICON_MAP} from "../../util/authorizations";
import {parseAndFormatDateTime} from "../../util/date-time";

const ProjectAuth = ({projectAuth}) => (
    <div className="project-row">
        <div>{projectAuth.simulation.name}</div>
        <div>{parseAndFormatDateTime(projectAuth.simulation.datetimeLastEdited)}</div>
        <div>
            <span className={classNames("fa", "fa-" + AUTH_ICON_MAP[projectAuth.authorizationLevel])}/>
            {AUTH_DESCRIPTION_MAP[projectAuth.authorizationLevel]}
        </div>
        <ProjectActions projectId={projectAuth.simulation.id}/>
    </div>
);

ProjectAuth.propTypes = {
    projectAuth: Shapes.Authorization.isRequired,
};

export default ProjectAuth;
