/*
 * Copyright (c) 2021 AtLarge Research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { addRackToTile } from '../../../../redux/actions/topology/room'
import { findTileWithPosition } from '../../../../util/tile-calculations'
import HoverLayerComponent from './HoverLayerComponent'
import TilePlusIcon from '../elements/TilePlusIcon'

function ObjectHoverLayer() {
    const isEnabled = useSelector((state) => state.construction.inRackConstructionMode)
    const isValid = useSelector((state) => (x, y) => {
        if (state.interactionLevel.mode !== 'ROOM') {
            return false
        }

        const currentRoom = state.topology.rooms[state.interactionLevel.roomId]
        const tiles = currentRoom.tiles.map((tileId) => state.topology.tiles[tileId])
        const tile = findTileWithPosition(tiles, x, y)

        return !(tile === null || tile.rack)
    })

    const dispatch = useDispatch()
    const onClick = (x, y) => dispatch(addRackToTile(x, y))
    return (
        <HoverLayerComponent onClick={onClick} isEnabled={isEnabled} isValid={isValid}>
            <TilePlusIcon />
        </HoverLayerComponent>
    )
}

export default ObjectHoverLayer
