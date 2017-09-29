import React from "react";
import ReactDOM from "react-dom/server";
import {VictoryAxis, VictoryChart, VictoryLine, VictoryScatter} from "victory";
import {convertSecondsToFormattedTime} from "../../../../util/date-time";

const LoadChartComponent = ({data, currentTick}) => {
    const onExport = () => {
        const newWindow = window.open("");
        newWindow.document.write(ReactDOM.renderToString(
            <VictoryChartComponent data={data} currentTick={currentTick} showCurrentTick={false}/>
        ));
        newWindow.document.title = "OpenDC Chart Export";
    };

    return (
        <div className="mt-1" style={{position: "relative"}}>
            <strong>Load over time</strong>
            <VictoryChartComponent data={data} currentTick={currentTick} showCurrentTick={true}/>
            <ExportChartComponent onExport={onExport}/>
        </div>
    );
};

const VictoryChartComponent = ({data, currentTick, showCurrentTick}) => (
    <VictoryChart
        height={250}
        padding={{top: 10, bottom: 50, left: 50, right: 50}}
    >
        <VictoryAxis
            tickFormat={tick => convertSecondsToFormattedTime(tick)}
            fixLabelOverlap={true}
            label="Simulated Time"
        />
        <VictoryAxis
            dependentAxis
            label="Load"
        />
        <VictoryLine
            data={data}
        />
        <VictoryScatter
            data={data}
        />
        {showCurrentTick ?
            <VictoryLine
                data={[
                    {x: currentTick + 1, y: 0},
                    {x: currentTick + 1, y: 1},
                ]}
                style={{
                    data: {stroke: "#00A6D6", strokeWidth: 3}
                }}
            /> :
            undefined
        }
    </VictoryChart>
);

const ExportChartComponent = ({onExport}) => (
    <button
        className="btn btn-success btn-circle btn-sm"
        title="Export Chart to PNG Image"
        onClick={onExport}
        style={{position: "absolute", top: 0, right: 0}}
    >
        <span className="fa fa-camera"/>
    </button>
);

export default LoadChartComponent;
