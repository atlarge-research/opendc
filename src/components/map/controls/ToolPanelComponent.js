import React from "react";
import ZoomControlContainer from "../../../containers/map/controls/ZoomControlContainer";
import ExportCanvasComponent from "./ExportCanvasComponent";
import "./ToolPanelComponent.css";

const ToolPanelComponent = () => (
    <div className="tool-panel">
        <ZoomControlContainer/>
        <ExportCanvasComponent/>
    </div>
);

export default ToolPanelComponent;
