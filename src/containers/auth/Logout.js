import {connect} from "react-redux";
import {logOut} from "../../actions/auth";
import LogoutButton from "../../components/navigation/LogoutButton";

const mapStateToProps = state => {
    return {};
};

const mapDispatchToProps = dispatch => {
    return {
        onLogout: () => dispatch(logOut()),
    };
};

const Logout = connect(
    mapStateToProps,
    mapDispatchToProps
)(LogoutButton);

export default Logout;
