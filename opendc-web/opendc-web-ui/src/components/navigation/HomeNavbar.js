import React from 'react'
import { NavItem, NavLink } from 'reactstrap'
import Navbar from './Navbar'
import './Navbar.sass'

const ScrollNavItem = ({ id, name }) => (
    <NavItem>
        <NavLink href={id}>{name}</NavLink>
    </NavItem>
)

const HomeNavbar = () => (
    <Navbar fullWidth={false}>
        <ScrollNavItem id="#stakeholders" name="Stakeholders" />
        <ScrollNavItem id="#modeling" name="Modeling" />
        <ScrollNavItem id="#project" name="Project" />
        <ScrollNavItem id="#technologies" name="Technologies" />
        <ScrollNavItem id="#team" name="Team" />
        <ScrollNavItem id="#contact" name="Contact" />
    </Navbar>
)

export default HomeNavbar
