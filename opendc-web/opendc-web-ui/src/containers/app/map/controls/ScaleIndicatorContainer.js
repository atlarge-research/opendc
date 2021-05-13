import React from 'react'
import ScaleIndicatorComponent from '../../../../components/app/map/controls/ScaleIndicatorComponent'
import { useMapScale } from '../../../../store/hooks/map'

const ScaleIndicatorContainer = (props) => {
    const scale = useMapScale()
    return <ScaleIndicatorComponent {...props} scale={scale} />
}

export default ScaleIndicatorContainer
