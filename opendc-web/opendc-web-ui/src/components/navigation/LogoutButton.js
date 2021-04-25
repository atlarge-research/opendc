import PropTypes from 'prop-types'
import React from 'react'
import FontAwesome from 'react-fontawesome'
import { Link } from 'react-router-dom'
import { NavLink } from 'reactstrap'

const LogoutButton = ({ onLogout }) => (
    <NavLink tag={Link} className="logout" title="Sign out" to="#" onClick={onLogout}>
        <FontAwesome name="power-off" size="lg" />
    </NavLink>
)

LogoutButton.propTypes = {
    onLogout: PropTypes.func.isRequired,
}

export default LogoutButton
