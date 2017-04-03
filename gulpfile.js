/**
 * Build file for the frontend codebase of OpenDC.
 *
 * Usage:
 *  $ gulp --config=config.json        # for a single build
 *  $ gulp watch --config=config.json  # to run once, watch for changes, and rebuild when something changed
 *
 * If the `config` argument is omitted, the config file is assumed to be named `config.json` and present in this
 * directory.
 */

'use strict';

const argv = require('yargs').argv;

const gulp = require('gulp');
const notify = require('gulp-notify');
const gulpUtil = require('gulp-util');
const rename = require('gulp-rename');
const replace = require('gulp-replace');
const del = require('del');
const fs = require('fs');
const runSequence = require('run-sequence');
const source = require('vinyl-source-stream');
const es = require('event-stream');
const less = require('gulp-less');
const browserify = require('browserify');
const watchify = require('watchify');
const tsify = require('tsify');
const gulpTypings = require("gulp-typings");
const processHTML = require('gulp-processhtml');
const bower = require('gulp-bower');


/**
 * Checks whether the configuration file is specified and reads its contents.
 *
 * @throws an Exception if the config file could not be found or read (logs appropriately to the console)
 * @returns {Object} the config file contents.
 */
function getConfigFile() {
    let configInput = argv.config;

    if (configInput === undefined) {
        if (!fs.existsSync("./config.json")) {
            gulpUtil.log(gulpUtil.colors.red('Config file argument missing\n'), 'Usage:\n' +
                ' $ gulp --config=config.json');
            throw new Exception();
        } else {
            gulpUtil.log(gulpUtil.colors.magenta('No config file argument, assuming `config.json`.'));
            configInput = "config.json";
        }
    }

    try {
        let configFilePath;
        if (configInput.indexOf('/') === -1) {
            configFilePath = './' + configInput;
        } else {
            configFilePath = configInput;
        }

        return require(configFilePath);
    } catch (error) {
        gulpUtil.log(gulpUtil.colors.red('Config file could not be read'), error);
        throw new Exception();
    }
}


/**
 * Stylesheet task.
 */
const stylesRootDir = './src/styles/';
const stylesDestDir = './build/styles/';

const styleFileNames = ['main', 'splash', 'projects', 'profile', 'navbar', '404'];
const styleFilePaths = styleFileNames.map(function (fileName) {
    return stylesRootDir + fileName + '.less';
});

gulp.task('styles', function () {
    return gulp.src(styleFilePaths)
        .pipe(less())
        .pipe(gulp.dest(stylesDestDir))
        .pipe(notify({message: 'Styles task complete', onLast: true}));
});


/**
 * Script task.
 */
const scriptsRootDir = './src/scripts/';
const scriptsDestDir = './build/scripts/';

const postfix = '.entry';
const scriptsFileNames = ['splash', 'main', 'projects', 'profile', 'error404'];
const scriptsFilePaths = scriptsFileNames.map(function (fileName) {
    return scriptsRootDir + fileName + postfix + '.ts';
});

gulp.task('scripts', function () {
    const configFile = getConfigFile();

    const tasks = scriptsFilePaths.map(function (entry, index) {
        return browserify({
            entries: [entry],
            debug: false,
            insertGlobals: true,
            cache: {},
            packageCache: {}
        })
            .plugin(tsify)
            .bundle()
            .pipe(source(scriptsFileNames[index] + postfix + '.js'))
            .pipe(replace('SERVER_BASE_URL', configFile.SERVER_BASE_URL))
            .pipe(gulp.dest(scriptsDestDir));
    });
    return es.merge.apply(null, tasks)
        .pipe(notify({message: 'Scripts task complete', onLast: true}));
});

function getWatchifyHandler(bundler, fileName) {
    const configFile = getConfigFile();

    return () => {
        gulpUtil.log('Beginning build for ' + fileName);
        return bundler
            .bundle()
            .pipe(source(fileName + postfix + '.js'))
            .pipe(replace('SERVER_BASE_URL', configFile.SERVER_BASE_URL))
            .pipe(gulp.dest(scriptsDestDir));
    };
}

gulp.task('watch-scripts', function () {
    const tasks = scriptsFilePaths.map(function (entry, index) {
        const watchedBrowserify = watchify(browserify({
            entries: [entry],
            debug: false,
            cache: {},
            packageCache: {},
            insertGlobals: true,
            poll: 100
        }).plugin(tsify));
        const watchFunction = getWatchifyHandler(watchedBrowserify, scriptsFileNames[index]);

        watchedBrowserify.on('update', watchFunction);
        watchedBrowserify.on('log', gulpUtil.log);
        return watchFunction();
    });

    return es.merge.apply(null, tasks)
        .pipe(notify({message: 'Scripts watch task complete', onLast: true}));
});


/**
 * TypeScript definitions task.
 */
gulp.task("typings", function () {
    return gulp.src("./typings.json")
        .pipe(gulpTypings())
        .pipe(notify({message: 'Typings task complete'}));
});


/**
 * HTML task.
 */
const htmlRootDir = './src/';
const htmlDestDir = './build/';

const htmlFileNames = ['index', 'app', 'projects', 'profile', '404'];
const htmlFilePaths = htmlFileNames.map(function (fileName) {
    return htmlRootDir + fileName + '.html';
});

gulp.task('html', function () {
    const configFile = getConfigFile();

    return gulp.src(htmlFilePaths)
        .pipe(replace('GOOGLE_OAUTH_CLIENT_ID', configFile.GOOGLE_OAUTH_CLIENT_ID))
        .pipe(replace('SERVER_BASE_URL', configFile.SERVER_BASE_URL))
        .pipe(processHTML())
        .pipe(gulp.dest(htmlDestDir))
        .pipe(notify({message: 'HTML task complete', onLast: true}));
});


/**
 * Images task.
 */
const imagesRootDir = './src/img/';
const imagesDestDir = './build/img/';

const imagesFilePaths = [imagesRootDir + '**/*.png', imagesRootDir + '**/*.gif'];

gulp.task('images', function () {
    return gulp.src(imagesFilePaths)
        .pipe(gulp.dest(imagesDestDir))
        .pipe(notify({message: 'Images task complete', onLast: true}));
});


/**
 * Clean task.
 */
gulp.task('clean', function () {
    return del(['./build']);
});


/**
 * Bower task.
 */
gulp.task('bower', function () {
    return bower({cmd: 'install'}, ['--allow-root'])
        .pipe(notify({message: 'Bower task complete', onLast: true}));
});


/**
 * Default build task.
 */
gulp.task('default', function () {
    try {
        getConfigFile();
    } catch (error) {
        return;
    }
    runSequence('clean', 'typings', 'styles', 'bower', 'scripts', 'html', 'images');
});


/**
 * Watch task.
 */
gulp.task('watch', function () {
    try {
        getConfigFile();
    } catch (error) {
        return;
    }

    runSequence('default', () => {
        gulp.watch(stylesRootDir + '**/*.less', ['styles']);
        gulp.start('watch-scripts');
        gulp.watch(htmlRootDir + '**/*.html', ['html']);
        gulp.watch(imagesRootDir + '**/*.png', ['images']);
    });
});
