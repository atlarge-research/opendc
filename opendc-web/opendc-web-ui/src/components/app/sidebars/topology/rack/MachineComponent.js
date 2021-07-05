import PropTypes from 'prop-types'
import React from 'react'
import Image from 'next/image'
import { Machine } from '../../../../../shapes'
import { Badge, ListGroupItem } from 'reactstrap'

const UnitIcon = ({ id, type }) => (
    <div className="ml-1">
        <Image
            src={'/img/topology/' + id + '-icon.png'}
            alt={'Machine contains ' + type + ' units'}
            layout="intrinsic"
            height={35}
            width={35}
        />
    </div>
)

UnitIcon.propTypes = {
    id: PropTypes.string,
    type: PropTypes.string,
}

const MachineComponent = ({ position, machine, onClick }) => {
    const hasNoUnits =
        machine.cpuIds.length + machine.gpuIds.length + machine.memoryIds.length + machine.storageIds.length === 0

    return (
        <ListGroupItem
            action
            className="d-flex justify-content-between align-items-center"
            onClick={onClick}
            style={{ backgroundColor: 'white' }}
        >
            <Badge color="info" className="mr-1">
                {position}
            </Badge>
            <div className="d-inline-flex">
                {machine.cpuIds.length > 0 ? <UnitIcon id="cpu" type="CPU" /> : undefined}
                {machine.gpuIds.length > 0 ? <UnitIcon id="gpu" type="GPU" /> : undefined}
                {machine.memoryIds.length > 0 ? <UnitIcon id="memory" type="memory" /> : undefined}
                {machine.storageIds.length > 0 ? <UnitIcon id="storage" type="storage" /> : undefined}
                {hasNoUnits ? <Badge color="warning">Machine with no units</Badge> : undefined}
            </div>
        </ListGroupItem>
    )
}

MachineComponent.propTypes = {
    machine: Machine,
    position: PropTypes.number,
    onClick: PropTypes.func,
}

export default MachineComponent
