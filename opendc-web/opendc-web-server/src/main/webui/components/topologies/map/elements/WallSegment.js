import React from 'react'
import { Line } from 'react-konva'
import { WallSegment as WallSegmentShape } from '../../../../shapes'
import { WALL_COLOR } from '../../../../util/colors'
import { TILE_SIZE_IN_PIXELS, WALL_WIDTH_IN_PIXELS } from '../MapConstants'

function WallSegment({ wallSegment }) {
    let points
    if (wallSegment.isHorizontal) {
        points = [
            wallSegment.startPosX * TILE_SIZE_IN_PIXELS,
            wallSegment.startPosY * TILE_SIZE_IN_PIXELS,
            (wallSegment.startPosX + wallSegment.length) * TILE_SIZE_IN_PIXELS,
            wallSegment.startPosY * TILE_SIZE_IN_PIXELS,
        ]
    } else {
        points = [
            wallSegment.startPosX * TILE_SIZE_IN_PIXELS,
            wallSegment.startPosY * TILE_SIZE_IN_PIXELS,
            wallSegment.startPosX * TILE_SIZE_IN_PIXELS,
            (wallSegment.startPosY + wallSegment.length) * TILE_SIZE_IN_PIXELS,
        ]
    }

    return <Line points={points} lineCap="round" stroke={WALL_COLOR} strokeWidth={WALL_WIDTH_IN_PIXELS} />
}

WallSegment.propTypes = {
    wallSegment: WallSegmentShape,
}

export default WallSegment
