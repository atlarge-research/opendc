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

import React, { useState } from 'react'
import { useDispatch, useSelector } from 'react-redux'
import { Button, Form, FormGroup, NumberInput, TextInput } from '@patternfly/react-core'
import PlusIcon from '@patternfly/react-icons/dist/js/icons/plus-icon'
import { addDatacenter } from '../../../../redux/actions/topology/datacenter'
import { goFromBuildingToDatacenter } from '../../../../redux/actions/interaction-level'
import { DEFAULT_FLOORPLAN_HEIGHT, DEFAULT_FLOORPLAN_WIDTH } from '../../map/MapConstants'

function NewDatacenterContainer() {
    const dispatch = useDispatch()
    const topologyId = useSelector((state) => state.topology.root.id)
    const existingDatacenters = useSelector((state) => Object.values(state.topology.datacenters ?? {}))
    const [name, setName] = useState('')
    const [width, setWidth] = useState(DEFAULT_FLOORPLAN_WIDTH)
    const [height, setHeight] = useState(DEFAULT_FLOORPLAN_HEIGHT)

    const trimmedName = name.trim()
    const isDuplicate = trimmedName !== '' && existingDatacenters.some((dc) => dc.name === trimmedName)
    const isInvalid = !trimmedName || isDuplicate

    const handleAdd = () => {
        if (isInvalid) return
        const nextX = existingDatacenters.reduce((max, dc) => Math.max(max, (dc.x ?? 0) + (dc.width ?? 0) + 1), 0)
        const action = addDatacenter(topologyId, {
            name: trimmedName,
            rooms: [],
            width: Math.max(1, width),
            height: Math.max(1, height),
            x: nextX,
            y: 0,
        })
        dispatch(action)
        dispatch(goFromBuildingToDatacenter(action.datacenter.id))
        setName('')
        setWidth(DEFAULT_FLOORPLAN_WIDTH)
        setHeight(DEFAULT_FLOORPLAN_HEIGHT)
    }

    return (
        <Form>
            <FormGroup
                label="Name"
                fieldId="dc-name"
                helperTextInvalid={isDuplicate ? 'A datacenter with this name already exists' : 'Name is required'}
                validated={trimmedName === '' ? 'default' : isDuplicate ? 'error' : 'success'}
            >
                <TextInput
                    id="dc-name"
                    value={name}
                    onChange={(val) => setName(val)}
                    placeholder="Datacenter name"
                    validated={trimmedName === '' ? 'default' : isDuplicate ? 'error' : 'success'}
                    ouiaId="new-datacenter-name-input"
                />
            </FormGroup>
            <FormGroup label="Width (tiles)" fieldId="dc-width">
                <NumberInput
                    id="dc-width"
                    value={width}
                    min={1}
                    onMinus={() => setWidth((w) => Math.max(1, w - 1))}
                    onPlus={() => setWidth((w) => w + 1)}
                    onChange={(e) => setWidth(Math.max(1, parseInt(e.target.value) || 1))}
                />
            </FormGroup>
            <FormGroup label="Height (tiles)" fieldId="dc-height">
                <NumberInput
                    id="dc-height"
                    value={height}
                    min={1}
                    onMinus={() => setHeight((h) => Math.max(1, h - 1))}
                    onPlus={() => setHeight((h) => h + 1)}
                    onChange={(e) => setHeight(Math.max(1, parseInt(e.target.value) || 1))}
                />
            </FormGroup>
            <Button isBlock icon={<PlusIcon />} onClick={handleAdd} isDisabled={isInvalid} ouiaId="add-datacenter">
                Add datacenter
            </Button>
        </Form>
    )
}

export default NewDatacenterContainer
