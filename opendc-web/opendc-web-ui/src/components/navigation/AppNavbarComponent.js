import PropTypes from 'prop-types'
import React from 'react'
import Link from 'next/link'
import { NavLink, NavItem as RNavItem } from 'reactstrap'
import Navbar, { NavItem } from './Navbar'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faList } from '@fortawesome/free-solid-svg-icons'
import { Project } from '../../shapes'

const AppNavbarComponent = ({ project, fullWidth }) => (
    <Navbar fullWidth={fullWidth}>
        <NavItem route="/projects">
            <Link href="/projects" passHref>
                <NavLink title="My Projects">
                    <FontAwesomeIcon icon={faList} className="mr-2" />
                    My Projects
                </NavLink>
            </Link>
        </NavItem>
        {project ? (
            <RNavItem>
                <Link href={`/projects/${project._id}`} passHref>
                    <NavLink title="Current Project">
                        <span>{project.name}</span>
                    </NavLink>
                </Link>
            </RNavItem>
        ) : undefined}
    </Navbar>
)

AppNavbarComponent.propTypes = {
    project: Project,
    fullWidth: PropTypes.bool,
}

export default AppNavbarComponent
