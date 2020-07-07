import React from 'react'
import FontAwesome from 'react-fontawesome'
import { Link } from 'react-router-dom'
import Navbar, { NavItem } from './Navbar'
import './Navbar.css'

const AppNavbarComponent = ({ project, fullWidth }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/projects">
            <Link className="nav-link" title="My Projects" to="/projects">
                <FontAwesome name="list" className="mr-2"/>
                My Projects
            </Link>
        </NavItem>
        {project ? (
            <NavItem>
                <Link className="nav-link" title="Current Project" to={`/projects/${project._id}`}>
                    <span>{project.name}</span>
                </Link>
            </NavItem>
        ) : (
            undefined
        )}
    </Navbar>
)

export default AppNavbarComponent
