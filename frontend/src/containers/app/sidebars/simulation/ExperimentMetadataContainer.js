import { connect } from 'react-redux'
import ExperimentMetadataComponent from '../../../../components/app/sidebars/simulation/ExperimentMetadataComponent'

const mapStateToProps = state => {
    if (!state.objects.experiment[state.currentExperimentId]) {
        return {
            experimentName: 'Loading experiment',
            topologyName: '',
            traceName: '',
            schedulerName: '',
        }
    }

    const topology =
        state.objects.topology[
            state.objects.experiment[state.currentExperimentId].topologyId
            ]
    const topologyName = topology.name

    return {
        experimentName: state.objects.experiment[state.currentExperimentId].name,
        topologyName,
        traceName:
        state.objects.trace[
            state.objects.experiment[state.currentExperimentId].traceId
            ].name,
        schedulerName:
        state.objects.scheduler[
            state.objects.experiment[state.currentExperimentId].schedulerName
            ].name,
    }
}

const ExperimentMetadataContainer = connect(mapStateToProps)(
    ExperimentMetadataComponent,
)

export default ExperimentMetadataContainer
