import React from 'react'
import Link from 'next/link'
import { NavLink } from 'reactstrap'
import Navbar, { NavItem } from './Navbar'
import {} from './Navbar.module.scss'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faList } from '@fortawesome/free-solid-svg-icons'

const AppNavbarComponent = ({ project, fullWidth }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/projects">
            <Link href="/projects">
                <NavLink title="My Projects">
                    <FontAwesomeIcon icon={faList} className="mr-2" />
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
