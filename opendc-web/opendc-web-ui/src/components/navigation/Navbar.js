import React, { useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
    Navbar as RNavbar,
    NavItem as RNavItem,
    NavLink,
    NavbarBrand,
    NavbarToggler,
    Collapse,
    Nav,
    Container,
} from 'reactstrap'
import { userIsLoggedIn } from '../../auth/index'
import Login from '../../containers/auth/Login'
import Logout from '../../containers/auth/Logout'
import ProfileName from '../../containers/auth/ProfileName'
import { login, navbar, opendcBrand } from './Navbar.module.scss'

export const NAVBAR_HEIGHT = 60

const GitHubLink = () => (
    <a
        href="https://github.com/atlarge-research/opendc"
        className="ml-2 mr-3 text-dark"
        style={{ position: 'relative', top: 7 }}
    >
        <span className="fa fa-github fa-2x" />
    </a>
)

export const NavItem = ({ route, children }) => {
    const location = useLocation()
    return <RNavItem active={location.pathname === route}>{children}</RNavItem>
}

export const LoggedInSection = () => {
    const location = useLocation()
    return (
        <Nav navbar className="auth-links">
            {userIsLoggedIn() ? (
                [
                    location.pathname === '/' ? (
                        <NavItem route="/projects" key="projects">
                            <NavLink tag={Link} title="My Projects" to="/projects">
                                My Projects
                            </NavLink>
                        </NavItem>
                    ) : (
                        <NavItem route="/profile" key="profile">
                            <NavLink tag={Link} title="My Profile" to="/profile">
                                <ProfileName />
                            </NavLink>
                        </NavItem>
                    ),
                    <NavItem route="logout" key="logout">
                        <Logout />
                    </NavItem>,
                ]
            ) : (
                <NavItem route="login">
                    <GitHubLink />
                    <Login visible={true} className={login} />
                </NavItem>
            )}
        </Nav>
    )
}

const Navbar = ({ fullWidth, children }) => {
    const [isOpen, setIsOpen] = useState(false)
    const toggle = () => setIsOpen(!isOpen)

    return (
        <RNavbar fixed="top" color="light" light expand="lg" id="navbar" className={navbar}>
            <Container fluid={fullWidth}>
                <NavbarToggler onClick={toggle} />
                <NavbarBrand tag={Link} to="/" title="OpenDC" className={opendcBrand}>
                    <img src="/img/logo.png" alt="OpenDC" />
                </NavbarBrand>

                <Collapse isOpen={isOpen} navbar>
                    <Nav className="mr-auto" navbar>
                        {children}
                    </Nav>
                    <LoggedInSection />
                </Collapse>
            </Container>
        </RNavbar>
    )
}

export default Navbar
