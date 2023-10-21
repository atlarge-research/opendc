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
import React, { useMemo } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import MachineListComponent from './MachineListComponent'
import { goFromRackToMachine } from '../../../../redux/actions/interaction-level'
import { addMachine } from '../../../../redux/actions/topology/rack'

function MachineListContainer({ tileId, ...props }) {
    const rack = useSelector((state) => state.topology.racks[state.topology.tiles[tileId].rack])
    const machines = useSelector((state) => rack.machines.map((id) => state.topology.machines[id]))
    const machinesNull = useMemo(() => {
        const res = Array(rack.capacity).fill(null)
        for (const machine of machines) {
            res[machine.position - 1] = machine
        }
        return res
    }, [rack, machines])
    const dispatch = useDispatch()

    return (
        <MachineListComponent
            {...props}
            machines={machinesNull}
            onAdd={(index) => dispatch(addMachine(rack.id, index))}
            onSelect={(index) => dispatch(goFromRackToMachine(index))}
        />
    )
}

MachineListContainer.propTypes = {
    tileId: PropTypes.string.isRequired,
}

export default MachineListContainer
