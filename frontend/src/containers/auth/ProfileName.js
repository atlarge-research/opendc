import React from 'react'
import { connect } from 'react-redux'

const mapStateToProps = state => {
    return {
        text: state.auth.givenName + ' ' + state.auth.familyName,
    }
}

const SpanElement = ({ text }) => <span>{text}</span>

const ProfileName = connect(mapStateToProps)(SpanElement)

export default ProfileName
