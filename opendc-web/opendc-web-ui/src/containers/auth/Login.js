import React from 'react'
import { Button } from 'reactstrap'
import { useAuth } from '../../auth'

function Login({ visible, className }) {
    const { loginWithRedirect } = useAuth()

    if (!visible) {
        return <span />
    }

    return (
        <Button color="primary" onClick={() => loginWithRedirect()} className={className}>
            <span aria-hidden className="fa fa-sign-in" /> Sign In
        </Button>
    )
}

export default Login
