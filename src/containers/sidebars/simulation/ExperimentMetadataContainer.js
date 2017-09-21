import {connect} from "react-redux";
import ExperimentMetadataComponent from "../../../components/sidebars/simulation/ExperimentMetadataComponent";

const mapStateToProps = state => {
    if (!state.objects.experiment[state.currentExperimentId]) {
        return {
            experimentName: "Loading experiment",
            pathName: "",
            traceName: "",
            schedulerName: "",
        }
    }

    const path = state.objects.path[state.objects.experiment[state.currentExperimentId].pathId];
    const pathName = path.name ? path.name : "Path " + path.id;

    return {
        experimentName: state.objects.experiment[state.currentExperimentId].name,
        pathName,
        traceName: state.objects.trace[state.objects.experiment[state.currentExperimentId].traceId].name,
        schedulerName: state.objects.scheduler[state.objects.experiment[state.currentExperimentId].schedulerName].name,
    };
};

const ExperimentMetadataContainer = connect(
    mapStateToProps
)(ExperimentMetadataComponent);

export default ExperimentMetadataContainer;
