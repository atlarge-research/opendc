# OpenDC docs site

This branch contains the sources for the [OpenDC website](https://opendc.org). The website is built
using [Docusaurus 2](https://docusaurus.io/), a modern static website generator.

To run the website locally, you need to have [Node.js](https://nodejs.org/en/) installed.
To start the website, run the following commands:

### Installation

```
$ npm i
```

### Local Development

```
$ npm run start
```

This command starts a local development server and opens up a browser window. Most changes are reflected live without having to restart the server.

### Build

```
$ npm run build
```

This command generates static content into the `build` directory and can be served using any static contents hosting service.

### Deployment

The site is automatically deployed using GitHub Actions.
