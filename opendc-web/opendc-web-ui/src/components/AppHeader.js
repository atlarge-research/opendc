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

import React from 'react'
import {
    Masthead,
    MastheadMain,
    MastheadBrand,
    MastheadContent,
    Toolbar,
    ToolbarContent,
    ToolbarItem,
} from '@patternfly/react-core'
import Link from "next/link";
import AppHeaderTools from './AppHeaderTools'
import AppHeaderUser from './AppHeaderUser'
import ProjectSelector from './context/ProjectSelector'

import styles from './AppHeader.module.scss'

export function AppHeader() {
    return (
        <Masthead id="app-header">
            <MastheadMain>
                <MastheadBrand className={styles.logo} component={props => <Link href="/projects"><a {...props} /></Link>}>
                    <img src="/img/logo.svg" alt="OpenDC logo" width={30} height={30} />
                    <span>OpenDC</span>
                </MastheadBrand>
            </MastheadMain>
            <MastheadContent>
                <Toolbar id="toolbar" isFullHeight isStatic>
                    <ToolbarContent>
                        <ToolbarItem>
                            <ProjectSelector />
                        </ToolbarItem>
                        <AppHeaderTools />
                        <AppHeaderUser />
                    </ToolbarContent>
                </Toolbar>
            </MastheadContent>
        </Masthead>
    )
}

AppHeader.propTypes = {}
