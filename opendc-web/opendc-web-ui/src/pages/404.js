import React from 'react'
import Head from 'next/head'
import TerminalWindow from '../components/not-found/TerminalWindow'
import style from './404.module.scss'

const NotFound = () => {
    return (
        <>
            <Head>
                <title>Page Not Found - OpenDC</title>
            </Head>
            <div className={style['not-found-backdrop']}>
                <TerminalWindow />
            </div>
        </>
    )
}

export default NotFound
