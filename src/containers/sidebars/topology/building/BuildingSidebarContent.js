import {connect} from "react-redux";
import BuildingSidebarContentComponent from "../../../../components/sidebars/topology/building/BuildingSidebarContentComponent";

const mapStateToProps = state => {
    return {
        currentRoomInConstruction: state.currentRoomInConstruction
    };
};

const BuildingSidebarContent = connect(
    mapStateToProps
)(BuildingSidebarContentComponent);

export default BuildingSidebarContent;
