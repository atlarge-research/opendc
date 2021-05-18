import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faCamera } from '@fortawesome/free-solid-svg-icons'

const ExportCanvasComponent = () => (
    <button
        className="btn btn-success btn-circle btn-sm"
        title="Export Canvas to PNG Image"
        onClick={() => window['exportCanvasToImage']()}
    >
        <FontAwesomeIcon icon={faCamera} />
    </button>
)

export default ExportCanvasComponent
