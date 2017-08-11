import React from 'react';
import {BrowserRouter, Redirect, Route, Switch} from "react-router-dom";
import {userIsLoggedIn} from "../auth/index";
import Home from "../pages/Home";
import NotFound from "../pages/NotFound";
import Projects from "../pages/Projects";

const Routes = () => (
    <BrowserRouter>
        <Switch>
            <Route exact path="/" component={Home}/>
            <Route exact path="/projects" render={() => (
                userIsLoggedIn() ? (
                    <Projects/>
                ) : (
                    <Redirect to="/"/>
                )
            )}/>
            <Route path="/*" component={NotFound}/>
        </Switch>
    </BrowserRouter>
);

export default Routes;
