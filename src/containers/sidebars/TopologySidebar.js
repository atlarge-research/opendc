import {connect} from "react-redux";
import TopologySidebarComponent from "../../components/sidebars/TopologySidebarComponent";

const mapStateToProps = state => {
    return {
        interactionLevel: state.interactionLevel
    };
};

const TopologySidebar = connect(
    mapStateToProps
)(TopologySidebarComponent);

export default TopologySidebar;
