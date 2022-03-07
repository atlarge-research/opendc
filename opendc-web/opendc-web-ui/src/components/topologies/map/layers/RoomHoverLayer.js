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
import { toggleTileAtLocation } from '../../../../redux/actions/topology/building'
import {
    deriveValidNextTilePositions,
    findPositionInPositions,
    findPositionInRooms,
} from '../../../../util/tile-calculations'
import HoverLayerComponent from './HoverLayerComponent'

function RoomHoverLayer() {
    const dispatch = useDispatch()
    const onClick = (x, y) => dispatch(toggleTileAtLocation(x, y))
    const isEnabled = useSelector((state) => state.construction.currentRoomInConstruction !== '-1')
    const isValid = useSelector((state) => (x, y) => {
        const newRoom = { ...state.topology.rooms[state.construction.currentRoomInConstruction] }
        const oldRooms = Object.keys(state.topology.rooms)
            .map((id) => ({ ...state.topology.rooms[id] }))
            .filter(
                (room) =>
                    state.topology.root.rooms.indexOf(room.id) !== -1 &&
                    room.id !== state.construction.currentRoomInConstruction
            )

        ;[...oldRooms, newRoom].forEach((room) => {
            room.tiles = room.tiles.map((tileId) => state.topology.tiles[tileId])
        })
        if (newRoom.tiles.length === 0) {
            return findPositionInRooms(oldRooms, x, y) === -1
        }

        const validNextPositions = deriveValidNextTilePositions(oldRooms, newRoom.tiles)
        return findPositionInPositions(validNextPositions, x, y) !== -1
    })

    return <HoverLayerComponent onClick={onClick} isEnabled={isEnabled} isValid={isValid} />
}

export default RoomHoverLayer
