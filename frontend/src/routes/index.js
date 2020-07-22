import React from 'react'
import { BrowserRouter, Redirect, Route, Switch } from 'react-router-dom'
import { userIsLoggedIn } from '../auth/index'
import App from '../pages/App'
import Home from '../pages/Home'
import NotFound from '../pages/NotFound'
import Profile from '../pages/Profile'
import Projects from '../pages/Projects'

const ProtectedComponent = (component) => () => (userIsLoggedIn() ? component : <Redirect to="/" />)
const AppComponent = ({ match }) =>
    userIsLoggedIn() ? (
        <App
            projectId={match.params.projectId}
            portfolioId={match.params.portfolioId}
            scenarioId={match.params.scenarioId}
        />
    ) : (
        <Redirect to="/" />
    )

const Routes = () => (
    <BrowserRouter>
        <Switch>
            <Route exact path="/" component={Home} />
            <Route exact path="/projects" render={ProtectedComponent(<Projects />)} />
            <Route exact path="/projects/:projectId" component={AppComponent} />
            <Route exact path="/projects/:projectId/portfolios/:portfolioId" component={AppComponent} />
            <Route
                exact
                path="/projects/:projectId/portfolios/:portfolioId/scenarios/:scenarioId"
                component={AppComponent}
            />
            <Route exact path="/profile" render={ProtectedComponent(<Profile />)} />
            <Route path="/*" component={NotFound} />
        </Switch>
    </BrowserRouter>
)

export default Routes
