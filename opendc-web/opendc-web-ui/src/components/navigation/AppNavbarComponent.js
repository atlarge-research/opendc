import React from 'react'
import FontAwesome from 'react-fontawesome'
import Link from 'next/link'
import { NavLink } from 'reactstrap'
import Navbar, { NavItem } from './Navbar'
import {} from './Navbar.module.scss'

const AppNavbarComponent = ({ project, fullWidth }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/projects">
            <Link href="/projects">
                <NavLink title="My Projects">
                    <FontAwesome name="list" className="mr-2" />
                    My Projects
                </NavLink>
            </Link>
        </NavItem>
        {project ? (
            <NavItem>
                <Link href={`/projects/${project._id}`}>
                    <NavLink title="Current Project">
                        <span>{project.name}</span>
                    </NavLink>
                </Link>
            </NavItem>
        ) : undefined}
    </Navbar>
)

export default AppNavbarComponent
