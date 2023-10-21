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
import { usePortfolio } from '../../data/project'

function NewScenarioModal({ projectId, portfolioId, isOpen, onSubmit: onSubmitUpstream, onCancel: onCancelUpstream }) {
    const { data: portfolio } = usePortfolio(projectId, portfolioId)
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
    const nameInput = useRef(null)

    const resetState = () => {
        setSubmitted(false)
        setTraceLoad(100)
        setTrace(undefined)
        setTopology(undefined)
        setScheduler(undefined)
        setFailuresEnabled(false)
        setOpPhenEnabled(false)
        nameInput.current.value = ''
    }

    const onSubmit = (event) => {
        setSubmitted(true)

        if (event) {
            event.preventDefault()
        }

        const name = nameInput.current.value

        onSubmitUpstream(portfolio.project.id, portfolio.number, {
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
        })

        resetState()
        return true
    }
    const onCancel = () => {
        onCancelUpstream()
        resetState()
    }

    return (
        <Modal title="New Scenario" isOpen={isOpen} onSubmit={onSubmit} onCancel={onCancel}>
            <Form onSubmit={onSubmit}>
                <FormGroup label="Name" fieldId="name" isRequired>
                    <TextInput
                        id="name"
                        name="name"
                        type="text"
                        isDisabled={portfolio?.scenarios?.length === 0}
                        defaultValue={portfolio?.scenarios?.length === 0 ? 'Base scenario' : ''}
                        ref={nameInput}
                    />
                </FormGroup>
                <FormSection title="Workload">
                    <FormGroup label="Trace" fieldId="trace" isRequired>
                        <FormSelect id="trace" name="trace" value={trace} onChange={setTrace}>
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
                        <FormSelect id="topology" name="topology" value={topology} onChange={setTopology}>
                            {topologies.map((topology) => (
                                <FormSelectOption value={topology.number} key={topology.number} label={topology.name} />
                            ))}
                        </FormSelect>
                    </FormGroup>

                    <FormGroup label="Scheduler" fieldId="scheduler" isRequired>
                        <FormSelect id="scheduler" name="scheduler" value={scheduler} onChange={setScheduler}>
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
                    />
                    <Checkbox
                        label="Performance Interference"
                        id="perf-interference"
                        name="perf-interference"
                        isChecked={opPhenEnabled}
                        onChange={() => setOpPhenEnabled((e) => !e)}
                    />
                </FormSection>
            </Form>
        </Modal>
    )
}

NewScenarioModal.propTypes = {
    projectId: PropTypes.number,
    portfolioId: PropTypes.number,
    isOpen: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewScenarioModal
