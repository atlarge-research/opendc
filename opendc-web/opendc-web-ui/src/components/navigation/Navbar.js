import React, { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/router'
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
import Login from '../../containers/auth/Login'
import Logout from '../../containers/auth/Logout'
import ProfileName from '../../containers/auth/ProfileName'
import { login, navbar, opendcBrand } from './Navbar.module.scss'
import { useAuth } from '../../auth'

export const NAVBAR_HEIGHT = 60

const GitHubLink = () => (
    <a
        href="https://github.com/atlarge-research/opendc"
        className="ml-2 mr-3 text-dark"
        style={{ position: 'relative', top: 7 }}
    >
        <span aria-hidden className="fa fa-github fa-2x" />
    </a>
)

export const NavItem = ({ route, children }) => {
    const router = useRouter()
    const handleClick = (e) => {
        e.preventDefault()
        router.push(route)
    }
    return (
        <RNavItem onClick={handleClick} active={router.asPath === route}>
            {children}
        </RNavItem>
    )
}

export const LoggedInSection = () => {
    const router = useRouter()
    const { isAuthenticated } = useAuth()
    return (
        <Nav navbar className="auth-links">
            {isAuthenticated ? (
                [
                    router.asPath === '/' ? (
                        <NavItem route="/projects" key="projects">
                            <Link href="/projects">
                                <NavLink title="My Projects" to="/projects">
                                    My Projects
                                </NavLink>
                            </Link>
                        </NavItem>
                    ) : (
                        <NavItem key="profile">
                            <NavLink title="My Profile">
                                <ProfileName />
                            </NavLink>
                        </NavItem>
                    ),
                    <NavItem route="logout" key="logout">
                        <Logout />
                    </NavItem>,
                ]
            ) : (
                <RNavItem>
                    <GitHubLink />
                    <Login visible={true} className={login} />
                </RNavItem>
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
                <NavbarBrand href="/" title="OpenDC" className={opendcBrand}>
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
