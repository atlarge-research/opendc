import React, {Component} from 'react';
import {Link} from "react-router-dom";
import "./Navbar.css";
import Mailto from "react-mailto";
import FontAwesome from "react-fontawesome";

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
                            headers={{subject: "OpenDC%20Support"}}>
                        <FontAwesome name="question-circle" size="lg"/>
                    </Mailto>
                    <Link className="username" title="My Profile" to="/profile">Profile</Link>
                    <Link className="sign-out" title="Sign out" to="#">
                        <FontAwesome name="power-off" size="lg"/>
                    </Link>
                </div>
                <div id="google-signin" className="navbar-right"/>
            </div>
        );
    }
}

export default Navbar;
