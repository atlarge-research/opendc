import React from 'react'
import { NavItem, NavLink } from 'reactstrap'
import Navbar from './Navbar'

const HomeNavbar = () => (
    <Navbar fullWidth={false}>
        <NavLink href="#stakeholders">Stakeholders</NavLink>
        <NavLink href="#modeling">Modeling</NavLink>
        <NavLink href="#project">Project</NavLink>
        <NavLink href="#technologies">Technologies</NavLink>
        <NavLink href="#team">Team</NavLink>
        <NavLink href="#contact">Contact</NavLink>
    </Navbar>
)

export default HomeNavbar
