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
import { Button, AlertGroup, Alert, AlertVariant, AlertActionCloseButton } from '@patternfly/react-core'
import { SaveIcon } from '@patternfly/react-icons'
import { useSelector } from 'react-redux'
import { useRouter } from 'next/router'
import { denormalize } from 'normalizr'
import { Machine } from '../../../../util/topology-schema'
import { useNewMachinePrefab } from '../../../../data/machine-prefabs'

function AddMachinePrefab({ tileId, position }) {
    const [alert, setAlert] = React.useState(null)
    const router = useRouter()
    const { project: projectId } = router.query
    const { mutate: addMachinePrefab } = useNewMachinePrefab()

    const machine = useSelector(({ topology }) => {
        const rack = topology.racks[topology.tiles[tileId]?.rack]
        if (!rack) return null
        for (const machineId of rack.machines) {
            const m = topology.machines[machineId]
            if (m && m.position === position) {
                return denormalize(machineId, Machine, topology)
            }
        }
        return null
    })

    const onClick = () => {
        if (machine && projectId) {
            addMachinePrefab(
                {
                    projectId,
                    name: machine.name ?? `Machine at position ${machine.position}`,
                    machine: {
                        ...machine,
                        id: '',
                    },
                },
                {
                    onSuccess: () => {
                        setAlert({ variant: AlertVariant.success, title: 'Machine saved as prefab' })
                    },
                    onError: (error) => {
                        setAlert({ variant: AlertVariant.danger, title: `Failed to save machine: ${error}` })
                    },
                }
            )
        }
    }

    return (
        <>
            <AlertGroup isToast>
                {alert && (
                    <Alert
                        isLiveRegion
                        variant={alert.variant}
                        title={alert.title}
                        actionClose={
                            <AlertActionCloseButton
                                title={alert.title}
                                onClose={() => setAlert(null)}
                            />
                        }
                    />
                )}
            </AlertGroup>
            <Button
                variant="primary"
                icon={<SaveIcon />}
                isBlock
                onClick={onClick}
                className="pf-u-mb-sm"
                isDisabled={!machine}
                ouiaId="save-machine-prefab"
            >
                Save this machine to a prefab
            </Button>
        </>
    )
}

AddMachinePrefab.propTypes = {
    tileId: PropTypes.string.isRequired,
    position: PropTypes.number.isRequired,
}

export default AddMachinePrefab
