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

import { Dropdown, DropdownItem, DropdownToggle, Nav, NavItem, NavList } from '@patternfly/react-core'
import { useRouter } from 'next/router'
import NavItemLink from './util/NavItemLink'
import { useProject } from '../data/project'

export function AppNavigation() {
    const { pathname, query } = useRouter()
    const { project: projectId } = query
    const { data: project } = useProject(projectId)

    const nextTopologyId = project?.topologyIds?.[0]
    const nextPortfolioId = project?.portfolioIds?.[0]

    return (
        <Nav variant="horizontal">
            <NavList>
                <NavItem
                    id="projects"
                    to="/projects"
                    itemId={0}
                    component={NavItemLink}
                    isActive={pathname === '/projects' || pathname === '/projects/[project]'}
                >
                    Projects
                </NavItem>
                {pathname.startsWith('/projects/[project]') && (
                    <>
                        <NavItem
                            id="topologies"
                            to={nextTopologyId ? `/projects/${projectId}/topologies/${nextTopologyId}` : '/projects'}
                            itemId={1}
                            component={NavItemLink}
                            isActive={pathname === '/projects/[project]/topologies/[topology]'}
                        >
                            Topologies
                        </NavItem>
                        <NavItem
                            id="portfolios"
                            to={nextPortfolioId ? `/projects/${projectId}/portfolios/${nextPortfolioId}` : '/projects'}
                            itemId={2}
                            component={NavItemLink}
                            isActive={pathname === '/projects/[project]/portfolios/[portfolio]'}
                        >
                            Portfolios
                        </NavItem>
                    </>
                )}
            </NavList>
        </Nav>
    )
}

AppNavigation.propTypes = {}
