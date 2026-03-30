import PropTypes from 'prop-types'
import React from 'react'
import { control, toolBar } from './Toolbar.module.css'
import { Button } from '@patternfly/react-core'
import { SearchPlusIcon, SearchMinusIcon, CameraIcon } from '@patternfly/react-icons'

function Toolbar({ onZoom, onExport }) {
    return (
        <div className={toolBar}>
            <Button variant="tertiary" title="Zoom in" onClick={() => onZoom(true)} className={control}>
                <SearchPlusIcon />
            </Button>
            <Button variant="tertiary" title="Zoom out" onClick={() => onZoom(false)} className={control}>
                <SearchMinusIcon />
            </Button>
            <Button
                variant="tertiary"
                title="Export Canvas to PNG Image"
                onClick={() => onExport()}
                className={control}
            >
                <CameraIcon />
            </Button>
        </div>
    )
}

Toolbar.propTypes = {
    onZoom: PropTypes.func,
    onExport: PropTypes.func,
}

export default Toolbar
