import {connect} from "react-redux";
import BuildingSidebarComponent from "../../../../components/sidebars/topology/building/BuildingSidebarComponent";

const mapStateToProps = state => {
    return {
        currentRoomInConstruction: state.currentRoomInConstruction
    };
};

const BuildingSidebarContainer = connect(
    mapStateToProps
)(BuildingSidebarComponent);

export default BuildingSidebarContainer;
