import React from 'react';
import FontAwesome from "react-fontawesome";
import Mailto from "react-mailto";
import {Link} from "react-router-dom";
import Navbar, {NavItem} from "./Navbar";
import "./Navbar.css";

const AppNavbar = ({simulationId, inSimulation, fullWidth}) => (
    <Navbar fullWidth={fullWidth}>
        {inSimulation ?
            <NavItem route={"/simulations/" + simulationId}>
                <Link
                    className="nav-link"
                    title="Construction"
                    to={"/simulations/" + simulationId}>
                    <FontAwesome name="industry" className="mr-2"/>
                    Construction
                </Link>
            </NavItem> :
            undefined
        }
        {inSimulation ?
            <NavItem route={"/simulations/" + simulationId + "/experiments"}>
                <Link
                    className="nav-link"
                    title="Experiments"
                    to={"/simulations/" + simulationId + "/experiments"}>
                    <FontAwesome name="play" className="mr-2"/>
                    Experiments
                </Link>
            </NavItem> :
            undefined
        }
        <NavItem route="/simulations">
            <Link
                className="nav-link"
                title="My Simulations"
                to="/simulations">
                <FontAwesome name="list" className="mr-2"/>
                My Simulations
            </Link>
        </NavItem>
        <NavItem route="email">
            <Mailto className="nav-link" title="Support" email="opendc@atlarge-research.com"
                    headers={{subject: "[support]"}}>
                <FontAwesome name="envelope" className="mr-2"/>
                Support
            </Mailto>
        </NavItem>
    </Navbar>
);

export default AppNavbar;
