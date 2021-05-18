import React from 'react'
import { useAuth } from '../../auth'

function ProfileName() {
    const { isLoading, user } = useAuth()
    return isLoading ? <span>Loading...</span> : <span>{user.name}</span>
}

export default ProfileName
