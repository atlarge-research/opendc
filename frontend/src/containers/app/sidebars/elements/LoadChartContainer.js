import { connect } from 'react-redux'
import LoadChartComponent from '../../../../components/app/sidebars/elements/LoadChartComponent'

const mapStateToProps = (state, ownProps) => {
    const data = []

    return {
        data,
        currentTick: state.currentTick,
    }
}

const LoadChartContainer = connect(mapStateToProps)(LoadChartComponent)

export default LoadChartContainer
