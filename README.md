# bomberman-clj

[![CircleCI](https://circleci.com/gh/idleherb/bomberman-clj.svg?style=svg)](https://circleci.com/gh/idleherb/bomberman-clj)

A basic Bomberman game written in Clojure and ClojureScript.

## Installation

Clone from <https://github.com/idleherb/bomberman-clj>.

## Usage

### Dev with Hot Reloading

    $1 lein run
    $2 lein figwheel

In your browser, open <http://0.0.0.0:3449>.

### Prod

    $ lein uberjar
    $ java -jar bomberman-clj-standalone.jar host port

In your browser, open <http://0.0.0.0:8080>.

Control with arrow keys to move, spacebar to place bombs, return to detonate remote bombs. Work in progress :)

## License

See UNLICENSE.
