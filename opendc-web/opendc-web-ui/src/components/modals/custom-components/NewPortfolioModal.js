import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import Modal from '../Modal'
import {
    Form,
    FormGroup,
    FormSection,
    NumberInput,
    Select,
    SelectGroup,
    SelectOption,
    SelectVariant,
    TextInput,
} from '@patternfly/react-core'
import { METRIC_GROUPS, METRIC_NAMES } from '../../../util/available-metrics'

const NewPortfolioModal = ({ isOpen, onSubmit: onSubmitUpstream, onCancel: onUpstreamCancel }) => {
    const nameInput = useRef(null)
    const [repeats, setRepeats] = useState(1)
    const [isSelectOpen, setSelectOpen] = useState(false)
    const [selectedMetrics, setSelectedMetrics] = useState([])

    const [isSubmitted, setSubmitted] = useState(false)
    const [errors, setErrors] = useState({})

    const clearState = () => {
        setSubmitted(false)
        setErrors({})
        nameInput.current.value = ''
        setRepeats(1)
        setSelectOpen(false)
        setSelectedMetrics([])
    }

    const onSubmit = (event) => {
        setSubmitted(true)

        if (event) {
            event.preventDefault()
        }

        const name = nameInput.current.value

        if (!name) {
            setErrors({ name: true })
            return false
        } else {
            onSubmitUpstream(name, { enabledMetrics: selectedMetrics, repeatsPerScenario: repeats })
        }

        clearState()
        return false
    }
    const onCancel = () => {
        onUpstreamCancel()
        clearState()
    }

    const onSelect = (event, selection) => {
        if (selectedMetrics.includes(selection)) {
            setSelectedMetrics((metrics) => metrics.filter((item) => item !== selection))
        } else {
            setSelectedMetrics((metrics) => [...metrics, selection])
        }
    }

    return (
        <Modal title="New Portfolio" isOpen={isOpen} onSubmit={onSubmit} onCancel={onCancel}>
            <Form onSubmit={onSubmit}>
                <FormSection>
                    <FormGroup
                        label="Name"
                        fieldId="name"
                        isRequired
                        validated={isSubmitted && errors.name ? 'error' : 'default'}
                        helperTextInvalid="This field cannot be empty"
                    >
                        <TextInput
                            name="name"
                            id="name"
                            type="text"
                            isRequired
                            ref={nameInput}
                            placeholder="My Portfolio"
                        />
                    </FormGroup>
                </FormSection>
                <FormSection title="Targets" titleElement="h4">
                    <FormGroup label="Metrics" fieldId="metrics">
                        <Select
                            variant={SelectVariant.typeaheadMulti}
                            typeAheadAriaLabel="Select a metric"
                            onToggle={() => setSelectOpen(!isSelectOpen)}
                            onSelect={onSelect}
                            onClear={() => setSelectedMetrics([])}
                            selections={selectedMetrics}
                            isOpen={isSelectOpen}
                            placeholderText="Select a metric"
                            menuAppendTo="parent"
                            maxHeight="300px"
                            chipGroupProps={{ numChips: 1 }}
                            isGrouped
                        >
                            {Object.entries(METRIC_GROUPS).map(([group, metrics]) => (
                                <SelectGroup label={group} key={group}>
                                    {metrics.map((metric) => (
                                        <SelectOption key={metric} value={metric}>
                                            {METRIC_NAMES[metric]}
                                        </SelectOption>
                                    ))}
                                </SelectGroup>
                            ))}
                        </Select>
                    </FormGroup>
                    <FormGroup label="Repeats per Scenario" fieldId="repeats">
                        <NumberInput
                            id="repeats"
                            inputName="repeats"
                            type="number"
                            value={repeats}
                            onChange={(e) => setRepeats(Number(e.target.value))}
                            onPlus={() => setRepeats((r) => r + 1)}
                            onMinus={() => setRepeats((r) => r - 1)}
                            min={1}
                        />
                    </FormGroup>
                </FormSection>
            </Form>
        </Modal>
    )
}

NewPortfolioModal.propTypes = {
    isOpen: PropTypes.bool.isRequired,
    onSubmit: PropTypes.func.isRequired,
    onCancel: PropTypes.func.isRequired,
}

export default NewPortfolioModal
