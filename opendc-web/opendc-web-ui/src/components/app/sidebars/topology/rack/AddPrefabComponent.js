import PropTypes from 'prop-types'
import React from 'react'
import { SaveIcon } from '@patternfly/react-icons'
import { Button } from '@patternfly/react-core'

const AddPrefabComponent = ({ onClick }) => (
    <Button variant="primary" icon={<SaveIcon />} isBlock onClick={onClick} className="pf-u-mb-sm">
        Save this rack to a prefab
    </Button>
)

AddPrefabComponent.propTypes = {
    onClick: PropTypes.func,
}

export default AddPrefabComponent
