import PropTypes from 'prop-types'
import React, { useRef, useState } from 'react'
import { Button, TextInput } from '@patternfly/react-core'
import { PencilAltIcon, CheckIcon, TimesIcon } from '@patternfly/react-icons'

function NameComponent({ name, onEdit }) {
    const [isEditing, setEditing] = useState(false)
    const nameInput = useRef(null)

    const onCancel = () => {
        nameInput.current.value = name
        setEditing(false)
    }

    const onSubmit = (event) => {
        if (event) {
            event.preventDefault()
        }

        const name = nameInput.current.value
        if (name) {
            onEdit(name)
        }

        setEditing(false)
    }

    return (
        <form
            className={`pf-c-inline-edit ${isEditing ? 'pf-m-inline-editable' : ''} pf-u-display-inline-block`}
            onSubmit={onSubmit}
        >
            <div className="pf-c-inline-edit__group">
                <div className="pf-c-inline-edit__value" id="single-inline-edit-example-label">
                    {name}
                </div>
                <div className="pf-c-inline-edit__action pf-m-enable-editable">
                    <Button className="pf-u-py-0" variant="plain" aria-label="Edit" onClick={() => setEditing(true)}>
                        <PencilAltIcon />
                    </Button>
                </div>
            </div>
            <div className="pf-c-inline-edit__group">
                <div className="pf-c-inline-edit__input">
                    <TextInput type="text" defaultValue={name} ref={nameInput} aria-label="Editable text input" />
                </div>
                <div className="pf-c-inline-edit__group pf-m-action-group pf-m-icon-group">
                    <div className="pf-c-inline-edit__action pf-m-valid">
                        <Button className="pf-u-py-0" variant="plain" aria-label="Save edits" onClick={onSubmit}>
                            <CheckIcon />
                        </Button>
                    </div>
                    <div className="pf-c-inline-edit__action">
                        <Button className="pf-u-py-0" variant="plain" aria-label="Cancel edits" onClick={onCancel}>
                            <TimesIcon />
                        </Button>
                    </div>
                </div>
            </div>
        </form>
    )
}

NameComponent.propTypes = {
    name: PropTypes.string,
    onEdit: PropTypes.func,
}

export default NameComponent
