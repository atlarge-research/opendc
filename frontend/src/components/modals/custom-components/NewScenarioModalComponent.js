import PropTypes from 'prop-types'
import React, { useRef } from 'react'
import { Form, FormGroup, Input, Label } from 'reactstrap'
import Shapes from '../../../shapes'
import Modal from '../Modal'

const NewScenarioModalComponent = ({
    show,
    callback,
    currentPortfolioId,
    currentPortfolioScenarioIds,
    traces,
    topologies,
    schedulers,
}) => {
    const textInput = useRef(null)
    const traceSelect = useRef(null)
    const traceLoadInput = useRef(null)
    const topologySelect = useRef(null)
    const failuresCheckbox = useRef(null)
    const performanceInterferenceCheckbox = useRef(null)
    const schedulerSelect = useRef(null)

    const onSubmit = () => {
        callback(
            textInput.current.value,
            currentPortfolioId,
            {
                traceId: traceSelect.current.value,
                loadSamplingFraction: parseFloat(traceLoadInput.current.value),
            },
            {
                topologyId: topologySelect.current.value,
            },
            {
                failuresEnabled: failuresCheckbox.current.checked,
                performanceInterferenceEnabled: performanceInterferenceCheckbox.current.checked,
                schedulerName: schedulerSelect.current.value,
            }
        )
    }
    const onCancel = () => {
        callback(undefined)
    }

    return (
        <Modal title="New Scenario" show={show} onSubmit={onSubmit} onCancel={onCancel}>
            <Form
                onSubmit={(e) => {
                    e.preventDefault()
                    onSubmit()
                }}
            >
                <FormGroup>
                    <Label for="name">Name</Label>
                    <Input
                        name="name"
                        type="text"
                        required
                        disabled={currentPortfolioScenarioIds.length === 0}
                        defaultValue={currentPortfolioScenarioIds.length === 0 ? 'Base scenario' : ''}
                        innerRef={textInput}
                    />
                </FormGroup>
                <h4>Trace</h4>
                <FormGroup>
                    <Label for="trace">Trace</Label>
                    <Input name="trace" type="select" innerRef={traceSelect}>
                        {traces.map((trace) => (
                            <option value={trace._id} key={trace._id}>
                                {trace.name}
                            </option>
                        ))}
                    </Input>
                </FormGroup>
                <FormGroup>
                    <Label for="trace-load">Load sampling fraction</Label>
                    <Input
                        name="trace-load"
                        type="number"
                        innerRef={traceLoadInput}
                        required
                        defaultValue="1"
                        min="0"
                        max="1"
                        step="0.1"
                    />
                </FormGroup>
                <h4>Topology</h4>
                <div className="form-group">
                    <Label for="topology">Topology</Label>
                    <Input name="topology" type="select" innerRef={topologySelect}>
                        {topologies.map((topology) => (
                            <option value={topology._id} key={topology._id}>
                                {topology.name}
                            </option>
                        ))}
                    </Input>
                </div>
                <h4>Operational Phenomena</h4>
                <FormGroup check>
                    <Label check for="failures">
                        <Input type="checkbox" name="failures" innerRef={failuresCheckbox} />{' '}
                        <span className="ml-2">Enable failures</span>
                    </Label>
                </FormGroup>
                <FormGroup check>
                    <Label check for="perf-interference">
                        <Input type="checkbox" name="perf-interference" innerRef={performanceInterferenceCheckbox} />{' '}
                        <span className="ml-2">Enable performance interference</span>
                    </Label>
                </FormGroup>
                <FormGroup>
                    <Label for="scheduler">Scheduler</Label>
                    <Input name="scheduler" type="select" innerRef={schedulerSelect}>
                        {schedulers.map((scheduler) => (
                            <option value={scheduler.name} key={scheduler.name}>
                                {scheduler.name}
                            </option>
                        ))}
                    </Input>
                </FormGroup>
            </Form>
        </Modal>
    )
}

NewScenarioModalComponent.propTypes = {
    show: PropTypes.bool.isRequired,
    currentPortfolioId: PropTypes.string.isRequired,
    currentPortfolioScenarioIds: PropTypes.arrayOf(PropTypes.string),
    traces: PropTypes.arrayOf(Shapes.Trace),
    topologies: PropTypes.arrayOf(Shapes.Topology),
    schedulers: PropTypes.arrayOf(Shapes.Scheduler),
    callback: PropTypes.func.isRequired,
}

export default NewScenarioModalComponent
