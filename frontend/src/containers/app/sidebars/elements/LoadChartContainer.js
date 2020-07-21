import { connect } from 'react-redux'
import MetricChartComponent from '../../../../components/app/results/MetricChartComponent'

const mapStateToProps = (state, ownProps) => {
    const data = []

    return {
        data,
        currentTick: state.currentTick,
    }
}

const LoadChartContainer = connect(mapStateToProps)(MetricChartComponent)

export default LoadChartContainer
