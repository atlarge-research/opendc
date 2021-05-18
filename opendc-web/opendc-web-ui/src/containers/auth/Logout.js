import React from 'react'
import LogoutButton from '../../components/navigation/LogoutButton'
import { useAuth } from '../../auth'

const Logout = (props) => {
    const { logout } = useAuth()
    return <LogoutButton {...props} onLogout={() => logout({ returnTo: window.location.origin })} />
}

export default Logout
