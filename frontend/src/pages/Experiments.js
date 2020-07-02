import PropTypes from 'prop-types'
import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { fetchExperimentsOfSimulation } from '../actions/experiments'
import { openSimulationSucceeded } from '../actions/simulations'
import AppNavbar from '../components/navigation/AppNavbar'
import ExperimentListContainer from '../containers/experiments/ExperimentListContainer'
import NewExperimentButtonContainer from '../containers/experiments/NewExperimentButtonContainer'
import NewExperimentModal from '../containers/modals/NewExperimentModal'

class ExperimentsComponent extends React.Component {
    static propTypes = {
        simulationId: PropTypes.string.isRequired,
        simulationName: PropTypes.string,
    }

    componentDidMount() {
        this.props.storeSimulationId(this.props.simulationId)
        this.props.fetchExperimentsOfSimulation(this.props.simulationId)
    }

    render() {
        return (
            <DocumentTitle
                title={
                    this.props.simulationName
                        ? 'Experiments - ' + this.props.simulationName + ' - OpenDC'
                        : 'Experiments - OpenDC'
                }
            >
                <div className="full-height">
                    <AppNavbar simulationId={this.props.simulationId} inSimulation={true} fullWidth={true} />
                    <div className="container text-page-container full-height">
                        <ExperimentListContainer />
                        <NewExperimentButtonContainer />
                    </div>
                    <NewExperimentModal />
                </div>
            </DocumentTitle>
        )
    }
}

const mapStateToProps = (state) => {
    let simulationName = undefined
    if (state.currentSimulationId !== -1 && state.objects.simulation[state.currentSimulationId]) {
        simulationName = state.objects.simulation[state.currentSimulationId].name
    }

    return {
        simulationName,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        storeSimulationId: (id) => dispatch(openSimulationSucceeded(id)),
        fetchExperimentsOfSimulation: (id) => dispatch(fetchExperimentsOfSimulation(id)),
    }
}

const Experiments = connect(mapStateToProps, mapDispatchToProps)(ExperimentsComponent)

export default Experiments
