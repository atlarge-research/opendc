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

import React from 'react'
import clsx from 'clsx'

import styles from './styles.module.css'

const leads = [
    {
        name: 'Prof.dr.ir. Alexandru Iosup',
        title: 'Project Lead',
        avatar: 'https://www.atlarge-research.com/images/people/aiosup_large.png',
        url: 'https://www.atlarge-research.com/aiosup/',
    },
    {
        name: 'Fabian Mastenbroek',
        title: 'Technology Lead',
        avatar: 'https://www.atlarge-research.com/images/people/fmastenbroek_large.png',
        url: 'https://www.atlarge-research.com/fmastenbroek/',
    },
    {
        name: 'Georgios Andreadis',
        title: 'Former Technology Lead (2018-2020)',
        avatar: 'https://www.atlarge-research.com/images/people/gandreadis_large.png',
        url: 'https://www.atlarge-research.com/gandreadis/',
    },
    {
        name: 'Vincent van Beek',
        title: 'Former Technology Lead (2017-2018)',
        avatar: 'https://www.atlarge-research.com/images/people/vvanbeek_large.png',
        url: 'https://www.atlarge-research.com/vvanbeek/',
    },
]

const members = [
    {
        name: 'Matthijs Bijman',
        avatar: 'https://www.atlarge-research.com/images/people/mbijman_large.png',
        url: 'https://www.atlarge-research.com/mbijman/',
    },
    {
        name: 'Jaro Bosch',
        avatar: 'https://www.atlarge-research.com/images/people/jbosch_large.png',
        url: 'https://www.atlarge-research.com/jbosch/',
    },
    {
        name: 'Jacob Burley',
        avatar: 'https://www.atlarge-research.com/images/people/jburley_large.png',
        url: 'https://www.atlarge-research.com/jburley/',
    },
    {
        name: 'Erwin van Eyk',
        avatar: 'https://www.atlarge-research.com/images/people/evaneyk_large.png',
        url: 'https://www.atlarge-research.com/evaneyk/',
    },
    {
        name: 'Hongyu He',
        avatar: 'https://www.atlarge-research.com/images/people/hhe_large.png',
        url: 'https://www.atlarge-research.com/hhe/',
    },
    {
        name: 'Soufiane Jounaid',
        avatar: 'https://www.atlarge-research.com/images/people/sjounaid_large.png',
        url: 'https://www.atlarge-research.com/sjounaid/',
    },
    {
        name: 'Wenchen Lai',
        avatar: 'https://www.atlarge-research.com/images/people/wlai_large.png',
        url: 'https://www.atlarge-research.com/wlai/',
    },
    {
        name: 'Leon Overweel',
        avatar: 'https://www.atlarge-research.com/images/people/loverweel_large.png',
        url: 'https://www.atlarge-research.com/loverweel/',
    },

    {
        name: 'Sacheendra Talluri',
        avatar: 'https://www.atlarge-research.com/images/people/stalluri_large.png',
        url: 'https://www.atlarge-research.com/stalluri/',
    },
    {
        name: 'Laurens Versluis',
        avatar: 'https://www.atlarge-research.com/images/people/lfdversluis_large.png',
        url: 'https://www.atlarge-research.com/lfdversluis/',
    },
]

function TeamMember({ className, name, title, avatar, url, size = 'lg' }) {
    return (
        <div className={clsx('avatar avatar--vertical', styles.member, className)}>
            <a className={`avatar__photo-link avatar__photo avatar__photo--${size}`} href={url}>
                <img alt={`${name} Profile`} src={avatar} />
            </a>
            <div className={clsx(styles.memberIntro, 'avatar__intro')}>
                <div className="avatar__name">{name}</div>
                {title && <small className="avatar__subtitle">{title}</small>}
            </div>
        </div>
    )
}

export default function TeamMembers() {
    return (
        <div className="container">
            <div className={clsx(styles.members, 'row')}>
                {leads.map(({ name, title, avatar, url }) => (
                    <TeamMember
                        key={name}
                        className="col col--3"
                        name={name}
                        title={title}
                        avatar={avatar}
                        url={url}
                        size="xl"
                    />
                ))}
                {members.map(({ name, avatar, url }) => (
                    <TeamMember key={name} className="col col--2" name={name} avatar={avatar} url={url} />
                ))}
            </div>
        </div>
    )
}
