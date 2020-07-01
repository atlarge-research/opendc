import React from 'react'
import ReactDOM from 'react-dom/server'
import SvgSaver from 'svgsaver'
import { VictoryAxis, VictoryChart, VictoryLabel, VictoryLine, VictoryScatter } from 'victory'
import { convertSecondsToFormattedTime } from '../../../../util/date-time'

const LoadChartComponent = ({ data, currentTick }) => {
    const onExport = () => {
        const div = document.createElement('div')
        div.innerHTML = ReactDOM.renderToString(
            <VictoryChartComponent
                data={data}
                currentTick={currentTick}
                showCurrentTick={false}
            />,
        )
        div.firstChild.style =
            'font-family: Roboto, Arial, sans-serif; font-size: 10pt;'
        const svgSaver = new SvgSaver()
        svgSaver.asSvg(
            div.firstChild,
            'opendc-chart-export-' + Date.now() + '.svg',
        )
    }

    return (
        <div className="mt-1" style={{ position: 'relative' }}>
            <strong>Load over time</strong>
            <VictoryChartComponent
                data={data}
                currentTick={currentTick}
                showCurrentTick={true}
            />
            <ExportChartComponent onExport={onExport}/>
        </div>
    )
}

const VictoryChartComponent = ({ data, currentTick, showCurrentTick }) => (
    <VictoryChart
        height={250}
        padding={{ top: 10, bottom: 50, left: 50, right: 50 }}
    >
        <VictoryAxis
            tickFormat={tick => convertSecondsToFormattedTime(tick)}
            fixLabelOverlap={true}
            label="Simulated Time"
        />
        <VictoryAxis dependentAxis label="Load"/>
        <VictoryLine data={data}/>
        <VictoryScatter data={data}/>
        {showCurrentTick ? (
            <VictoryLine
                labelComponent={
                    <VictoryLabel renderInPortal angle={90} dy={-5} dx={60}/>
                }
                data={[{ x: currentTick + 1, y: 0 }, { x: currentTick + 1, y: 1 }]}
                labels={point =>
                    point.y === 1
                        ? 'Current tick : ' + convertSecondsToFormattedTime(currentTick)
                        : ''}
                style={{
                    data: { stroke: '#00A6D6', strokeWidth: 4 },
                    labels: { fill: '#00A6D6' },
                }}
            />
        ) : (
            undefined
        )}
    </VictoryChart>
)

const ExportChartComponent = ({ onExport }) => (
    <button
        className="btn btn-success btn-circle btn-sm"
        title="Export Chart to PNG Image"
        onClick={onExport}
        style={{ position: 'absolute', top: 0, right: 0 }}
    >
        <span className="fa fa-camera"/>
    </button>
)

export default LoadChartComponent
