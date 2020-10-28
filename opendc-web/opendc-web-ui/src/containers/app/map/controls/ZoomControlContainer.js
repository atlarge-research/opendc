import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { zoomInOnCenter } from '../../../../actions/map'
import ZoomControlComponent from '../../../../components/app/map/controls/ZoomControlComponent'

const ZoomControlContainer = () => {
    const dispatch = useDispatch()
    const scale = useSelector((state) => state.map.scale)
    return <ZoomControlComponent mapScale={scale} zoomInOnCenter={(zoomIn) => dispatch(zoomInOnCenter(zoomIn))} />
}

export default ZoomControlContainer
