import PropTypes from 'prop-types'
import React from 'react'
import { NavLink } from 'reactstrap'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSignOutAlt } from '@fortawesome/free-solid-svg-icons'

const LogoutButton = ({ onLogout }) => (
    <NavLink className="logout" title="Sign out" onClick={onLogout}>
        <FontAwesomeIcon icon={faSignOutAlt} size="lg" />
    </NavLink>
)

LogoutButton.propTypes = {
    onLogout: PropTypes.func.isRequired,
}

export default LogoutButton
