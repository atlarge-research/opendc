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

import {
    Button,
    ButtonVariant,
    Dropdown,
    DropdownItem,
    KebabToggle,
    ToolbarGroup,
    ToolbarItem,
} from '@patternfly/react-core'
import { useReducer } from 'react'
import { GithubIcon, HelpIcon } from '@patternfly/react-icons'

function AppHeaderTools() {
    const [isKebabDropdownOpen, toggleKebabDropdown] = useReducer((t) => !t, false)
    const kebabDropdownItems = [
        <DropdownItem
            key={0}
            component={
                <a href="https://opendc.org" target="_blank" rel="noreferrer">
                    <HelpIcon /> Help
                </a>
            }
        />,
    ]

    return (
        <ToolbarGroup
            variant="icon-button-group"
            alignment={{ default: 'alignRight' }}
            spacer={{ default: 'spacerNone', md: 'spacerMd' }}
        >
            <ToolbarGroup variant="icon-button-group" visibility={{ default: 'hidden', lg: 'visible' }}>
                <ToolbarItem>
                    <Button
                        component="a"
                        href="https://github.com/atlarge-research/opendc"
                        target="_blank"
                        aria-label="Source code"
                        variant={ButtonVariant.plain}
                    >
                        <GithubIcon />
                    </Button>
                </ToolbarItem>
                <ToolbarItem>
                    <Button
                        component="a"
                        href="https://opendc.org/"
                        target="_blank"
                        aria-label="Help actions"
                        variant={ButtonVariant.plain}
                    >
                        <HelpIcon />
                    </Button>
                </ToolbarItem>
            </ToolbarGroup>
            <ToolbarItem visibility={{ lg: 'hidden' }}>
                <Dropdown
                    isPlain
                    position="right"
                    toggle={<KebabToggle onToggle={toggleKebabDropdown} />}
                    isOpen={isKebabDropdownOpen}
                    dropdownItems={kebabDropdownItems}
                />
            </ToolbarItem>
        </ToolbarGroup>
    )
}

AppHeaderTools.propTypes = {}

export default AppHeaderTools
