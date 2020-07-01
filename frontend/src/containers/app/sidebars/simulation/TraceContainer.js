import { connect } from 'react-redux'
import TraceComponent from '../../../../components/app/sidebars/simulation/TraceComponent'

const mapStateToProps = state => {
    if (
        !state.objects.experiment[state.currentExperimentId] ||
        !state.objects.trace[
            state.objects.experiment[state.currentExperimentId].traceId
            ].jobIds
    ) {
        return {
            jobs: [],
        }
    }

    return {
        jobs: state.objects.trace[
            state.objects.experiment[state.currentExperimentId].traceId
            ].jobIds.map(id => state.objects.job[id]),
    }
}

const TraceContainer = connect(mapStateToProps)(TraceComponent)

export default TraceContainer
