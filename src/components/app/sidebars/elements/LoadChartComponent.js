import React from "react";
import {VictoryAxis, VictoryChart, VictoryLine, VictoryScatter} from "victory";
import {convertSecondsToFormattedTime} from "../../../../util/date-time";

const LoadChartComponent = ({data, currentTick}) => (
    <div className="mt-1">
        <strong>Load over time</strong>
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
            <VictoryLine
                data={[
                    {x: currentTick + 1, y: 0},
                    {x: currentTick + 1, y: 1},
                ]}
                style={{
                    data: {stroke: "#00A6D6", strokeWidth: 3}
                }}
            />
        </VictoryChart>
    </div>
);

export default LoadChartComponent;
