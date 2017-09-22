import {connect} from "react-redux";
import {pauseSimulation, playSimulation} from "../../../actions/simulation/playback";
import PlayButtonComponent from "../../../components/app/timeline/PlayButtonComponent";

const mapStateToProps = state => {
    return {
        isPlaying: state.isPlaying,
    };
};

const mapDispatchToProps = dispatch => {
    return {
        onPlay: () => dispatch(playSimulation()),
        onPause: () => dispatch(pauseSimulation()),
    };
};

const PlayButtonContainer = connect(
    mapStateToProps,
    mapDispatchToProps
)(PlayButtonComponent);

export default PlayButtonContainer;
