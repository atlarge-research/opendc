import PropTypes from 'prop-types'
import React from 'react'
import { Button, EmptyState, EmptyStateBody, EmptyStateIcon, Modal, ModalVariant, Title } from '@patternfly/react-core'
import { CheckCircleIcon } from '@patternfly/react-icons'
import { Table, Thead, Tr, Th, Tbody, Td } from '@patternfly/react-table'
import { useJobReport } from '../../data/project'

function formatDuration(seconds) {
    if (seconds < 60) {
        return `${seconds}s`
    }
    const minutes = Math.floor(seconds / 60)
    const remainingSeconds = seconds % 60
    if (minutes < 60) {
        return `${minutes}m ${remainingSeconds}s`
    }
    const hours = Math.floor(minutes / 60)
    const remainingMinutes = minutes % 60
    return `${hours}h ${remainingMinutes}m ${remainingSeconds}s`
}

function JobReportModal({ jobId, isOpen, onClose }) {
    const { data: report, isLoading } = useJobReport(jobId, { enabled: isOpen })

    const logs = report?.logs || []
    const error = report?.error
    const summary = report?.summary
    const createdAt = report?.createdAt
    const startedAt = report?.startedAt

    const actions = [
        <Button variant="primary" onClick={onClose} key="close">
            Close
        </Button>,
    ]

    return (
        <Modal variant={ModalVariant.large} isOpen={isOpen} onClose={onClose} title="Job Report" actions={actions}>
            {isLoading && <div>Loading report...</div>}

            {!isLoading && (createdAt || startedAt) && (
                <div style={{ marginBottom: '20px', fontSize: '14px', color: '#6a6e73' }}>
                    {createdAt && (
                        <div>
                            <strong>Created:</strong> {new Date(createdAt).toLocaleString()}
                        </div>
                    )}
                    {startedAt && (
                        <div>
                            <strong>Started:</strong> {new Date(startedAt).toLocaleString()}
                        </div>
                    )}
                </div>
            )}

            {!isLoading && logs.length === 0 && !error && (
                <>
                    {summary && (summary.runtimeSeconds !== undefined || summary.waitTimeSeconds !== undefined) && (
                        <div style={{ marginBottom: '15px' }}>
                            {summary.runtimeSeconds !== undefined && (
                                <div>
                                    <strong>Runtime:</strong> {formatDuration(summary.runtimeSeconds)}
                                </div>
                            )}
                            {summary.waitTimeSeconds !== undefined && (
                                <div>
                                    <strong>Queue Wait Time:</strong> {formatDuration(summary.waitTimeSeconds)}
                                </div>
                            )}
                        </div>
                    )}
                    <EmptyState>
                        <EmptyStateIcon icon={CheckCircleIcon} color="green" />
                        <Title headingLevel="h4" size="lg">
                            No warnings or errors
                        </Title>
                        <EmptyStateBody>This simulation completed successfully with no issues.</EmptyStateBody>
                    </EmptyState>
                </>
            )}

            {!isLoading && error && (
                <div style={{ marginBottom: '20px' }}>
                    <Title headingLevel="h3" size="md">
                        Error
                    </Title>
                    <div
                        style={{
                            padding: '10px',
                            backgroundColor: '#fef0f0',
                            border: '1px solid #c9190b',
                            borderRadius: '3px',
                            marginTop: '10px',
                        }}
                    >
                        <div>
                            <strong>Type:</strong> {error.type}
                        </div>
                        <div>
                            <strong>Message:</strong> {error.message}
                        </div>
                        {error.stackTrace && (
                            <details style={{ marginTop: '10px' }}>
                                <summary style={{ cursor: 'pointer' }}>Stack Trace</summary>
                                <pre style={{ fontSize: '12px', overflow: 'auto', maxHeight: '200px' }}>
                                    {error.stackTrace}
                                </pre>
                            </details>
                        )}
                    </div>
                </div>
            )}

            {!isLoading && logs.length > 0 && (
                <div>
                    {summary && (
                        <div style={{ marginBottom: '15px' }}>
                            <strong>Summary:</strong> {summary.totalWarnings} warning(s), {summary.totalErrors} error(s)
                            {summary.runtimeSeconds !== undefined && (
                                <>
                                    {' | '}
                                    <strong>Runtime:</strong> {formatDuration(summary.runtimeSeconds)}
                                </>
                            )}
                            {summary.waitTimeSeconds !== undefined && (
                                <>
                                    {' | '}
                                    <strong>Queue Wait Time:</strong> {formatDuration(summary.waitTimeSeconds)}
                                </>
                            )}
                        </div>
                    )}
                    <Table variant="compact">
                        <Thead>
                            <Tr>
                                <Th>Time</Th>
                                <Th>Level</Th>
                                <Th>Logger</Th>
                                <Th>Message</Th>
                            </Tr>
                        </Thead>
                        <Tbody>
                            {logs.map((log, index) => (
                                <Tr key={index}>
                                    <Td>{new Date(log.timestamp).toLocaleTimeString()}</Td>
                                    <Td>
                                        <span
                                            style={{
                                                color: log.level === 'ERROR' ? '#c9190b' : '#f0ab00',
                                                fontWeight: 'bold',
                                            }}
                                        >
                                            {log.level}
                                        </span>
                                    </Td>
                                    <Td style={{ fontSize: '12px' }}>{log.logger}</Td>
                                    <Td>{log.message}</Td>
                                </Tr>
                            ))}
                        </Tbody>
                    </Table>
                </div>
            )}
        </Modal>
    )
}

JobReportModal.propTypes = {
    jobId: PropTypes.number.isRequired,
    isOpen: PropTypes.bool,
    onClose: PropTypes.func.isRequired,
}

JobReportModal.defaultProps = {
    isOpen: false,
}

export default JobReportModal
