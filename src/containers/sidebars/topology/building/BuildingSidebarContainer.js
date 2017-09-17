import {connect} from "react-redux";
import BuildingSidebarComponent from "../../../../components/sidebars/topology/building/BuildingSidebarComponent";

const mapStateToProps = state => {
    return {
        inSimulation: state.currentExperimentId !== -1
    };
};

const BuildingSidebarContainer = connect(
    mapStateToProps
)(BuildingSidebarComponent);

export default BuildingSidebarContainer;
