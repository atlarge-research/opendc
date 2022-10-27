# OpenDC Web UI

The user-facing component of the OpenDC stack, allowing users to build and interact with their own (virtual)
datacenters. Built in *React.js* and *Redux*, with the help of [Next.js](https://nextjs.org/).

## Architecture

The codebase follows a standard React.js structure, with static assets being contained in the `public` folder, while
dynamic components and their styles are contained in `src`.

### Pages

All pages are represented by a component in the `src/pages` directory, following
the [Next.js conventions](https://nextjs.org/docs/routing/introduction) for routing. There are components for the
following pages:

1. **index.js** - Entry page (`/`)
2. **projects/index.js** - Overview of projects of the user (`/projects`)
3. **projects/[project]/index.js** - Main application, with datacenter construction and simulation UI (`/projects/:projectId`
and `/projects/:projectId/portfolios/:portfolioId`)
4. **profile.js** - Profile of the current user (`/profile`)
5. **404.js** - 404 page to appear when the route is invalid (`/*`)

### Components & Containers

The building blocks of the UI are divided into so-called *components* and *containers* 
([as encouraged](https://medium.com/@dan_abramov/smart-and-dumb-components-7ca2f9a7c7d0) by the author of Redux). 
*Components* are considered 'pure', rendered as a function of input properties. *Containers*, on the other hand,
are wrappers around *components*, injecting state through the properties of the components they wrap.

Even the canvas (the main component of the app) is built using React components, with the help of the `react-konva`
module. To illustrate: A rectangular object on the canvas is defined in a way that is not very different from how we
define a standard `div` element on the splash page.

### API Interaction

The web-app needs to pull data in from the API of a backend running on a server. The functions that call routes are
located in `src/api`. The actual logic responsible for calling these functions is contained in `src/data`.

### State Management

State for the topology editor is managed via a Redux store. State is kept there in an immutable form, only to be modified through
actions being dispatched. These actions are contained in the `src/actions` folder, and the reducers (managing how state
is updated according to dispatched actions) are located in `src/reducers`. If you're not familiar with the Redux
approach to state management, have a look at their [official documentation](https://redux.js.org/).

## Running the development server

Before we can start the development server, create a file called `.env` in this directory and specify the base URL of
the API that the React frontend will communicate with. For instance, if you run the OpenDC development server:

```
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
```

Now, you're ready to start the Next.js development server. Run the following command in the root of the repository
(that is, two levels up where the `gradlew` file is located):

```bash
./gradlew :opendc-web:opendc-web-ui:nextDev
```

This will start a development server running on [`localhost:3000`](http://localhost:3000), watching for changes you make
to the code and rebuilding automatically when you save changes.

To compile everything for camera-ready deployment, use the following command:

```bash
./gradlew :opendc-web:opendc-web-ui:build
```

You can then run the production server using Next.js as follows:

```bash
./gradlew :opendc-web:opendc-web-ui:nextStart
```

## Tests

Files containing tests can be recognized by the `.test.js` suffix. They are usually located right next to the source
code they are testing, to make discovery easier.

### Running all tests

The following command runs all tests in the codebase using [Jest](https://jest.io). On top of this, it also watches the
code for changes and reruns the tests whenever any file is saved.

```bash
./gradlew :opendc-web:opendc-web-ui:test
```

## Code Quality

We use [Prettier](https://prettier.io) to ensure the formatting of the JavaScript codebase remains consistent. To format
the files of the codebase according to the preferred coding style, run the following command:

```bash
./gradlew :opendc-web:opendc-web-ui:prettierFormat
```

Furthermore, we also employ [ESLint](https://eslint.org/) (via Next) to detect issues and problematic code in our
codebase. To check for potential issues, run the following command:

```bash
./gradlew :opendc-web:opendc-web-ui:nextLint
```
