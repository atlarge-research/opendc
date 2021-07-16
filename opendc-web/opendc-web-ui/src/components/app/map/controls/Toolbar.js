import PropTypes from 'prop-types'
import React from 'react'
import { control, toolBar } from './Toolbar.module.scss'
import { Button } from '@patternfly/react-core'
import { SearchPlusIcon, SearchMinusIcon } from '@patternfly/react-icons'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCamera } from '@fortawesome/free-solid-svg-icons'

const Toolbar = ({ onZoom, onExport }) => (
    <div className={toolBar}>
        <Button variant="tertiary" title="Zoom in" onClick={() => onZoom(true)} className={control}>
            <SearchPlusIcon />
        </Button>
        <Button variant="tertiary" title="Zoom out" onClick={() => onZoom(false)} className={control}>
            <SearchMinusIcon />
        </Button>
        <Button variant="tertiary" title="Export Canvas to PNG Image" onClick={() => onExport()} className={control}>
            <FontAwesomeIcon icon={faCamera} />
        </Button>
    </div>
)

Toolbar.propTypes = {
    onZoom: PropTypes.func,
    onExport: PropTypes.func,
}

export default Toolbar
