import PropTypes from 'prop-types'
import React from 'react'
import FontAwesome from 'react-fontawesome'
import { Link } from 'react-router-dom'

const LogoutButton = ({ onLogout }) => (
    <Link className="logout nav-link" title="Sign out" to="#" onClick={onLogout}>
        <FontAwesome name="power-off" size="lg" />
    </Link>
)

LogoutButton.propTypes = {
    onLogout: PropTypes.func.isRequired,
}

export default LogoutButton
