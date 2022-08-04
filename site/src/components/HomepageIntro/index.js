/*
 * Copyright (c) 2022 AtLarge Research
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

import clsx from 'clsx'
import React from 'react'

import styles from './styles.module.css'
import DatacenterImage from '@site/static/img/datacenter-drawing.png'

export default function HomepageInto() {
    return (
        <section id="intro" className={styles.intro}>
            <div className="container padding-vert--lg">
                <div className="row">
                    <div className={clsx('col col--4', styles.textCol)}>
                        <h3>The datacenter (DC) industry...</h3>
                        <ul>
                            <li>Is worth over $200 bn, and growing</li>
                            <li>Has many hard-to-grasp concepts</li>
                            <li>Needs to become accessible to many</li>
                        </ul>
                    </div>
                    <div className="col col--3 text--center">
                        <img src={DatacenterImage} alt="Schematic top-down view of a datacenter" />
                    </div>
                    <div className={clsx('col col--4', styles.textCol)}>
                        <h3>OpenDC provides...</h3>
                        <ul>
                            <li>Online cloud DC modeling</li>
                            <li>Diverse and effective DC simulation</li>
                            <li>Rapid exploratory DC feedback</li>
                        </ul>
                    </div>
                </div>
            </div>
        </section>
    )
}
