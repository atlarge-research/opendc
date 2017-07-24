import React from "react";
import ReactDOM from "react-dom";
import {BrowserRouter, Route, Switch} from "react-router-dom";
import "./index.css";
import registerServiceWorker from "./registerServiceWorker";
import Home from "./pages/Home";
import Projects from "./pages/Projects";
import NotFound from "./pages/NotFound";

ReactDOM.render(
    <BrowserRouter>
        <Switch>
            <Route exact path="/" component={Home}/>
            <Route exact path="/projects" component={Projects}/>
            <Route path="/*" component={NotFound}/>
        </Switch>
    </BrowserRouter>,
    document.getElementById('root')
);

registerServiceWorker();
