import React from 'react'
import { useDispatch } from 'react-redux'
import { zoomInOnCenter } from '../../../../actions/map'
import ZoomControlComponent from '../../../../components/app/map/controls/ZoomControlComponent'
import { useMapScale } from '../../../../store/hooks/map'

const ZoomControlContainer = () => {
    const dispatch = useDispatch()
    const scale = useMapScale()
    return <ZoomControlComponent mapScale={scale} zoomInOnCenter={(zoomIn) => dispatch(zoomInOnCenter(zoomIn))} />
}

export default ZoomControlContainer
