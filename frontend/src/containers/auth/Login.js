import PropTypes from "prop-types";
import React from "react";
import GoogleLogin from "react-google-login";
import { connect } from "react-redux";
import { logIn } from "../../actions/auth";

class LoginContainer extends React.Component {
  static propTypes = {
    visible: PropTypes.bool.isRequired,
    onLogin: PropTypes.func.isRequired
  };

  onAuthResponse(response) {
    this.props.onLogin({
      email: response.getBasicProfile().getEmail(),
      givenName: response.getBasicProfile().getGivenName(),
      familyName: response.getBasicProfile().getFamilyName(),
      googleId: response.googleId,
      authToken: response.getAuthResponse().id_token,
      expiresAt: response.getAuthResponse().expires_at
    });
  }

  onAuthFailure(error) {
    console.error(error);
  }

  render() {
    if (!this.props.visible) {
      return <span />;
    }

    return (
      <GoogleLogin
        clientId={process.env.REACT_APP_OAUTH_CLIENT_ID}
        onSuccess={this.onAuthResponse.bind(this)}
        onFailure={this.onAuthFailure.bind(this)}
        render={renderProps => (
          <span onClick={renderProps.onClick} className="login btn btn-primary">
            <span className="fa fa-google" /> Login with Google
          </span>
        )}
      />
    );
  }
}

const mapStateToProps = (state, ownProps) => {
  return {
    visible: ownProps.visible
  };
};

const mapDispatchToProps = dispatch => {
  return {
    onLogin: payload => dispatch(logIn(payload))
  };
};

const Login = connect(
  mapStateToProps,
  mapDispatchToProps
)(LoginContainer);

export default Login;
