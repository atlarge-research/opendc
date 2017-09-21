import React from "react";
import {VictoryChart, VictoryLine, VictoryScatter} from "victory";

const LoadChartComponent = ({data, currentTick}) => (
    <VictoryChart height={300}>
        <VictoryLine
            data={data}
        />
        <VictoryScatter
            data={data}
            size={5}
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
);

export default LoadChartComponent;
