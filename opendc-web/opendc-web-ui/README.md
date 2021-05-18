<h1 align="center">
    <img src="../../docs/images/logo.png" width="100" alt="OpenDC">
    <br>
    OpenDC Frontend
</h1>
<p align="center">
    Collaborative Datacenter Simulation and Exploration for Everybody
</p>

The user-facing component of the OpenDC stack, allowing users to build and interact with their own (virtual)
datacenters. Built in *React.js* and *Redux*, with the help of [Next.js](https://nextjs.org/).

## Get Up and Running

Looking for the full OpenDC stack? Check out the [deployment guide](../../docs/deploy.md) for instructions on
how to set up a Docker container with all of OpenDC, without the hassle of running each of the components manually.

### Installation

To get started, you'll need the [Node.js environment](https://nodejs.org) and
the [Yarn package manager](https://yarnpkg.com). Once you have those installed, run the following command from the root
directory of this repo:

```bash
yarn
```

### Running the development server

First, you need to set up an [Auth0](https://auth0.com) application. Check
the [documentation in the deployment guide](../../docs/deploy.md) if you're not sure how to do this. Once you have such
an ID, you need to set it as environment variable `NEXT_PUBLIC_AUTH0_CLIENT_ID` and `NEXT_PUBLIC_AUTH0_DOMAIN`
One way of doing this is to create an `.env.local` file with content `NEXT_PUBLIC_AUTH0_CLIENT_ID=YOUR_ID` and
`NEXT_PUBLIC_AUTH0_DOMAIN=YOUR_AUTH0_DOMAIN` in the root directory of this repo.

Once you've set this variable, start the OpenDC `docker-compose` setup. See the root README for instructions on this.

Now, you're ready to start the development server:

```bash
yarn dev
```

This will start a development server running on [`localhost:3000`](http://localhost:3000), watching for changes you make
to the code and rebuilding automatically when you save changes.

To compile everything for camera-ready deployment, use the following command:

```bash
yarn build
```

You can run the production server using Next.js as follows:

```bash
yarn start
```

## Architecture

The codebase follows a standard React.js structure, with static assets being contained in the `public` folder, while
dynamic components and their styles are contained in `src`.

### Pages

All pages are represented by a component in the `src/pages` directory, following
the [Next.js conventions](https://nextjs.org/docs/routing/introduction) for routing. There are components for the
following pages:

**index.js** - Entry page (`/`)

**projects/index.js** - Overview of projects of the user (`/projects`)

**projects/[project]/index.js** - Main application, with datacenter construction and simulation UI (`/projects/:projectId`
and `/projects/:projectId/portfolios/:portfolioId`)

**profile.js** - Profile of the current user (`/profile`)

**404.js** - 404 page to appear when the route is invalid (`/*`)

### Components & Containers

The building blocks of the UI are divided into so-called *components* and *
containers* ([as encouraged](https://medium.com/@dan_abramov/smart-and-dumb-components-7ca2f9a7c7d0) by the author of
Redux). *Components* are considered 'pure', rendered as a function of input properties. *Containers*, on the other hand,
are wrappers around *components*, injecting state through the properties of the components they wrap.

Even the canvas (the main component of the app) is built using React components, with the help of the `react-konva`
module. To illustrate: A rectangular object on the canvas is defined in a way that is not very different from how we
define a standard `div` element on the splashpage.

### State Management

Almost all state is kept in a central Redux store. State is kept there in an immutable form, only to be modified through
actions being dispatched. These actions are contained in the `src/actions` folder, and the reducers (managing how state
is updated according to dispatched actions) are located in `src/reducers`. If you're not familiar with the Redux
approach to state management, have a look at their [official documentation](https://redux.js.org/).

### API Interaction

The web-app needs to pull data in from the API of a backend running on a server. The functions that call routes are
located in `src/api`. The actual logic responsible for calling these functions is contained in `src/sagas`. These API
fetch procedures are written with the help of `redux-saga`. The [official documentation](https://redux-saga.js.org/)
of `redux-saga` can be a helpful aid in understanding that part of the codebase.

## Tests

Files containing tests can be recognized by the `.test.js` suffix. They are usually located right next to the source
code they are testing, to make discovery easier.

### Running all tests

The following command runs all tests in the codebase. On top of this, it also watches the code for changes and reruns
the tests whenever any file is saved.

```bash
yarn test
```
