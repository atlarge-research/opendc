import HomeIcon from '@patternfly/react-icons/dist/js/icons/home-icon'
import EditIcon from '@patternfly/react-icons/dist/js/icons/edit-icon'
import EyeIcon from '@patternfly/react-icons/dist/js/icons/eye-icon'

export const AUTH_ICON_MAP = {
    OWNER: HomeIcon,
    EDITOR: EditIcon,
    VIEWER: EyeIcon,
}

export const AUTH_DESCRIPTION_MAP = {
    OWNER: 'Own',
    EDITOR: 'Can Edit',
    VIEWER: 'Can View',
}
