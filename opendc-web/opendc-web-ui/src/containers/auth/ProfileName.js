import React from 'react'
import { useUser } from '../../auth/hook'

function ProfileName() {
    const user = useUser()
    return (
        <span>
            {user.givenName} {user.familyName}
        </span>
    )
}

export default ProfileName
