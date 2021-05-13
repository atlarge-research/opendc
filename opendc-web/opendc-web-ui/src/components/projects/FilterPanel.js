import React from 'react'
import PropTypes from 'prop-types'
import { Button, ButtonGroup } from 'reactstrap'
import { filterPanel } from './FilterPanel.module.scss'

export const FILTERS = { SHOW_ALL: 'All Projects', SHOW_OWN: 'My Projects', SHOW_SHARED: 'Shared with me' }

const FilterPanel = ({ onSelect, activeFilter = 'SHOW_ALL' }) => (
    <ButtonGroup className={`${filterPanel} mb-2`}>
        {Object.keys(FILTERS).map((filter) => (
            <Button
                color="secondary"
                key={filter}
                onClick={() => activeFilter === filter || onSelect(filter)}
                active={activeFilter === filter}
            >
                {FILTERS[filter]}
            </Button>
        ))}
    </ButtonGroup>
)

FilterPanel.propTypes = {
    onSelect: PropTypes.func.isRequired,
    activeFilter: PropTypes.string,
}

export default FilterPanel
