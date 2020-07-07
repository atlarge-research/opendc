import PropTypes from 'prop-types'
import React from 'react'
import DocumentTitle from 'react-document-title'
import { connect } from 'react-redux'
import { fetchExperimentsOfProject } from '../actions/experiments'
import { openProjectSucceeded } from '../actions/projects'
import AppNavbar from '../components/navigation/AppNavbar'
import ExperimentListContainer from '../containers/experiments/ExperimentListContainer'
import NewExperimentButtonContainer from '../containers/experiments/NewExperimentButtonContainer'
import NewExperimentModal from '../containers/modals/NewExperimentModal'

class ExperimentsComponent extends React.Component {
    static propTypes = {
        projectId: PropTypes.string.isRequired,
        projectName: PropTypes.string,
    }

    componentDidMount() {
        this.props.storeProjectId(this.props.projectId)
        this.props.fetchExperimentsOfProject(this.props.projectId)
    }

    render() {
        return (
            <DocumentTitle
                title={
                    this.props.projectName
                        ? 'Experiments - ' + this.props.projectName + ' - OpenDC'
                        : 'Experiments - OpenDC'
                }
            >
                <div className="full-height">
                    <AppNavbar projectId={this.props.projectId} inProject={true} fullWidth={true}/>
                    <div className="container text-page-container full-height">
                        <ExperimentListContainer/>
                        <NewExperimentButtonContainer/>
                    </div>
                    <NewExperimentModal/>
                </div>
            </DocumentTitle>
        )
    }
}

const mapStateToProps = (state) => {
    let projectName = undefined
    if (state.currentProjectId !== '-1' && state.objects.project[state.currentProjectId]) {
        projectName = state.objects.project[state.currentProjectId].name
    }

    return {
        projectName,
    }
}

const mapDispatchToProps = (dispatch) => {
    return {
        storeProjectId: (id) => dispatch(openProjectSucceeded(id)),
        fetchExperimentsOfProject: (id) => dispatch(fetchExperimentsOfProject(id)),
    }
}

const Experiments = connect(mapStateToProps, mapDispatchToProps)(ExperimentsComponent)

export default Experiments
