import PropTypes from "prop-types";
import React from "react";
import {Link} from "react-router-dom";
import "./LogoutButton.css";

const LogoutButton = ({onLogout}) => (
    <Link className="logout" title="Sign out" to="#" onClick={onLogout}>
        <span className="fa fa-lg fa-power-off"/>
    </Link>
);

LogoutButton.propTypes = {
    onLogout: PropTypes.func.isRequired,
};

export default LogoutButton;
