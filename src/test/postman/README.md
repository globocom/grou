# Tests

## First steps

Before, ensure that you have NodeJS. Using Homebrew:

```terminal
$ brew install node
```

You can see more details in GitHub of the project:

* [NodeJS](https://nodejs.org/en/download/package-manager/)
* [Newman](https://github.com/postmanlabs/newman)

## Installing Newman

Now, you can install the Newman to run a test from the command-line.

```terminal
$ npm install newman --global;
```

## Environment variables

In the "Grou.postman_environment.json" file we can change the values according to the environment tested.

## Running tests

### Basic:

```terminal
$ newman run Grou.postman_collection.json -e Grou.postman_environment.json`
```

### Repeat the same test N times

```terminal
$ newman run Grou.postman_collection.json -e Grou.postman_environment.json --iteration-count <number>
```

**Note:** [More options](https://github.com/postmanlabs/newman#command-line-options)

## CI integration