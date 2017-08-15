import React, {Component} from 'react';
import FontAwesome from "react-fontawesome";
import Mailto from "react-mailto";
import {Link} from "react-router-dom";
import Logout from "../../containers/auth/Logout";
import ProfileName from "../../containers/auth/ProfileName";
import "./Navbar.css";

class Navbar extends Component {
    render() {
        return (
            <div className="opendc-navbar">
                <Link className="opendc-brand" to="/">
                    <img src="/img/logo.png" alt="OpenDC Logo"/>
                    <div className="opendc-title">
                        Open<strong>DC</strong>
                    </div>
                </Link>
                <div className="navigation navbar-button-group">
                    <Link className="projects" title="Projects" to="/projects">Projects</Link>
                </div>
                <div className="user-controls navbar-button-group">
                    <Mailto className="support" title="Support" email="opendc.tudelft@gmail.com"
                            headers={{subject: "OpenDC Support"}}>
                        <FontAwesome name="question-circle" size="lg"/>
                    </Mailto>
                    <Link className="username" title="My Profile" to="/profile">
                        <ProfileName/>
                    </Link>
                    <Logout/>
                </div>
            </div>
        );
    }
}

export default Navbar;
