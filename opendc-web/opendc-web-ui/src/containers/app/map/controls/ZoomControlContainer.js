import React from 'react'
import { useDispatch } from 'react-redux'
import { zoomInOnCenter } from '../../../../redux/actions/map'
import ZoomControlComponent from '../../../../components/app/map/controls/ZoomControlComponent'
import { useMapScale } from '../../../../data/map'

const ZoomControlContainer = () => {
    const dispatch = useDispatch()
    const scale = useMapScale()
    return <ZoomControlComponent mapScale={scale} zoomInOnCenter={(zoomIn) => dispatch(zoomInOnCenter(zoomIn))} />
}

export default ZoomControlContainer
