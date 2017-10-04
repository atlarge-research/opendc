import React from "react";
import ReactDOM from "react-dom";
import { Provider } from "react-redux";
import { setupSocketConnection } from "./api/socket";
import "./index.css";
import registerServiceWorker from "./registerServiceWorker";
import Routes from "./routes";
import configureStore from "./store/configure-store";

setupSocketConnection(() => {
  const store = configureStore();

  ReactDOM.render(
    <Provider store={store}>
      <Routes />
    </Provider>,
    document.getElementById("root")
  );

  registerServiceWorker();
});
