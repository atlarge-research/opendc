import React from 'react'
import { useSelector } from 'react-redux'

function ProfileName() {
    const name = useSelector((state) => `${state.auth.givenName} ${state.auth.familyName}`)
    return <span>{name}</span>
}

export default ProfileName
