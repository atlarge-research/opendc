import { connect } from 'react-redux'
import ExperimentListComponent from '../../components/experiments/ExperimentListComponent'

const mapStateToProps = state => {
    if (
        state.currentSimulationId === -1 ||
        !('experimentIds' in state.objects.simulation[state.currentSimulationId])
    ) {
        return {
            loading: true,
            experimentIds: [],
        }
    }

    const experimentIds =
        state.objects.simulation[state.currentSimulationId].experimentIds
    if (experimentIds) {
        return {
            experimentIds,
        }
    }
}

const ExperimentListContainer = connect(mapStateToProps)(
    ExperimentListComponent,
)

export default ExperimentListContainer
