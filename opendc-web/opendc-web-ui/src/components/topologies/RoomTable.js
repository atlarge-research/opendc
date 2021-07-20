import { Button } from '@patternfly/react-core'
import PropTypes from 'prop-types'
import React from 'react'
import { useDispatch } from 'react-redux'
import { useTopology } from '../../data/topology'
import { Table, TableBody, TableHeader } from '@patternfly/react-table'
import { goToRoom } from '../../redux/actions/interaction-level'
import { deleteRoom } from '../../redux/actions/topology/room'
import TableEmptyState from '../util/TableEmptyState'

function RoomTable({ topologyId }) {
    const dispatch = useDispatch()
    const { status, data: topology } = useTopology(topologyId)

    const onClick = (room) => dispatch(goToRoom(room._id))
    const onDelete = (room) => dispatch(deleteRoom(room._id))

    const columns = ['Name', 'Tiles', 'Racks']
    const rows =
        topology?.rooms.length > 0
            ? topology.rooms.map((room) => {
                  const tileCount = room.tiles.length
                  const rackCount = room.tiles.filter((tile) => tile.rack).length
                  return [
                      {
                          title: (
                              <Button variant="link" isInline onClick={() => onClick(room)}>
                                  {room.name}
                              </Button>
                          ),
                      },
                      tileCount === 1 ? '1 tile' : `${tileCount} tiles`,
                      rackCount === 1 ? '1 rack' : `${rackCount} racks`,
                  ]
              })
            : [
                  {
                      heightAuto: true,
                      cells: [
                          {
                              props: { colSpan: 3 },
                              title: <TableEmptyState status={status} loadingTitle="Loading Rooms" />,
                          },
                      ],
                  },
              ]

    const actions =
        topology?.rooms.length > 0
            ? [
                  {
                      title: 'Delete room',
                      onClick: (_, rowId) => onDelete(topology.rooms[rowId]),
                  },
              ]
            : []

    return (
        <Table aria-label="Room list" variant="compact" cells={columns} rows={rows} actions={actions}>
            <TableHeader />
            <TableBody />
        </Table>
    )
}

RoomTable.propTypes = {
    topologyId: PropTypes.string,
}

export default RoomTable
