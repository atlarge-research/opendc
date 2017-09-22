# OpenDC Frontend

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The user-facing component of the OpenDC stack, allowing users to build and interact with their own (virtual) datacenters. Built in *React.js* and *Redux*, with the help of `create-react-app`.


## Architecture

The codebase follows a standard React.js structure, with static assets being contained in the `public` folder, while dynamic components and their styles are contained in `src`. The app uses client-side routing (with `react-router`), meaning that the only HTML file needed to be served is a `index.html` file.

### Pages

All pages are represented by a component in the `src/pages` directory. There are components for the following pages:

**Home.js** - Entry page (`/`)

**Simulations.js** - Overview of simulations of the user (`/simulations`)

**App.js** - Main application, with datacenter construction and simulation UI (`/simulations/:simulationId` and `/simulations/:simulationId/experiments/:experimentId`)

**Experiments.js** - Overview of experiments of the current simulation (`/simulations/:simulationId/experiments`)

**Profile.js** - Profile of the current user (`/profile`)

**NotFound.js** - 404 page to appear when the route is invalid (`/*`)

### Components & Containers

The building blocks of the UI are divided into so-called *components* and *containers* (as [encouraged](https://medium.com/@dan_abramov/smart-and-dumb-components-7ca2f9a7c7d0) by the author of Redux). *Components* are considered 'pure', rendered as a function of input properties. *Containers*, on the other hand, are wrappers around *components*, injecting state through the properties of the components they wrap.

### State Management

Almost all state is kept in a central Redux store. State is kept there in an immutable form, only to be modified through actions being dispatched. These actions are contained in the `src/actions` folder, and the reducers (managing how state is updated according to dispatched actions) are located in `src/reducers`. If you're not familiar with the Redux approach to state management, have a look at [their official documentation](http://redux.js.org/).

### API Interaction

The web-app needs to pull data in from the API of a backend running on a server. The functions that call routes are located in `src/api`. The actual logic responsible for calling these functions is contained in `src/sagas`. These API fetch procedures are written with the help of `redux-saga`. Learn more about this way of getting API data in Redux on [their official documentation](https://redux-saga.js.org/).
