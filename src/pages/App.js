import PropTypes from "prop-types";
import React from 'react';
import {connect} from "react-redux";
import {openSimulationSucceeded} from "../actions/simulations";
import {fetchLatestDatacenter} from "../actions/topology";
import MapStage from "../components/map/MapStage";
import Navbar from "../components/navigation/Navbar";

class AppContainer extends React.Component {
    static propTypes = {
        simulationId: PropTypes.number.isRequired,
    };

    componentDidMount() {
        this.props.storeSimulationId(this.props.simulationId);
        this.props.fetchLatestDatacenter();
    }

    render() {
        return (
            <div className="page-container full-height">
                <Navbar/>
                <div className="full-height">
                    <MapStage/>
                </div>
            </div>
        );
    }
}

const mapDispatchToProps = dispatch => {
    return {
        storeSimulationId: id => dispatch(openSimulationSucceeded(id)),
        fetchLatestDatacenter: () => dispatch(fetchLatestDatacenter()),
    };
};

const App = connect(
    undefined,
    mapDispatchToProps
)(AppContainer);

export default App;
