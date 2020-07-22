import React from 'react'
import DocumentTitle from 'react-document-title'
import TerminalWindow from '../components/not-found/TerminalWindow'
import './NotFound.css'

const NotFound = () => (
    <DocumentTitle title="Page Not Found - OpenDC">
        <div className="not-found-backdrop">
            <TerminalWindow />
        </div>
    </DocumentTitle>
)

export default NotFound
