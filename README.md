# OpenDC Frontend

The OpenDC frontend is the user-facing component of the OpenDC stack, allowing users to build and interact with their own (virtual) datacenters. It is built in TypeScript, using CreateJS for canvas interactions and Gulp for build-automation.

This document gives a high-level view of the frontend architecture ([jump](#architecture)), and describes how to set it up for local development ([jump](#setup-for-local-development)).

## Architecture

### Application Interaction Model
Let's first take a look at how the user interacts with the frontend. 

#### Pages
The user starts at the splashpage (`index.html`) where he/she gets a first impression of OpenDC, including screenshots and features. After signing in with Google Authentication, the user is redirected to the page listing all projects (`projects.html`) shared or owned by that user. Here the user also has the possibility to open a particular project, redirecting to the main application page (`app.html`).

#### Main Application
The main application allows the user to construct and simulate a datacenter. To understand how the user can do this, have a look at the state diagram below. It visualizes the main interactions a user can make with the application, as well as under which conditions those can happen.

![OpenDC Frontend Interaction State Diagram](https://raw.githubusercontent.com/atlarge-research/opendc-frontend/master/images/opendc-frontend-interaction-state-diagram.png)

### Components
Under the hood, this looks as follows:

![OpenDC Frontend Component Diagram](https://raw.githubusercontent.com/atlarge-research/opendc-frontend/master/images/opendc-frontend-component-diagram.png)

#### Entry Scripts
The entry scripts are the first entities triggered on page load of the respective pages. They are responsible for instantiating the corresponding view and controller.

#### Controllers
In the main web-application, the controllers handle user input and state. They also initiate re-renders of views when a part of their managed state changes. There are different classes of controllers; some concerning themselves with the API connection, others with the different modes of interaction, and others with simulation-specific actions. This all is orchestrated by a central class called the `MapController`.

#### Views
The views are responsible for drawing content to the canvas. They are split up into different layers, corresponding to the way they are rendered.

## Setup for Local Development

### Initial setup

#### Setting up the server
To be able to see the frontend run in your browser, first set up the web server. The steps needed for this are listed on [the `opendc-web-server` repo](https://github.com/atlarge-research/opendc-web-server).

Once the web-server is set up, clone [this frontend-repo](https://github.com/atlarge-research/opendc-frontend.git) into the same base-directory as the repos you cloned during server setup:

```bash
git clone https://github.com/atlarge-research/opendc-frontend.git
```

Change directory to that new directory, and you're ready to continue to the frontend setup steps below.

#### Resolving dependencies
We use the NPM package repository to manage our third-party dependencies on the frontend. To fetch and install these dependencies, you'll need to have the [Node.js](https://nodejs.org/en/) environment installed. 

For easier fetching, we recommend the [Yarn Package Manager](https://yarnpkg.com), but the standard NPM tool will suffice, too. You can get your build setup installed by executing the following two commands:

```bash
npm install -g yarn
npm install -g gulp
```
   
You may need to prepend these commands with `sudo`, if you are on a Debian-based Linux machine. If you're having trouble giving NPM the necessary permissions on such a machine, have a look at [this NPM documentation page](https://docs.npmjs.com/getting-started/fixing-npm-permissions).

### Building the project
Run the following commands from this directory to fetch dependencies and compile the code of the frontend side:

```bash
yarn
gulp --config=config.json
```

**Note:** You need to replace `config.json` with the name / path of a real config file. This config file can be created by making a copy of the `sample_config.json` template and replacing the entries with your setup data. Make sure not to check this new config file into the VCS, as it is unique to each deployment situation.

### Automatically triggered builds
To make development easier, we've set up a `watch` task. With this task, you can quickly see what effects a certain change has on the program. It runs in the background, automatically triggering a rebuild of relevant files on file-change.

Start it by executing:

```bash
gulp watch --config=config.json
```
