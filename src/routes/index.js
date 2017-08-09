import React from 'react';
import {BrowserRouter, Route, Switch} from "react-router-dom";
import Home from "../pages/Home";
import NotFound from "../pages/NotFound";
import Projects from "../pages/Projects";

const Routes = () => (
    <BrowserRouter>
        <Switch>
            <Route exact path="/" component={Home}/>
            <Route exact path="/projects" component={Projects}/>
            <Route path="/*" component={NotFound}/>
        </Switch>
    </BrowserRouter>
);

export default Routes;
