import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import Modal from '../util/modals/Modal'
import {
    Checkbox,
    Form,
    FormGroup,
    FormSection,
    FormSelect,
    FormSelectOption,
    NumberInput,
    TextInput,
} from '@patternfly/react-core'
import { useSchedulers, useTraces } from '../../data/experiments'
import { useTopologies } from '../../data/topology'
import { useExperiment } from '../../data/project'

function NewScenarioModal({ projectId, experimentId, isOpen, onSubmit: onSubmitUpstream, onCancel: onCancelUpstream }) {
    const { data: experiment } = useExperiment(projectId, experimentId)
    const { data: topologies = [] } = useTopologies(projectId, { enabled: isOpen })
    const { data: traces = [] } = useTraces({ enabled: isOpen })
    const { data: schedulers = [] } = useSchedulers({ enabled: isOpen })

    // eslint-disable-next-line no-unused-vars
    const [isSubmitted, setSubmitted] = useState(false)
    const [traceLoad, setTraceLoad] = useState(100)
    const [trace, setTrace] = useState(undefined)
    const [topology, setTopology] = useState(undefined)
    const [scheduler, setScheduler] = useState(undefined)
    const [failuresEnabled, setFailuresEnabled] = useState(false)
    const [opPhenEnabled, setOpPhenEnabled] = useState(false)
    const [exportsEnabled, setExportsEnabled] = useState(false)
    const [exportInterval, setExportInterval] = useState(3600)
    const [exportFiles, setExportFiles] = useState({ host: true, task: true, service: true, scheduler: true })
    const nameInput = useRef(null)

    const resetState = () => {
        setSubmitted(false)
        setTraceLoad(100)
        setTrace(undefined)
        setTopology(undefined)
        setScheduler(undefined)
        setFailuresEnabled(false)
        setOpPhenEnabled(false)
        setExportsEnabled(false)
        setExportInterval(3600)
        setExportFiles({ host: true, task: true, service: true, scheduler: true })
        nameInput.current.value = ''
    }

    const onSubmit = (event) => {
        setSubmitted(true)

        if (event) {
            event.preventDefault()
        }

        const name = nameInput.current.value

        onSubmitUpstream(experiment.project.id, experiment.number, {
            name,
            workload: {
                trace: trace || traces[0].id,
                samplingFraction: traceLoad / 100,
            },
            topology: topology || topologies[0].number,
            phenomena: {
                failures: failuresEnabled,
                interference: opPhenEnabled,
            },
            schedulerName: scheduler || schedulers[0],
            exportModels: exportsEnabled
                ? [
                      {
                          exportInterval,
                          filesToExport: Object.entries(exportFiles)
                              .filter(([, checked]) => checked)
                              .map(([name]) => name),
                      },
                  ]
                : null,
        })

        resetState()
        return true
    }
    const onCancel = () => {
        onCancelUpstream()
        resetState()
    }

    return (
        <Modal title="New Scenario" isOpen={isOpen} onSubmit={onSubmit} onCancel={onCancel} ouiaId="new-scenario-modal">
            <Form onSubmit={onSubmit}>
                <FormGroup label="Name" fieldId="name" isRequired>
                    <TextInput
                        id="name"
                        name="name"
                        type="text"
                        isDisabled={experiment?.scenarios?.length === 0}
                        defaultValue={experiment?.scenarios?.length === 0 ? 'Base scenario' : ''}
                        ref={nameInput}
                        ouiaId="new-scenario-name-input"
                    />
                </FormGroup>
                <FormSection title="Workload">
                    <FormGroup label="Trace" fieldId="trace" isRequired>
                        <FormSelect id="trace" name="trace" value={trace} onChange={setTrace} ouiaId="new-scenario-trace-select">
                            {traces.map((trace) => (
                                <FormSelectOption value={trace.id} key={trace.id} label={trace.name} />
                            ))}
                        </FormSelect>
                    </FormGroup>
                    <FormGroup label="Load Sampling Fraction" fieldId="trace-load" isRequired>
                        <NumberInput
                            name="trace-load"
                            type="number"
                            min={0}
                            max={100}
                            value={traceLoad}
                            onMinus={() => setTraceLoad((load) => load - 1)}
                            onPlus={() => setTraceLoad((load) => load + 1)}
                            onChange={(e) => setTraceLoad(Number(e.target.value))}
                            unit="%"
                        />
                    </FormGroup>
                </FormSection>
                <FormSection title="Topology">
                    <FormGroup label="Topology" fieldId="topology" isRequired>
                        <FormSelect id="topology" name="topology" value={topology} onChange={setTopology} ouiaId="new-scenario-topology-select">
                            {topologies.map((topology) => (
                                <FormSelectOption value={topology.number} key={topology.number} label={topology.name} />
                            ))}
                        </FormSelect>
                    </FormGroup>

                    <FormGroup label="Scheduler" fieldId="scheduler" isRequired>
                        <FormSelect id="scheduler" name="scheduler" value={scheduler} onChange={setScheduler} ouiaId="new-scenario-scheduler-select">
                            {schedulers.map((scheduler) => (
                                <FormSelectOption value={scheduler} key={scheduler} label={scheduler} />
                            ))}
                        </FormSelect>
                    </FormGroup>
                </FormSection>
                <FormSection title="Operational Phenomena">
                    <Checkbox
                        label="Failures"
                        id="failures"
                        name="failures"
                        isChecked={failuresEnabled}
                        onChange={() => setFailuresEnabled((e) => !e)}
                        ouiaId="new-scenario-failures-checkbox"
                    />
                    <Checkbox
                        label="Performance Interference"
                        id="perf-interference"
                        name="perf-interference"
                        isChecked={opPhenEnabled}
                        onChange={() => setOpPhenEnabled((e) => !e)}
                        ouiaId="new-scenario-interference-checkbox"
                    />
                </FormSection>
                <FormSection title="Exports">
                    <Checkbox
                        label="Enable export models"
                        id="exports-enabled"
                        name="exports-enabled"
                        isChecked={exportsEnabled}
                        onChange={() => setExportsEnabled((e) => !e)}
                        ouiaId="new-scenario-exports-checkbox"
                    />
                    {exportsEnabled && (
                        <>
                            <FormGroup label="Export interval (s)" fieldId="export-interval">
                                <NumberInput
                                    id="export-interval"
                                    name="export-interval"
                                    min={60}
                                    value={exportInterval}
                                    onMinus={() => setExportInterval((v) => Math.max(60, v - 60))}
                                    onPlus={() => setExportInterval((v) => v + 60)}
                                    onChange={(e) => setExportInterval(Number(e.target.value))}
                                    unit="s"
                                />
                            </FormGroup>
                            <FormGroup label="Files to export" fieldId="export-files">
                                {['host', 'task', 'service', 'scheduler'].map((file) => (
                                    <Checkbox
                                        key={file}
                                        label={file}
                                        id={`export-file-${file}`}
                                        name={`export-file-${file}`}
                                        isChecked={exportFiles[file]}
                                        onChange={() =>
                                            setExportFiles((prev) => ({ ...prev, [file]: !prev[file] }))
                                        }
                                        ouiaId={`new-scenario-export-file-${file}`}
                                    />
                                ))}
                            </FormGroup>
                        </>
                    )}
                </FormSection>
            </Form>
        </Modal>
    )
}

NewScenarioModal.propTypes = {
    projectId: PropTypes.number,
    experimentId: PropTypes.number,
    isOpen: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewScenarioModal
