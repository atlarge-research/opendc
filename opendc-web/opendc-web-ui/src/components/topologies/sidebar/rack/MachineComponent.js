import PropTypes from 'prop-types'
import React from 'react'
import { Flex, Label } from '@patternfly/react-core'
import { Machine } from '../../../../shapes'

const UnitIcon = ({ id, type }) => (
    // eslint-disable-next-line @next/next/no-img-element
    <img src={'/img/topology/' + id + '-icon.png'} alt={'Machine contains ' + type + ' units'} height={24} width={24} />
)

UnitIcon.propTypes = {
    id: PropTypes.string,
    type: PropTypes.string,
}

function MachineComponent({ machine, onClick }) {
    const hasNoUnits =
        machine.cpus.length + machine.gpus.length + machine.memories.length + machine.storages.length === 0

    return (
        <Flex onClick={() => onClick()}>
            {machine.cpus.length > 0 ? <UnitIcon id="cpu" type="CPU" /> : undefined}
            {machine.gpus.length > 0 ? <UnitIcon id="gpu" type="GPU" /> : undefined}
            {machine.memories.length > 0 ? <UnitIcon id="memory" type="memory" /> : undefined}
            {machine.storages.length > 0 ? <UnitIcon id="storage" type="storage" /> : undefined}
            {hasNoUnits ? (
                <Label variant="outline" color="orange">
                    Machine with no units
                </Label>
            ) : undefined}
        </Flex>
    )
}

MachineComponent.propTypes = {
    machine: Machine.isRequired,
    onClick: PropTypes.func,
}

export default MachineComponent
