import React from 'react'
import { BrowserRouter, Redirect, Route, Switch } from 'react-router-dom'
import { userIsLoggedIn } from '../auth/index'
import App from '../pages/App'
import Experiments from '../pages/Experiments'
import Home from '../pages/Home'
import NotFound from '../pages/NotFound'
import Profile from '../pages/Profile'
import Simulations from '../pages/Simulations'

const ProtectedComponent = (component) => () => (userIsLoggedIn() ? component : <Redirect to="/" />)
const AppComponent = ({ match }) =>
    userIsLoggedIn() ? <App simulationId={match.params.simulationId} /> : <Redirect to="/" />

const ExperimentsComponent = ({ match }) =>
    userIsLoggedIn() ? <Experiments simulationId={match.params.simulationId} /> : <Redirect to="/" />

const SimulationComponent = ({ match }) =>
    userIsLoggedIn() ? (
        <App simulationId={match.params.simulationId} inSimulation={true} experimentId={match.params.experimentId} />
    ) : (
        <Redirect to="/" />
    )

const Routes = () => (
    <BrowserRouter>
        <Switch>
            <Route exact path="/" component={Home} />
            <Route exact path="/simulations" render={ProtectedComponent(<Simulations />)} />
            <Route exact path="/simulations/:simulationId" component={AppComponent} />
            <Route exact path="/simulations/:simulationId/experiments" component={ExperimentsComponent} />
            <Route exact path="/simulations/:simulationId/experiments/:experimentId" component={SimulationComponent} />
            <Route exact path="/profile" render={ProtectedComponent(<Profile />)} />
            <Route path="/*" component={NotFound} />
        </Switch>
    </BrowserRouter>
)

export default Routes
