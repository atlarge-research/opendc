import React from "react";
import ReactDOM from "react-dom";
import {Provider} from "react-redux";
import "./index.css";
import registerServiceWorker from "./registerServiceWorker";
import Routes from "./routes";
import configureStore from "./store/configureStore";

const store = configureStore();

ReactDOM.render(
    <Provider store={store}>
        <Routes/>
    </Provider>,
    document.getElementById('root')
);

registerServiceWorker();
