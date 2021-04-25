import PropTypes from 'prop-types'
import React, { useRef } from 'react'
import { Form, FormGroup, Input, Label } from 'reactstrap'
import Modal from '../Modal'
import { AVAILABLE_METRICS, METRIC_NAMES } from '../../../util/available-metrics'

const NewPortfolioModalComponent = ({ show, callback }) => {
    const form = useRef(null)
    const textInput = useRef(null)
    const repeatsInput = useRef(null)
    const metricCheckboxes = useRef({})

    const onSubmit = () => {
        if (form.current.reportValidity()) {
            callback(textInput.current.value, {
                enabledMetrics: AVAILABLE_METRICS.filter((metric) => metricCheckboxes.current[metric].checked),
                repeatsPerScenario: parseInt(repeatsInput.current.value),
            })

            return true
        } else {
            return false
        }
    }
    const onCancel = () => callback(undefined)

    return (
        <Modal title="New Portfolio" show={show} onSubmit={onSubmit} onCancel={onCancel}>
            <Form
                onSubmit={(e) => {
                    e.preventDefault()
                    this.onSubmit()
                }}
                innerRef={form}
            >
                <FormGroup>
                    <Label for="name">Name</Label>
                    <Input name="name" type="text" required innerRef={textInput} placeholder="My Portfolio" />
                </FormGroup>
                <h4>Targets</h4>
                <h5>Metrics</h5>
                <FormGroup>
                    {AVAILABLE_METRICS.map((metric) => (
                        <FormGroup check key={metric}>
                            <Label for={metric} check>
                                <Input
                                    name={metric}
                                    type="checkbox"
                                    innerRef={(ref) => (metricCheckboxes.current[metric] = ref)}
                                />
                                {METRIC_NAMES[metric]}
                            </Label>
                        </FormGroup>
                    ))}
                </FormGroup>
                <FormGroup>
                    <Label for="repeats">Repeats per scenario</Label>
                    <Input
                        name="repeats"
                        type="number"
                        required
                        innerRef={repeatsInput}
                        defaultValue="1"
                        min="1"
                        step="1"
                    />
                </FormGroup>
            </Form>
        </Modal>
    )
}

NewPortfolioModalComponent.propTypes = {
    show: PropTypes.bool.isRequired,
    callback: PropTypes.func.isRequired,
}

export default NewPortfolioModalComponent
