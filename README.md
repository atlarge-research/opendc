# OpenDC Frontend

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

The user-facing component of the OpenDC stack, allowing users to build and interact with their own (virtual) datacenters. Built in *React.js* and *Redux*, with the help of `create-react-app`.


## Architecture

The codebase follows a standard React.js structure, with static assets being contained in the `public` folder, while dynamic components and their styles are contained in `src`. The app uses client-side routing (with `react-router`), meaning that the only HTML file needed to be served is a `index.html` file.

### Pages

All pages are represented by a component in the `src/pages` directory. There are components for the following pages:

**Home.js** - Entry page (`/`)

**Simulations.js** - Overview of simulations the user (`/simulations`)

**App.js** - Main application, with datacenter construction and simulation UI (`/simulations/:simulationId` and `/simulations/:simulationId/experiments/:experimentId`)

**Experiments.js** - Overview of experiments of the current simulation (`/simulations/:simulationId/experiments`)

**Profile.js** - Profile of the current user (`/profile`)

**NotFound.js** - 404 page to appear when route is invalid (`/*`)

### Components & Containers

//

### State Management

//

### API Interaction

//
