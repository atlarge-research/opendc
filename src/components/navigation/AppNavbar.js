import React from 'react';
import Mailto from "react-mailto";
import {Link} from "react-router-dom";
import Navbar, {NavItem} from "./Navbar";
import "./Navbar.css";

const AppNavbar = () => (
    <Navbar>
        <NavItem route="/simulations">
            <Link className="nav-link simulations" title="Simulations" to="/simulations">Simulations</Link>
        </NavItem>
        <NavItem route="email">
            <Mailto className="nav-link support" title="Support" email="opendc@atlarge-research.com"
                    headers={{subject: "OpenDC Support"}}>
                Support
            </Mailto>
        </NavItem>

    </Navbar>
);

export default AppNavbar;
