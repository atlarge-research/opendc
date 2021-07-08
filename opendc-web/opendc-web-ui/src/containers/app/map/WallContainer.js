import React from 'react'
import { useSelector } from 'react-redux'
import WallGroup from '../../../components/app/map/groups/WallGroup'

const WallContainer = (props) => {
    const tiles = useSelector((state) =>
        state.objects.room[props.roomId].tiles.map((tileId) => state.objects.tile[tileId])
    )
    return <WallGroup {...props} tiles={tiles} />
}

export default WallContainer
