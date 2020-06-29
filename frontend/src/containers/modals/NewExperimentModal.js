import { connect } from "react-redux";
import { addExperiment } from "../../actions/experiments";
import { closeNewExperimentModal } from "../../actions/modals/experiments";
import NewExperimentModalComponent from "../../components/modals/custom-components/NewExperimentModalComponent";

const mapStateToProps = state => {
  return {
    show: state.modals.newExperimentModalVisible,
    paths: Object.values(state.objects.path).filter(
      path => path.simulationId === state.currentSimulationId
    ),
    traces: Object.values(state.objects.trace),
    schedulers: Object.values(state.objects.scheduler)
  };
};

const mapDispatchToProps = dispatch => {
  return {
    callback: (name, pathId, traceId, schedulerName) => {
      if (name) {
        dispatch(
          addExperiment({
            name,
            pathId,
            traceId,
            schedulerName
          })
        );
      }
      dispatch(closeNewExperimentModal());
    }
  };
};

const NewExperimentModal = connect(mapStateToProps, mapDispatchToProps)(
  NewExperimentModalComponent
);

export default NewExperimentModal;
