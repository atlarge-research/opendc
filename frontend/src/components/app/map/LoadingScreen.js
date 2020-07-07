import React from 'react'
import FontAwesome from 'react-fontawesome'

const LoadingScreen = () => (
    <div className="display-4">
        <FontAwesome name="refresh" className="mr-4" spin/>
        Loading your project...
    </div>
)

export default LoadingScreen
