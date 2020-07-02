import React from 'react'
import FontAwesome from 'react-fontawesome'
import { Link } from 'react-router-dom'
import Navbar, { NavItem } from './Navbar'
import './Navbar.css'

const AppNavbar = ({ simulationId, inSimulation, fullWidth, onViewTopologies }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/simulations">
            <Link className="nav-link" title="My Simulations" to="/simulations">
                <FontAwesome name="list" className="mr-2"/>
                My Simulations
            </Link>
        </NavItem>
        {inSimulation ? (
            <>
                <NavItem route={'/simulations/' + simulationId}>
                    <Link
                        className="nav-link"
                        title="Construction"
                        to={'/simulations/' + simulationId}
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
                        <FontAwesome name="home" className="mr-2"/>
                        Topologies
                    </span>
                </NavItem>
                <NavItem route={'/simulations/' + simulationId + '/experiments'}>
                    <Link
                        className="nav-link"
                        title="Experiments"
                        to={'/simulations/' + simulationId + '/experiments'}
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
