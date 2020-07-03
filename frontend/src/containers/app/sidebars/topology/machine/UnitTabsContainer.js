import { connect } from 'react-redux'
import UnitTabsComponent from '../../../../../components/app/sidebars/topology/machine/UnitTabsComponent'

const mapStateToProps = state => {
    return {
        inSimulation: state.currentExperimentId !== '-1',
    }
}

const UnitTabsContainer = connect(mapStateToProps)(UnitTabsComponent)

export default UnitTabsContainer
