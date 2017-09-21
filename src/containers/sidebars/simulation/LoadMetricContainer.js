import {connect} from "react-redux";
import LoadMetricComponent from "../../../components/sidebars/simulation/LoadMetricComponent";

const mapStateToProps = state => {
    return {
        loadMetric: state.loadMetric,
    }
};

const LoadMetricContainer = connect(
    mapStateToProps
)(LoadMetricComponent);

export default LoadMetricContainer;
