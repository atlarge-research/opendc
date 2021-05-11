import React from 'react'
import GoogleLogin from 'react-google-login'
import { useDispatch } from 'react-redux'
import { logIn } from '../../actions/auth'
import { Button } from 'reactstrap'
import config from '../../config'

function Login({ visible, className }) {
    const dispatch = useDispatch()

    const onLogin = (payload) => dispatch(logIn(payload))
    const onAuthResponse = (response) => {
        onLogin({
            email: response.getBasicProfile().getEmail(),
            givenName: response.getBasicProfile().getGivenName(),
            familyName: response.getBasicProfile().getFamilyName(),
            googleId: response.googleId,
            authToken: response.getAuthResponse().id_token,
            expiresAt: response.getAuthResponse().expires_at,
        })
    }
    const onAuthFailure = (error) => {
        // TODO Show error alert
        console.error(error)
    }

    if (!visible) {
        return <span />
    }

    return (
        <GoogleLogin
            clientId={config.OAUTH_CLIENT_ID}
            onSuccess={onAuthResponse}
            onFailure={onAuthFailure}
            render={(renderProps) => (
                <Button color="primary" onClick={renderProps.onClick} className={className}>
                    <span className="fa fa-google" /> Login with Google
                </Button>
            )}
        />
    )
}

export default Login
