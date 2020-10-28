import React from 'react'
import { useSelector } from 'react-redux'
import ScaleIndicatorComponent from '../../../../components/app/map/controls/ScaleIndicatorComponent'

const ScaleIndicatorContainer = (props) => {
    const scale = useSelector((state) => state.map.scale)
    return <ScaleIndicatorComponent {...props} scale={scale} />
}

export default ScaleIndicatorContainer
