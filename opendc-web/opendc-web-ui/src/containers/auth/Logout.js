import React from 'react'
import { useDispatch } from 'react-redux'
import { logOut } from '../../actions/auth'
import LogoutButton from '../../components/navigation/LogoutButton'

const Logout = (props) => {
    const dispatch = useDispatch()
    return <LogoutButton {...props} onLogout={() => dispatch(logOut())} />
}

export default Logout
