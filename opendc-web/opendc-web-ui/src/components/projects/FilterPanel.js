import React from 'react'
import FilterLink from '../../containers/projects/FilterLink'
import { filterPanel } from './FilterPanel.module.scss'

const FilterPanel = () => (
    <div className={`btn-group ${filterPanel} mb-2`}>
        <FilterLink filter="SHOW_ALL">All Projects</FilterLink>
        <FilterLink filter="SHOW_OWN">My Projects</FilterLink>
        <FilterLink filter="SHOW_SHARED">Shared with me</FilterLink>
    </div>
)

export default FilterPanel
