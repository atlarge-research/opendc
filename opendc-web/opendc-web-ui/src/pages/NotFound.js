import React from 'react'
import TerminalWindow from '../components/not-found/TerminalWindow'
import './NotFound.sass'
import { useDocumentTitle } from '../util/hooks'

const NotFound = () => {
    useDocumentTitle('Page Not Found - OpenDC')
    return (
        <div className="not-found-backdrop">
            <TerminalWindow />
        </div>
    )
}

export default NotFound
