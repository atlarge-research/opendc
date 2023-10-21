import { Button, Bullseye } from '@patternfly/react-core'
import PropTypes from 'prop-types'
import React from 'react'
import { useDispatch } from 'react-redux'
import { useTopology } from '../../data/topology'
import { Tr, Th, Thead, TableComposable, Td, ActionsColumn, Tbody } from '@patternfly/react-table'
import { deleteRoom } from '../../redux/actions/topology/room'
import TableEmptyState from '../util/TableEmptyState'

function RoomTable({ projectId, topologyId, onSelect }) {
    const dispatch = useDispatch()
    const { status, data: topology } = useTopology(projectId, topologyId)
    const onDelete = (room) => dispatch(deleteRoom(room.id))
    const actions = (room) => [
        {
            title: 'Delete room',
            onClick: () => onDelete(room),
        },
    ]

    return (
        <TableComposable aria-label="Room list" variant="compact">
            <Thead>
                <Tr>
                    <Th>Name</Th>
                    <Th>Tiles</Th>
                    <Th>Racks</Th>
                </Tr>
            </Thead>
            <Tbody>
                {topology?.rooms.map((room) => {
                    const tileCount = room.tiles.length
                    const rackCount = room.tiles.filter((tile) => tile.rack).length
                    return (
                        <Tr key={room.id}>
                            <Td dataLabel="Name">
                                <Button variant="link" isInline onClick={() => onSelect(room)}>
                                    {room.name}
                                </Button>
                            </Td>
                            <Td dataLabel="Tiles">{tileCount === 1 ? '1 tile' : `${tileCount} tiles`}</Td>
                            <Td dataLabel="Racks">{rackCount === 1 ? '1 rack' : `${rackCount} racks`}</Td>
                            <Td isActionCell>
                                <ActionsColumn items={actions(room)} />
                            </Td>
                        </Tr>
                    )
                })}
                {topology?.rooms.length === 0 && (
                    <Tr>
                        <Td colSpan={4}>
                            <Bullseye>
                                <TableEmptyState
                                    status={status}
                                    loadingTitle="Loading Rooms"
                                    emptyTitle="No rooms"
                                    emptyText="There are currently no rooms in this topology. Open the Floor Plan to create a room"
                                />
                            </Bullseye>
                        </Td>
                    </Tr>
                )}
            </Tbody>
        </TableComposable>
    )
}

RoomTable.propTypes = {
    projectId: PropTypes.number,
    topologyId: PropTypes.number,
    onSelect: PropTypes.func,
}

export default RoomTable
