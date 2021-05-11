import React from 'react'
import TerminalWindow from '../components/not-found/TerminalWindow'
import style from './NotFound.module.scss'
import { useDocumentTitle } from '../util/hooks'

const NotFound = () => {
    useDocumentTitle('Page Not Found - OpenDC')
    return (
        <div className={style['not-found-backdrop']}>
            <TerminalWindow />
        </div>
    )
}

export default NotFound
