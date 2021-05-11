import React from 'react'
import FontAwesome from 'react-fontawesome'
import { Link } from 'react-router-dom'
import { NavLink } from 'reactstrap'
import Navbar, { NavItem } from './Navbar'
import {} from './Navbar.module.scss'

const AppNavbarComponent = ({ project, fullWidth }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/projects">
            <NavLink tag={Link} title="My Projects" to="/projects">
                <FontAwesome name="list" className="mr-2" />
                My Projects
            </NavLink>
        </NavItem>
        {project ? (
            <NavItem>
                <NavLink tag={Link} title="Current Project" to={`/projects/${project._id}`}>
                    <span>{project.name}</span>
                </NavLink>
            </NavItem>
        ) : undefined}
    </Navbar>
)

export default AppNavbarComponent
