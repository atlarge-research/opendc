import React from 'react'
import { Link } from 'react-router-dom'
import BlinkingCursor from './BlinkingCursor'
import CodeBlock from './CodeBlock'
import { terminalWindow, terminalHeader, terminalBody, segfault, subTitle, homeBtn } from './TerminalWindow.module.scss'

const TerminalWindow = () => (
    <div className={terminalWindow}>
        <div className={terminalHeader}>Terminal -- bash</div>
        <div className={terminalBody}>
            <div className={segfault}>
                $ status
                <br />
                opendc[4264]: segfault at 0000051497be459d1 err 12 in libopendc.9.0.4
                <br />
                opendc[4269]: segfault at 000004234855fc2db err 3 in libopendc.9.0.4
                <br />
                opendc[4270]: STDERR Page does not exist
                <br />
            </div>
            <CodeBlock />
            <div className={subTitle}>
                Got lost?
                <BlinkingCursor />
            </div>
            <Link to="/" className={homeBtn}>
                <span className="fa fa-home" /> GET ME BACK TO OPENDC
            </Link>
        </div>
    </div>
)

export default TerminalWindow
