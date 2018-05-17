<h1 align="center">
  <img src="public/img/logo.png" width="100" alt="OpenDC">
  <br>
  OpenDC Frontend
</h1>
<p align="center">
  Collaborative Datacenter Simulation and Exploration for Everybody
</p>

<p align="center">
  <a href="https://travis-ci.org/atlarge-research/opendc-frontend"><img src="https://travis-ci.org/atlarge-research/opendc-frontend.svg?branch=master" alt="Build Status"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License: MIT"></a>
  <a href="https://github.com/prettier/prettier"><img src="https://img.shields.io/badge/styled_with-prettier-ff69b4.svg" alt="styled with prettier"></a><br/>
  <a href="https://waffle.io/atlarge-research/opendc-frontend?utm_source=badge"><img src="https://badge.waffle.io/atlarge-research/opendc-frontend.png?columns=all" alt="Waffle.io - Columns and their card count"></a>
</p>

The user-facing component of the OpenDC stack, allowing users to build and interact with their own (virtual) datacenters. Built in *React.js* and *Redux*, with the help of `create-react-app`.


## Get Up and Running

Looking for the full OpenDC stack? Check out [the main OpenDC repo](https://github.com/atlarge-research/opendc) for instructions on how to set up a Docker container with all of OpenDC, without the hassle of running each of the components manually.

### Installation

To get started, you'll need the [Node.js environment](https://nodejs.org) and the [Yarn package manager](https://yarnpkg.com). Once you have those installed, run the following command from the root directory of this repo:

```bash
yarn
```

### Running the development server

First, you need to have a Google OAuth client ID set up. Check the [documentation of the main OpenDC repo](https://github.com/atlarge-research/opendc) if you're not sure how to do this. Once you have such an ID, you need to set it as environment variable `REACT_APP_OAUTH_CLIENT_ID`. One way of doing this is to create an `.env` file with content `REACT_APP_OAUTH_CLIENT_ID=YOUR_ID` (`YOUR_ID` without quotes), in the root directory of this repo.

Once you've set this variable, you're ready to start the development server:

```bash
yarn start
```

This will start a development server running on [`localhost:3000`](http://localhost:3000), watching for changes you make to the code and rebuilding automatically when you save changes.

To compile everything for camera-ready deployment, use the following command:

```bash
yarn build
```

**Note:** Perhaps this goes without saying, but for any functionality beyond visiting the entry page, a server backend running in the background is necessary. The easiest way to do this is to have an OpenDC docker container running, see [the main repo](https://github.com/atlarge-research/opendc) for more information on how to do this.


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

The building blocks of the UI are divided into so-called *components* and *containers* ([as encouraged](https://medium.com/@dan_abramov/smart-and-dumb-components-7ca2f9a7c7d0) by the author of Redux). *Components* are considered 'pure', rendered as a function of input properties. *Containers*, on the other hand, are wrappers around *components*, injecting state through the properties of the components they wrap.

Even the canvas (the main component of the app) is built using React components, with the help of the `react-konva` module. To illustrate: A rectangular object on the canvas is defined in a way that is not very different from how we define a standard `div` element on the splashpage.

### State Management

Almost all state is kept in a central Redux store. State is kept there in an immutable form, only to be modified through actions being dispatched. These actions are contained in the `src/actions` folder, and the reducers (managing how state is updated according to dispatched actions) are located in `src/reducers`. If you're not familiar with the Redux approach to state management, have a look at their [official documentation](http://redux.js.org/).

### API Interaction

The web-app needs to pull data in from the API of a backend running on a server. The functions that call routes are located in `src/api`. The actual logic responsible for calling these functions is contained in `src/sagas`. These API fetch procedures are written with the help of `redux-saga`. The [official documentation](https://redux-saga.js.org/) of `redux-saga` can be a helpful aid in understanding that part of the codebase.


## Tests

Files containing tests can be recognized by the `.test.js` suffix. They are usually located right next to the source code they are testing, to make discovery easier.

### Running all tests

The following command runs all tests in the codebase. On top of this, it also watches the code for changes and reruns the tests whenever any file is saved.

```bash
yarn test
```


## License

The code is released under the MIT license. See `LICENSE.md`.
