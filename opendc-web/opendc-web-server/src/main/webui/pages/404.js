import React from 'react'
import Head from 'next/head'
import { AppPage } from '../components/AppPage'
import {
    Bullseye,
    EmptyState,
    EmptyStateBody,
    EmptyStateIcon,
    PageSection,
    PageSectionVariants,
    Title,
} from '@patternfly/react-core'
import { UnknownIcon } from '@patternfly/react-icons'

const NotFound = () => {
    return (
        <AppPage>
            <Head>
                <title>Page Not Found - OpenDC</title>
            </Head>
            <PageSection variant={PageSectionVariants.light}>
                <Bullseye>
                    <EmptyState>
                        <EmptyStateIcon variant="container" component={UnknownIcon} />
                        <Title size="lg" headingLevel="h4">
                            404: That page does not exist
                        </Title>
                        <EmptyStateBody>
                            The requested page is not found. Try refreshing the page if it was recently added.
                        </EmptyStateBody>
                    </EmptyState>
                </Bullseye>
            </PageSection>
        </AppPage>
    )
}

export default NotFound
