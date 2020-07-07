import React from 'react'
import FontAwesome from 'react-fontawesome'
import { Link } from 'react-router-dom'
import Navbar, { NavItem } from './Navbar'
import './Navbar.css'

const AppNavbar = ({ projectId, inProject, fullWidth, onViewTopologies }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/projects">
            <Link className="nav-link" title="My Projects" to="/projects">
                <FontAwesome name="list" className="mr-2"/>
                My Projects
            </Link>
        </NavItem>
        {inProject ? (
            <>
                <NavItem route={'/projects/' + projectId}>
                    <Link
                        className="nav-link"
                        title="Construction"
                        to={'/projects/' + projectId}
                    >
                        <FontAwesome name="industry" className="mr-2"/>
                        Construction
                    </Link>
                </NavItem>
                <NavItem route="topologies">
                    <span
                        className="nav-link"
                        title="Topologies"
                        onClick={onViewTopologies}
                    >
                        <FontAwesome name="server" className="mr-2"/>
                        Topologies
                    </span>
                </NavItem>
                <NavItem route={'/projects/' + projectId + '/experiments'}>
                    <Link
                        className="nav-link"
                        title="Experiments"
                        to={'/projects/' + projectId + '/experiments'}
                    >
                        <FontAwesome name="play" className="mr-2"/>
                        Experiments
                    </Link>
                </NavItem>
            </>
        ) : (
            undefined
        )}
    </Navbar>
)

export default AppNavbar
