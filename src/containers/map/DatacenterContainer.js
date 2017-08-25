import {connect} from "react-redux";
import DatacenterGroup from "../../components/map/groups/DatacenterGroup";
import {denormalize} from "../../store/denormalizer";

const mapStateToProps = state => {
    if (state.currentDatacenterId === -1) {
        return {};
    }

    const datacenter = denormalize(state, "datacenter", state.currentDatacenterId);
    console.log(datacenter);

    return {
        datacenter
    };
};

const DatacenterContainer = connect(
    mapStateToProps
)(DatacenterGroup);

export default DatacenterContainer;
