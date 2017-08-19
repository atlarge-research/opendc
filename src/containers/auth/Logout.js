import {connect} from "react-redux";
import {logOut} from "../../actions/auth";
import LogoutButton from "../../components/navigation/LogoutButton";

const mapDispatchToProps = dispatch => {
    return {
        onLogout: () => dispatch(logOut()),
    };
};

const Logout = connect(
    undefined,
    mapDispatchToProps
)(LogoutButton);

export default Logout;
