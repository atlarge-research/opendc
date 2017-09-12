import React from 'react';
import FontAwesome from "react-fontawesome";
import Mailto from "react-mailto";
import {Link} from "react-router-dom";
import Navbar, {NavItem} from "./Navbar";
import "./Navbar.css";

const AppNavbar = ({simulationId, inSimulation}) => (
    <Navbar>
        {inSimulation ?
            <NavItem route={"/simulations/" + simulationId}>
                <Link
                    className="nav-link"
                    title="Build"
                    to={"/simulations/" + simulationId}>
                    <FontAwesome name="industry" className="mr-1"/>
                    Build
                </Link>
            </NavItem> :
            undefined
        }
        {inSimulation ?
            <NavItem route={"/simulations/" + simulationId + "/experiments"}>
                <Link
                    className="nav-link"
                    title="Simulate"
                    to={"/simulations/" + simulationId + "/experiments"}>
                    <FontAwesome name="play" className="mr-1"/>
                    Simulate
                </Link>
            </NavItem> :
            undefined
        }
        <NavItem route="/simulations">
            <Link
                className="nav-link"
                title="My Simulations"
                to="/simulations">
                <FontAwesome name="list" className="mr-1"/>
                My Simulations
            </Link>
        </NavItem>
        <NavItem route="email">
            <Mailto className="nav-link" title="Support" email="opendc@atlarge-research.com"
                    headers={{subject: "OpenDC Support"}}>
                <FontAwesome name="envelope" className="mr-1"/>
                Support
            </Mailto>
        </NavItem>
    </Navbar>
);

export default AppNavbar;
