import React from 'react'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSpinner } from '@fortawesome/free-solid-svg-icons'

const LoadingScreen = () => (
    <div className="display-4">
        <FontAwesomeIcon icon={faSpinner} spin className="mr-4" />
        Loading your project...
    </div>
)

export default LoadingScreen
