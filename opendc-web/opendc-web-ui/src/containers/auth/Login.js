import React from 'react'
import { Button } from 'reactstrap'
import { useAuth } from '../../auth'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSignInAlt } from '@fortawesome/free-solid-svg-icons'

function Login({ visible, className }) {
    const { loginWithRedirect } = useAuth()

    if (!visible) {
        return <span />
    }

    return (
        <Button color="primary" onClick={() => loginWithRedirect()} className={className}>
            <FontAwesomeIcon icon={faSignInAlt} /> Sign In
        </Button>
    )
}

export default Login
