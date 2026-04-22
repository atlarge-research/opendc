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
import React, { useState } from 'react'
import {
    Badge,
    DescriptionList,
    DescriptionListDescription,
    DescriptionListGroup,
    DescriptionListTerm,
    EmptyState,
    EmptyStateBody,
    Modal,
    ModalVariant,
    Title,
    TreeView,
} from '@patternfly/react-core'

function buildTree(datacenters, onSelect, onRackSelect) {
    return (datacenters ?? []).map((dc) => {
        const rooms = dc.rooms ?? []
        return {
            id: dc.id,
            name: dc.name,
            customBadgeContent: (
                <Badge isRead>
                    {rooms.length} {rooms.length === 1 ? 'room' : 'rooms'}
                </Badge>
            ),
            children: rooms.map((room) => {
                const rackedTiles = (room.tiles ?? []).filter((t) => t.rack)
                return {
                    id: room.id,
                    name: (
                        <span className="pf-c-button pf-m-link pf-m-inline" role="button" tabIndex={0} onClick={() => onSelect && onSelect('room', room)} onKeyDown={(e) => e.key === 'Enter' && onSelect && onSelect('room', room)}>
                            {room.name}
                        </span>
                    ),
                    customBadgeContent: (
                        <Badge isRead>
                            {rackedTiles.length} {rackedTiles.length === 1 ? 'rack' : 'racks'}
                        </Badge>
                    ),
                    children: rackedTiles.map(({ rack }) => ({
                        id: rack.id,
                        name: (
                            <span className="pf-c-button pf-m-link pf-m-inline" role="button" tabIndex={0} onClick={() => onRackSelect(rack)} onKeyDown={(e) => e.key === 'Enter' && onRackSelect(rack)}>
                                {rack.name ?? 'Rack'}
                            </span>
                        ),
                        customBadgeContent: (
                            <Badge isRead>
                                {rack.machines?.length ?? 0}{' '}
                                {(rack.machines?.length ?? 0) === 1 ? 'machine' : 'machines'}
                            </Badge>
                        ),
                    })),
                }
            }),
        }
    })
}

function RackModal({ rack, onClose }) {
    if (!rack) return null

    const machines = (rack.machines ?? []).slice().sort((a, b) => a.position - b.position)

    return (
        <Modal
            title={`Rack: ${rack.name ?? 'Rack'}`}
            isOpen
            onClose={onClose}
            variant={ModalVariant.large}
            ouiaId="rack-detail-modal"
        >
            <DescriptionList isHorizontal>
                <DescriptionListGroup>
                    <DescriptionListTerm>Capacity</DescriptionListTerm>
                    <DescriptionListDescription>{rack.capacity} slots</DescriptionListDescription>
                </DescriptionListGroup>
                <DescriptionListGroup>
                    <DescriptionListTerm>Power capacity</DescriptionListTerm>
                    <DescriptionListDescription>{rack.powerCapacityW} W</DescriptionListDescription>
                </DescriptionListGroup>
                {rack.clusterName && (
                    <DescriptionListGroup>
                        <DescriptionListTerm>Cluster</DescriptionListTerm>
                        <DescriptionListDescription>{rack.clusterName}</DescriptionListDescription>
                    </DescriptionListGroup>
                )}
            </DescriptionList>

            {machines.length === 0 ? (
                <EmptyState>
                    <Title headingLevel="h4" size="md" ouiaId="rack-modal-no-servers-title">No servers</Title>
                    <EmptyStateBody>This rack has no servers configured.</EmptyStateBody>
                </EmptyState>
            ) : (
                machines.map((machine) => (
                    <div key={machine.id} style={{ marginTop: '1.5rem' }}>
                        <Title headingLevel="h3" size="md" style={{ marginBottom: '0.5rem' }} ouiaId={`rack-modal-server-${machine.position}-title`}>
                            Server {machine.position}
                        </Title>
                        <DescriptionList isHorizontal>
                            {(machine.cpus ?? []).map((cpu, i) => (
                                <DescriptionListGroup key={`${machine.position}-cpu-${i}`}>
                                    <DescriptionListTerm>CPU</DescriptionListTerm>
                                    <DescriptionListDescription>
                                        {cpu.name} — {cpu.numberOfCores} cores @ {cpu.clockRateMhz} MHz,{' '}
                                        {cpu.energyConsumptionW} W
                                    </DescriptionListDescription>
                                </DescriptionListGroup>
                            ))}
                            {(machine.memories ?? []).map((mem, i) => (
                                <DescriptionListGroup key={`${machine.position}-mem-${i}`}>
                                    <DescriptionListTerm>Memory</DescriptionListTerm>
                                    <DescriptionListDescription>
                                        {mem.name} — {mem.sizeMb} MB @ {mem.speedMbPerS} MB/s
                                    </DescriptionListDescription>
                                </DescriptionListGroup>
                            ))}
                            {(machine.gpus ?? []).map((gpu, i) => (
                                <DescriptionListGroup key={`${machine.position}-gpu-${i}`}>
                                    <DescriptionListTerm>GPU</DescriptionListTerm>
                                    <DescriptionListDescription>
                                        {gpu.name} — {gpu.numberOfCores} cores @ {gpu.clockRateMhz} MHz,{' '}
                                        {gpu.energyConsumptionW} W
                                    </DescriptionListDescription>
                                </DescriptionListGroup>
                            ))}
                        </DescriptionList>
                    </div>
                ))
            )}
        </Modal>
    )
}

RackModal.propTypes = {
    rack: PropTypes.object,
    onClose: PropTypes.func.isRequired,
}

function TopologyTree({ topology, onSelect }) {
    const [selectedRack, setSelectedRack] = useState(null)
    const items = buildTree(topology?.datacenters, onSelect, setSelectedRack)

    if (items.length === 0) {
        return (
            <EmptyState>
                <Title headingLevel="h4" size="md" ouiaId="topology-tree-empty-title">
                    No datacenters
                </Title>
                <EmptyStateBody>Open the Floor Plan tab to add a datacenter.</EmptyStateBody>
            </EmptyState>
        )
    }

    return (
        <>
            <TreeView data={items} allExpanded hasBadges />
            <RackModal rack={selectedRack} onClose={() => setSelectedRack(null)} />
        </>
    )
}

TopologyTree.propTypes = {
    topology: PropTypes.object,
    onSelect: PropTypes.func,
}

export default TopologyTree
