import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import { Portfolio } from '../../../shapes'
import Modal from '../Modal'
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
import { useSchedulers, useTraces } from '../../../data/experiments'
import { useProjectTopologies } from '../../../data/topology'
import { usePortfolio } from '../../../data/project'

const NewScenarioModal = ({ portfolioId, isOpen, onSubmit: onSubmitUpstream, onCancel: onCancelUpstream }) => {
    const { data: portfolio } = usePortfolio(portfolioId)
    const { data: topologies = [] } = useProjectTopologies(portfolio?.projectId)
    const { data: traces = [] } = useTraces()
    const { data: schedulers = [] } = useSchedulers()

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

        onSubmitUpstream(
            name,
            portfolio._id,
            {
                traceId: trace || traces[0]._id,
                loadSamplingFraction: traceLoad / 100,
            },
            {
                topologyId: topology || topologies[0]._id,
            },
            {
                failuresEnabled,
                performanceInterferenceEnabled: opPhenEnabled,
                schedulerName: scheduler || schedulers[0].name,
            }
        )

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
                        isDisabled={portfolio?.scenarioIds?.length === 0}
                        defaultValue={portfolio?.scenarioIds?.length === 0 ? 'Base scenario' : ''}
                        ref={nameInput}
                    />
                </FormGroup>
                <FormSection title="Workload">
                    <FormGroup label="Trace" fieldId="trace" isRequired>
                        <FormSelect id="trace" name="trace" value={trace} onChange={setTrace}>
                            {traces.map((trace) => (
                                <FormSelectOption value={trace._id} key={trace._id} label={trace.name} />
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
                                <FormSelectOption value={topology._id} key={topology._id} label={topology.name} />
                            ))}
                        </FormSelect>
                    </FormGroup>

                    <FormGroup label="Scheduler" fieldId="scheduler" isRequired>
                        <FormSelect id="scheduler" name="scheduler" value={scheduler} onChange={setScheduler}>
                            {schedulers.map((scheduler) => (
                                <FormSelectOption value={scheduler.name} key={scheduler.name} label={scheduler.name} />
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
    portfolioId: PropTypes.string,
    isOpen: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewScenarioModal
