import React from 'react'
import Navbar from './Navbar'
import './Navbar.css'

const ScrollNavItem = ({ id, name }) => (
    <li className="nav-item">
        <a className="nav-link" href={id}>
            {name}
        </a>
    </li>
)

const HomeNavbar = () => (
    <Navbar fullWidth={false}>
        <ScrollNavItem id="#stakeholders" name="Stakeholders"/>
        <ScrollNavItem id="#modeling" name="Modeling"/>
        <ScrollNavItem id="#project" name="Project"/>
        <ScrollNavItem id="#technologies" name="Technologies"/>
        <ScrollNavItem id="#team" name="Team"/>
        <ScrollNavItem id="#contact" name="Contact"/>
    </Navbar>
)

export default HomeNavbar
