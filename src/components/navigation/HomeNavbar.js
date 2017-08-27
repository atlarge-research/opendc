import React from 'react';
import Navbar from "./Navbar";
import "./Navbar.css";

const ScrollNavItem = ({id, name}) => (
    <li className="nav-item">
        <a className="nav-link" href={id}>{name}</a>
    </li>
);

const HomeNavbar = () => (
    <Navbar>
        <ScrollNavItem id="#stakeholders" name="Stakeholders"/>
        <ScrollNavItem id="#modeling" name="Modeling"/>
        <ScrollNavItem id="#simulation" name="Simulation"/>
        <ScrollNavItem id="#technologies" name="Technologies"/>
        <ScrollNavItem id="#team" name="Team"/>
        <ScrollNavItem id="#contact" name="Contact"/>
    </Navbar>
);

export default HomeNavbar;
