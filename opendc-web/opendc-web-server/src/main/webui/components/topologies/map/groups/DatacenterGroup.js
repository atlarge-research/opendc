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
import { Group } from 'react-konva'
import { Datacenter, InteractionLevel } from '../../../../shapes'
import RoomContainer from '../RoomContainer'
import GrayContainer from '../GrayContainer'

function DatacenterGroup({ datacenter, interactionLevel, onClick }) {
    if (!datacenter || !datacenter.rooms) {
        return <Group onClick={onClick} />
    }

    if (interactionLevel.mode === 'BUILDING' || interactionLevel.datacenterId !== datacenter.id) {
        return (
            <Group onClick={onClick}>
                {datacenter.rooms.map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            </Group>
        )
    }

    // This datacenter is the focused one — render rooms with graying for ROOM/RACK/MACHINE modes
    const focusedRoomId = interactionLevel.roomId

    if (!focusedRoomId) {
        // DATACENTER mode: show all rooms in this datacenter
        return (
            <Group onClick={onClick}>
                {datacenter.rooms.map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            </Group>
        )
    }

    return (
        <Group onClick={onClick}>
            {datacenter.rooms
                .filter((roomId) => roomId !== focusedRoomId)
                .map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
            {interactionLevel.mode === 'ROOM' ? <GrayContainer key="gray" /> : null}
            {datacenter.rooms
                .filter((roomId) => roomId === focusedRoomId)
                .map((roomId) => (
                    <RoomContainer key={roomId} roomId={roomId} />
                ))}
        </Group>
    )
}

DatacenterGroup.propTypes = {
    datacenter: Datacenter,
    interactionLevel: InteractionLevel,
    onClick: PropTypes.func,
}

export default DatacenterGroup
