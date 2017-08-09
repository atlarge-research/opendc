import React from 'react';
import FilterLink from "../../containers/projects/FilterLink";
import "./FilterPanel.css";

const ProjectFilterPanel = () => (
    <div className="filter-menu">
        <div className="project-filters">
            <FilterLink filter="SHOW_ALL">All Projects</FilterLink>
            <FilterLink filter="SHOW_OWN">My Projects</FilterLink>
            <FilterLink filter="SHOW_SHARED">Projects shared with me</FilterLink>
        </div>
    </div>
);

export default ProjectFilterPanel;
