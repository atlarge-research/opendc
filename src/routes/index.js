import React from 'react';
import {BrowserRouter, Redirect, Route, Switch} from "react-router-dom";
import {userIsLoggedIn} from "../auth/index";
import Home from "../pages/Home";
import NotFound from "../pages/NotFound";
import Profile from "../pages/Profile";
import Simulations from "../pages/Simulations";

const ProtectedComponent = (component) => () => userIsLoggedIn() ? component : <Redirect to="/"/>;

const Routes = () => (
    <BrowserRouter>
        <Switch>
            <Route exact path="/" component={Home}/>
            <Route exact path="/simulations" render={ProtectedComponent(<Simulations/>)}/>
            <Route exact path="/profile" render={ProtectedComponent(<Profile/>)}/>
            <Route path="/*" component={NotFound}/>
        </Switch>
    </BrowserRouter>
);

export default Routes;
