import { connect } from 'react-redux'
import ExperimentListComponent from '../../components/experiments/ExperimentListComponent'

const mapStateToProps = state => {
    if (
        state.currentProjectId === '-1' ||
        !('experimentIds' in state.objects.project[state.currentProjectId])
    ) {
        return {
            loading: true,
            experimentIds: [],
        }
    }

    const experimentIds =
        state.objects.project[state.currentProjectId].experimentIds
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
