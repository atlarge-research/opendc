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
import { useDispatch, useSelector } from 'react-redux'
import { Button, Title } from '@patternfly/react-core'
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon'
import { goFromBuildingToDatacenter } from '../../../../redux/actions/interaction-level'

function DatacenterListContainer() {
    const dispatch = useDispatch()
    const datacenterIds = useSelector((state) => state.topology.root?.datacenters ?? [])
    const datacenters = useSelector((state) => state.topology.datacenters ?? {})

    if (datacenterIds.length === 0) return null

    return (
        <>
            <Title headingLevel="h2" ouiaId="building-datacenters-title">Datacenters</Title>
            {datacenterIds.map((id) => (
                <Button
                    key={id}
                    variant="link"
                    isBlock
                    icon={<AngleRightIcon />}
                    iconPosition="right"
                    onClick={() => dispatch(goFromBuildingToDatacenter(id))}
                    ouiaId={`datacenter-select-${id}`}
                >
                    {datacenters[id]?.name ?? id}
                </Button>
            ))}
        </>
    )
}

export default DatacenterListContainer
