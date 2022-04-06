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
    Avatar,
    Button,
    ButtonVariant,
    Dropdown,
    DropdownGroup,
    DropdownItem,
    DropdownToggle,
    KebabToggle,
    PageHeaderTools,
    PageHeaderToolsGroup,
    PageHeaderToolsItem,
    Skeleton,
} from '@patternfly/react-core'
import { useState } from 'react'
import { useAuth } from '../auth'
import { GithubIcon, HelpIcon } from '@patternfly/react-icons'

function AppHeaderTools() {
    const { logout, user, isAuthenticated, isLoading } = useAuth()
    const username = isAuthenticated || isLoading ? user?.name : 'Anonymous'
    const avatar = isAuthenticated || isLoading ? user?.picture : '/img/avatar.svg'

    const [isKebabDropdownOpen, setKebabDropdownOpen] = useState(false)
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

    const [isDropdownOpen, setDropdownOpen] = useState(false)
    const userDropdownItems = [
        <DropdownGroup key="group 2">
            <DropdownItem
                key="group 2 logout"
                isDisabled={!isAuthenticated}
                onClick={() => logout({ returnTo: window.location.origin })}
            >
                Logout
            </DropdownItem>
        </DropdownGroup>,
    ]

    return (
        <PageHeaderTools>
            <PageHeaderToolsGroup visibility={{ default: 'hidden', lg: 'visible' }}>
                <PageHeaderToolsItem>
                    <Button
                        component="a"
                        href="https://github.com/atlarge-research/opendc"
                        target="_blank"
                        aria-label="Source code"
                        variant={ButtonVariant.plain}
                    >
                        <GithubIcon />
                    </Button>
                </PageHeaderToolsItem>
                <PageHeaderToolsItem>
                    <Button
                        component="a"
                        href="https://opendc.org/"
                        target="_blank"
                        aria-label="Help actions"
                        variant={ButtonVariant.plain}
                    >
                        <HelpIcon />
                    </Button>
                </PageHeaderToolsItem>
            </PageHeaderToolsGroup>
            <PageHeaderToolsGroup>
                <PageHeaderToolsItem visibility={{ lg: 'hidden' }}>
                    <Dropdown
                        isPlain
                        position="right"
                        toggle={<KebabToggle onToggle={() => setKebabDropdownOpen(!isKebabDropdownOpen)} />}
                        isOpen={isKebabDropdownOpen}
                        dropdownItems={kebabDropdownItems}
                    />
                </PageHeaderToolsItem>
                <PageHeaderToolsItem visibility={{ default: 'hidden', md: 'visible' }}>
                    <Dropdown
                        isPlain
                        position="right"
                        isOpen={isDropdownOpen}
                        toggle={
                            <DropdownToggle onToggle={() => setDropdownOpen(!isDropdownOpen)}>
                                {username ?? (
                                    <Skeleton
                                        fontSize="xs"
                                        width="150px"
                                        className="pf-u-display-inline-flex"
                                        screenreaderText="Loading username"
                                    />
                                )}
                            </DropdownToggle>
                        }
                        dropdownItems={userDropdownItems}
                    />
                </PageHeaderToolsItem>
            </PageHeaderToolsGroup>
            {avatar ? (
                <Avatar src={avatar} alt="Avatar image" />
            ) : (
                <Skeleton className="pf-c-avatar" shape="circle" width="2.25rem" screenreaderText="Loading avatar" />
            )}
        </PageHeaderTools>
    )
}

AppHeaderTools.propTypes = {}

export default AppHeaderTools
