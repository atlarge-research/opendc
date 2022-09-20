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
import PropTypes from 'prop-types'
import { useDispatch, useSelector } from 'react-redux'
import { goFromRoomToRack } from '../../../redux/actions/interaction-level'
import TileGroup from './groups/TileGroup'

function TileContainer({ tileId, ...props }) {
    const interactionLevel = useSelector((state) => state.interactionLevel)
    const dispatch = useDispatch()
    const tile = useSelector((state) => state.topology.tiles[tileId])

    if (!tile) {
        return null
    }

    const onClick = (tile) => {
        if (tile.rack) {
            dispatch(goFromRoomToRack(tile.id))
        }
    }
    return <TileGroup {...props} onClick={onClick} tile={tile} interactionLevel={interactionLevel} />
}

TileContainer.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default TileContainer
