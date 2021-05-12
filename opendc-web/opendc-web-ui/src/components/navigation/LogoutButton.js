import PropTypes from 'prop-types'
import React from 'react'
import FontAwesome from 'react-fontawesome'
import { NavLink } from 'reactstrap'

const LogoutButton = ({ onLogout }) => (
    <NavLink className="logout" title="Sign out" onClick={onLogout}>
        <FontAwesome name="power-off" size="lg" />
    </NavLink>
)

LogoutButton.propTypes = {
    onLogout: PropTypes.func.isRequired,
}

export default LogoutButton
