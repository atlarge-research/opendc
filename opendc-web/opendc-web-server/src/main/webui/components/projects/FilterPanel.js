import React from 'react'
import PropTypes from 'prop-types'
import { ToggleGroup, ToggleGroupItem } from '@patternfly/react-core'
import { filterPanel } from './FilterPanel.module.css'

export const FILTERS = { SHOW_ALL: 'All Projects', SHOW_OWN: 'My Projects', SHOW_SHARED: 'Shared with me' }

const FilterPanel = ({ onSelect, activeFilter = 'SHOW_ALL' }) => (
    <ToggleGroup className={`${filterPanel} pf-u-mb-sm`}>
        {Object.keys(FILTERS).map((filter) => (
            <ToggleGroupItem
                key={filter}
                onChange={() => activeFilter === filter || onSelect(filter)}
                isSelected={activeFilter === filter}
                text={FILTERS[filter]}
            />
        ))}
    </ToggleGroup>
)

FilterPanel.propTypes = {
    onSelect: PropTypes.func.isRequired,
    activeFilter: PropTypes.string,
}

export default FilterPanel
