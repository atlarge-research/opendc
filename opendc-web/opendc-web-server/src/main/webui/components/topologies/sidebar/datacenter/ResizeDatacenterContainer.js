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

import PropTypes from 'prop-types'
import React from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { TextList, TextListItem, TextListItemVariants, TextListVariants } from '@patternfly/react-core'
import { resizeDatacenter } from '../../../../redux/actions/topology/datacenter'
import { NumberInput } from '@patternfly/react-core'

function isBorderColumnInUse(datacenter, rooms, tiles) {
    const borderX = (datacenter.x ?? 0) + datacenter.width - 1
    return (datacenter.rooms ?? []).some((roomId) =>
        (rooms[roomId]?.tiles ?? []).some((tileId) => tiles[tileId]?.positionX === borderX)
    )
}

function isBorderRowInUse(datacenter, rooms, tiles) {
    const borderY = (datacenter.y ?? 0) + datacenter.height - 1
    return (datacenter.rooms ?? []).some((roomId) =>
        (rooms[roomId]?.tiles ?? []).some((tileId) => tiles[tileId]?.positionY === borderY)
    )
}

function ResizeDatacenterContainer({ datacenterId }) {
    const dispatch = useDispatch()
    const datacenter = useSelector((state) => state.topology.datacenters[datacenterId])
    const rooms = useSelector((state) => state.topology.rooms)
    const tiles = useSelector((state) => state.topology.tiles)

    if (!datacenter) return null

    const { width, height } = datacenter
    const canShrinkWidth = width > 1 && !isBorderColumnInUse(datacenter, rooms, tiles)
    const canShrinkHeight = height > 1 && !isBorderRowInUse(datacenter, rooms, tiles)

    return (
        <TextList component={TextListVariants.dl}>
            <TextListItem component={TextListItemVariants.dt}>Width (tiles)</TextListItem>
            <TextListItem component={TextListItemVariants.dd}>
                <NumberInput
                    value={width}
                    min={1}
                    minusBtnProps={{ isDisabled: !canShrinkWidth, title: canShrinkWidth ? '' : 'Last column is in use by a room' }}
                    onMinus={() => canShrinkWidth && dispatch(resizeDatacenter(datacenterId, width - 1, height))}
                    onPlus={() => dispatch(resizeDatacenter(datacenterId, width + 1, height))}
                    onChange={() => {}}
                />
            </TextListItem>
            <TextListItem component={TextListItemVariants.dt}>Height (tiles)</TextListItem>
            <TextListItem component={TextListItemVariants.dd}>
                <NumberInput
                    value={height}
                    min={1}
                    minusBtnProps={{ isDisabled: !canShrinkHeight, title: canShrinkHeight ? '' : 'Last row is in use by a room' }}
                    onMinus={() => canShrinkHeight && dispatch(resizeDatacenter(datacenterId, width, height - 1))}
                    onPlus={() => dispatch(resizeDatacenter(datacenterId, width, height + 1))}
                    onChange={() => {}}
                />
            </TextListItem>
        </TextList>
    )
}

ResizeDatacenterContainer.propTypes = {
    datacenterId: PropTypes.string.isRequired,
}

export default ResizeDatacenterContainer
