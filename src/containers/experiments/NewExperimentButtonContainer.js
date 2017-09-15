import {connect} from "react-redux";
import {openNewExperimentModal} from "../../actions/modals/experiments";
import NewExperimentButtonComponent from "../../components/experiments/NewExperimentButtonComponent";

const mapDispatchToProps = dispatch => {
    return {
        onClick: () => dispatch(openNewExperimentModal())
    };
};

const NewExperimentButtonContainer = connect(
    undefined,
    mapDispatchToProps
)(NewExperimentButtonComponent);

export default NewExperimentButtonContainer;
