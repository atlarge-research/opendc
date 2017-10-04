import classNames from "classnames";
import React from "react";
import { Link, withRouter } from "react-router-dom";
import { userIsLoggedIn } from "../../auth/index";
import Login from "../../containers/auth/Login";
import Logout from "../../containers/auth/Logout";
import ProfileName from "../../containers/auth/ProfileName";
import "./Navbar.css";

export const NAVBAR_HEIGHT = 60;

export const NavItem = withRouter(props => <NavItemWithoutRoute {...props} />);
export const LoggedInSection = withRouter(props => (
  <LoggedInSectionWithoutRoute {...props} />
));

const NavItemWithoutRoute = ({ route, location, children }) => (
  <li
    className={classNames(
      "nav-item",
      location.pathname === route ? "active" : undefined
    )}
  >
    {children}
  </li>
);

const LoggedInSectionWithoutRoute = ({ location }) => (
  <ul className="navbar-nav auth-links">
    {userIsLoggedIn() ? (
      [
        location.pathname === "/" ? (
          <NavItem route="/simulations" key="simulations">
            <Link className="nav-link" title="My Simulations" to="/simulations">
              My Simulations
            </Link>
          </NavItem>
        ) : (
          <NavItem route="/profile" key="profile">
            <Link className="nav-link" title="My Profile" to="/profile">
              <ProfileName />
            </Link>
          </NavItem>
        ),
        <NavItem route="logout" key="logout">
          <Logout />
        </NavItem>
      ]
    ) : (
      <NavItem route="login">
        <Login visible={true} />
      </NavItem>
    )}
  </ul>
);

const Navbar = ({ fullWidth, children }) => (
  <nav
    className="navbar fixed-top navbar-expand-lg navbar-light bg-faded"
    id="navbar"
  >
    <div className={fullWidth ? "container-fluid" : "container"}>
      <button
        className="navbar-toggler navbar-toggler-right"
        type="button"
        data-toggle="collapse"
        data-target="#navbarSupportedContent"
        aria-controls="navbarSupportedContent"
        aria-expanded="false"
        aria-label="Toggle navigation"
      >
        <span className="navbar-toggler-icon" />
      </button>
      <Link
        className="navbar-brand opendc-brand"
        to="/"
        title="OpenDC"
        onClick={() => window.scrollTo(0, 0)}
      >
        <img src="/img/logo.png" alt="OpenDC" />
      </Link>

      <div className="collapse navbar-collapse" id="navbarSupportedContent">
        <ul className="navbar-nav mr-auto">{children}</ul>
        <LoggedInSection />
      </div>
    </div>
  </nav>
);

export default Navbar;
