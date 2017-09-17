import React from "react";
import {VictoryChart, VictoryLine, VictoryScatter} from "victory";

const LoadChartComponent = ({data, tick}) => (
    <VictoryChart height={300}>
        <VictoryLine
            data={data}
        />
        <VictoryScatter
            data={data}
            size={5}
        />
    </VictoryChart>
);

export default LoadChartComponent;
