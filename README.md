# OpenDC Front End
## Initial setup
We use the NPM package repository to manage our third-party dependencies on the frontend. To fetch and install these dependencies, you'll need to have the [Node.js](https://nodejs.org/en/) environment installed. 

For easier fetching, we recommend the [Yarn Package Manager](https://yarnpkg.com), but the standard NPM tool will suffice, too. You can get your build setup installed by executing the following two commands:

    $ npm install -g yarn
    $ npm install -g gulp
    
You may need to prepend these commands with `sudo`, if you are on a Debian-based Linux machine. If you're having trouble giving NPM the necessary permissions on such a machine, have a look at [this NPM documentation page](https://docs.npmjs.com/getting-started/fixing-npm-permissions).

## Building the project
Run the following commands from this directory to fetch dependencies and compile the code of the frontend side:

```
$ yarn
$ gulp
```
