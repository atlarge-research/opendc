import React from 'react'
import ZoomControlContainer from '../../../../containers/app/map/controls/ZoomControlContainer'
import ExportCanvasComponent from './ExportCanvasComponent'
import { toolPanel } from './ToolPanelComponent.module.scss'

const ToolPanelComponent = () => (
    <div className={toolPanel}>
        <ZoomControlContainer />
        <ExportCanvasComponent />
    </div>
)

export default ToolPanelComponent
