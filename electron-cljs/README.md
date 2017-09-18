# iWasWhere desktop app

    $ npm install -g electron-builder@19.28.4
    $ npm install -g electron-publisher-s3@19.28.3
    
    $ yarn install
    $ lein build
    $ npm start


## Publishing

Publishing entire project from parent directory:

    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish_beta.sh
    $ AWS_ACCESS_KEY_ID=<...> AWS_SECRET_ACCESS_KEY=<...> ./publish.sh

