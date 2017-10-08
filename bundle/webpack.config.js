const webpack = require('webpack');
const path = require('path');

const BUILD_DIR = path.resolve(__dirname, '..', 'prod', 'bundle');
const APP_DIR = path.resolve(__dirname, 'src', 'js');

const config = {
    entry: `${APP_DIR}/main.js`,
    output: {
        path: BUILD_DIR,
        filename: 'bundle.js'
    },
    module: {
        loaders: [
            {
                test: /.jsx?$/,
                loader: 'babel-loader',
                exclude: /node_modules/,
                query: {
                    presets: ['es2015', 'stage-0', 'react']
                }
            },
            {
                test: /\.css$/,
                loader: "style-loader!css-loader"
            }
        ]
    },
};

module.exports = config;