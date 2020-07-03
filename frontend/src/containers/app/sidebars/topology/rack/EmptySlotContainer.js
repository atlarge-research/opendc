import { connect } from 'react-redux'
import { addMachine } from '../../../../../actions/topology/rack'
import EmptySlotComponent from '../../../../../components/app/sidebars/topology/rack/EmptySlotComponent'

const mapStateToProps = state => {
    return {
        inSimulation: state.currentExperimentId !== '-1',
    }
}

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onAdd: () => dispatch(addMachine(ownProps.position)),
    }
}

const EmptySlotContainer = connect(mapStateToProps, mapDispatchToProps)(
    EmptySlotComponent,
)

export default EmptySlotContainer
