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
import PropTypes from 'prop-types'
import {
    Form,
    FormGroup,
    FileUpload,
    Alert,
} from '@patternfly/react-core'
import Modal from '../util/modals/Modal'

function ImportTopologyModal({ isOpen, onCancel, onSubmit, topologies = [] }) {
    const [fileValue, setFileValue] = useState('')
    const [fileName, setFileName] = useState('')
    const [isLoading, setIsLoading] = useState(false)
    const [error, setError] = useState(null)

    const handleFileChange = (value, filename) => {
        setFileValue(value)
        setFileName(filename)
    }

    const handleFileReadStarted = () => setIsLoading(true)
    const handleFileReadFinished = () => setIsLoading(false)

    const clearState = () => {
        setFileValue('')
        setFileName('')
        setError(null)
    }

    const onCancelProxy = () => {
        onCancel()
        clearState()
    }

    const handleFormSubmit = (event) => {
        if (event) {
            event.preventDefault()
        }

        if (!fileValue) {
            setError('Please upload a topology JSON file.')
            return
        }

        try {
            const topology = JSON.parse(fileValue)
            if (!topology.name || !topology.rooms) {
                setError('Invalid topology format. Name and rooms are required.')
                return
            }

            if (topologies.some((t) => t.name === topology.name)) {
                setError('A topology with the name "' + topology.name + '" already exists.')
                return
            }

            onSubmit(topology)
            clearState()
        } catch (err) {
            setError('Failed to parse JSON: ' + err.message)
        }
    }

    return (
        <Modal
            title="Import Topology"
            isOpen={isOpen}
            onCancel={onCancelProxy}
            onSubmit={handleFormSubmit}
            submitButtonText="Import"
        >
            <Form onSubmit={handleFormSubmit}>
                {error && (
                    <Alert variant="danger" title={error} isInline />
                )}
                <FormGroup
                    label="Topology JSON File"
                    isRequired
                    fieldId="topology-file"
                    helperText="Upload the JSON file of the topology you want to import."
                >
                    <FileUpload
                        id="topology-file"
                        type="text"
                        value={fileValue}
                        filename={fileName}
                        onChange={handleFileChange}
                        onReadStarted={handleFileReadStarted}
                        onReadFinished={handleFileReadFinished}
                        isLoading={isLoading}
                        dropzoneProps={{
                            accept: '.json',
                        }}
                    />
                </FormGroup>
            </Form>
        </Modal>
    )
}

ImportTopologyModal.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onCancel: PropTypes.func.isRequired,
    onSubmit: PropTypes.func.isRequired,
    topologies: PropTypes.array,
}

export default ImportTopologyModal
