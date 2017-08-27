import classNames from "classnames";
import React from 'react';
import {Link, withRouter} from "react-router-dom";
import {userIsLoggedIn} from "../../auth/index";
import Login from "../../containers/auth/Login";
import Logout from "../../containers/auth/Logout";
import ProfileName from "../../containers/auth/ProfileName";
import "./Navbar.css";

export const NavItem = withRouter(props => <NavItemWithoutRoute {...props}/>);

const NavItemWithoutRoute = ({route, location, children}) => (
    <li className={classNames("nav-item", location.pathname === route ? "active" : undefined)}>
        {children}
    </li>
);

const Navbar = ({children}) => (
    <nav className="navbar fixed-top navbar-expand-lg navbar-light bg-faded" id="navbar">
        <div className="container">
            <button className="navbar-toggler navbar-toggler-right" type="button" data-toggle="collapse"
                    data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent"
                    aria-expanded="false" aria-label="Toggle navigation">
                <span className="navbar-toggler-icon"/>
            </button>
            <Link className="navbar-brand opendc-brand" to="/">
                <img src="/img/logo.png" alt="OpenDC Logo"/>
            </Link>

            <div className="collapse navbar-collapse" id="navbarSupportedContent">
                <ul className="navbar-nav mr-auto">
                    {children}
                </ul>
                <ul className="navbar-nav">
                    {userIsLoggedIn() ?
                        [
                            <NavItem route="/profile">
                                <Link className="username nav-link" title="My Profile" to="/profile">
                                    <ProfileName/>
                                </Link>
                            </NavItem>,
                            <NavItem route="logout">
                                <Logout/>
                            </NavItem>
                        ] :
                        <NavItem route="login">
                            <Login visible={true}/>
                        </NavItem>
                    }
                </ul>
            </div>
        </div>
    </nav>
);

export default Navbar;
