# bomberman-clj

[![CircleCI](https://circleci.com/gh/idleherb/bomberman-clj.svg?style=svg)](https://circleci.com/gh/idleherb/bomberman-clj)

A basic Bomberman game written in Clojure and ClojureScript.

## Installation

Clone from https://github.com/idleherb/bomberman-clj.

## Usage

### Dev with Hot Reloading

    $1 lein run 17 15 2 0.0.0.0 8080
    $2 lein figwheel

In your browser, open http://0.0.0.0:3449.

### Prod

    $ lein uberjar
    $ java -jar bomberman-clj-0.1.0-standalone.jar width height num-players host port

In your browser, open http://0.0.0.0:8080.

Control with arrow keys/spacebar. Work in progress :)

## License

See UNLICENSE.
