import {connect} from "react-redux";
import DatacenterGroup from "../../components/map/groups/DatacenterGroup";

const mapStateToProps = state => {
    if (state.currentDatacenterId === -1) {
        return {};
    }

    return {
        datacenter: state.objects.datacenter[state.currentDatacenterId],
        interactionLevel: state.interactionLevel
    };
};

const DatacenterContainer = connect(
    mapStateToProps
)(DatacenterGroup);

export default DatacenterContainer;
