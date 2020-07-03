import { connect } from 'react-redux'
import { deleteUnit } from '../../../../../actions/topology/machine'
import UnitComponent from '../../../../../components/app/sidebars/topology/machine/UnitComponent'

const mapStateToProps = (state, ownProps) => {
    return {
        inSimulation: state.currentExperimentId !== '-1',
        unit: state.objects[ownProps.unitType][ownProps.unitId],
    }
}

const mapDispatchToProps = (dispatch, ownProps) => {
    return {
        onDelete: () => dispatch(deleteUnit(ownProps.unitType, ownProps.index)),
    }
}

const UnitContainer = connect(mapStateToProps, mapDispatchToProps)(
    UnitComponent,
)

export default UnitContainer
