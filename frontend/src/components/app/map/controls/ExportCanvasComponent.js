import React from 'react'

const ExportCanvasComponent = () => (
    <button
        className="btn btn-success btn-circle btn-sm"
        title="Export Canvas to PNG Image"
        onClick={() => window['exportCanvasToImage']()}
    >
        <span className="fa fa-camera"/>
    </button>
)

export default ExportCanvasComponent
