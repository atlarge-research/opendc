import PropTypes from "prop-types";
import React from 'react';
import {connect} from "react-redux";
import {openSimulationSucceeded} from "../actions/simulations";
import {fetchLatestDatacenter} from "../actions/topology";
import MapStage from "../components/map/MapStage";
import Navbar from "../components/navigation/Navbar";
import Login from "../containers/auth/Login";

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
            <div className="full-height">
                <Navbar/>
                <div className="full-height">
                    <MapStage/>
                </div>
                <Login visible={false}/>
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
