import React from 'react';
import FilterLink from "../../containers/simulations/FilterLink";
import "./FilterPanel.css";

const FilterPanel = () => (
    <div className="filter-menu">
        <div className="simulation-filters">
            <FilterLink filter="SHOW_ALL">All Simulations</FilterLink>
            <FilterLink filter="SHOW_OWN">My Simulations</FilterLink>
            <FilterLink filter="SHOW_SHARED">Shared with me</FilterLink>
        </div>
    </div>
);

export default FilterPanel;
