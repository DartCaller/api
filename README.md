[![Tests](https://github.com/DartCaller/api/actions/workflows/main.yml/badge.svg)](https://github.com/DartCaller/api/actions/workflows/main.yml)
![](https://img.shields.io/github/license/DartCaller/api)
![](https://img.shields.io/tokei/lines/github/DartCaller/api)

# Ktor Backend
This Repo contain the kotlin backend which acts as a middle man between the dart recognition python server (https://github.com/DartCaller/darts-recognition) and the user facing frontend (https://github.com/DartCaller/web).


# Table of Contents
- [:package:Tech Stack](#package)  
- [:computer: Running Locally](#computer)
  - [:straight_ruler: Prerequisits](#straight_ruler)
  - [:running: Run](#running)
- [:bug: Testing](#bug)
- [:telephone_receiver: API Endpoints](#telephone_receiver)
  - [`/game/{gameID}/correctScore`](#correct)
  - [`/board/{boardID}/throw`](#throw)
    - [Dart Score Format Explained](#dartscore_format)
  - [`/ws`](#ws)
    - [CreateGame](#createGame)
      - [Network Game State Explained](#network_game_state) 
    - [JoinGame](#joinGame)
    - [NextLeg](#nextLeg)
- [:lock_with_ink_pen: License](#lock_with_ink_pen)

<a name="package"/>

## :package: Tech Stack
- Language: [Kotlin](https://kotlinlang.org/)
- Framework: [Ktor](https://ktor.io/)
- Database: [PostgreSQL](https://www.postgresql.org/)
- ORM Framework: [Exposed](https://github.com/JetBrains/Exposed)
- Testing: [JUnit5](https://junit.org/junit5/) & [the ktor testing tools](https://ktor.io/docs/testing.html)

<a name="computer"/>

## :computer: Running Locally
<a name="straight_ruler"/>

### :straight_ruler: Prerequisits

Kotlin runs on JVM, hence it is necessary to use JDK 8 for your local Kotlin development.

After that you can build the project with
```bash
# run tests and build
$ ./gradlew build


# build without tests
$ ./gradlew build -x test
```
<a name="running"/>

### :running: Run

```bash
# serve with hot reload at localhost:3000
$ DATABASE_URL={DB_URL} ./gradlew run
```

where DATABASE_URL should look roughly like this
`DATABASE_URL=postgres://{user}:{password}@{hostname}:{port}/{database-name}`

<a name="bug"/>

## :bug: Testing

```bash
# run tests
$ ./gradlew test
```

Currently this repo is equipped with [the ktor testing tools](https://ktor.io/docs/testing.html) including [JUnit5](https://junit.org/junit5/).
To quote the official ktor documentation on their testing tools
> Ktor has a special kind engine TestEngine, that doesn't create a web server, doesn't bind to sockets and doesn't do any real HTTP requests. Instead, it hooks directly into internal mechanisms and processes ApplicationCall directly. This allows for fast test execution at the expense of maybe missing some HTTP processing details. It's perfectly capable of testing application logic, but be sure to set up integration tests as well.

The tests can be found in https://github.com/DartCaller/api/tree/main/test

<a name="telephone_receiver"/>

## :telephone_receiver: API Endpoints

<a name="throw"/>

### POST: `/board/{boardID}/throw`
This is the endpoint which is used by the dart recognition python backend https://github.com/DartCaller/darts-recognition to submit scores that were detected on the dartboard. Each dart recognition hardware setup has a so called `boardID` that is used to specify on which board this score has been thrown. If the Frontend passes the same `boardID` during game creation, then this backend knows that any submitted scores using this `boardID` will be added to the Dart Game with the same `boardID`.
:warning: Currently the Frontend does not send this `boardID` but within here I have hardcoded the same `boardID=proto` for each and every game. This is why this backend currently doesn't support multiple parallel running games since one submitted dartscore using the `boardID=proto` would be published to all currently active Dart Games.

<a name="dartscore_format"/>

#### DartScore Format
The DartScore Format that is used in all three DartCaller Applications is pretty simple:
One DartScore equals one thrown Dart. You have a leading identifier which specifies if the dart hit a single (`S`), double (`D`) or tripple field (`T`)
After that we have the field that has been hit.
So `T20` would mean the triple 20 field resulting in a thrown score of 60 while `S25` means the single bull and `D25` means bulls-eye (the two small rings right at the center of the dartboard scoring 25 and 50 points respectively).

<a name="correct"/>

### POST: `/game/{gameID}/correctScore`

<a name="ws"/>

### `/ws`
This is the domain where the websocket for the frontend is served. The below mentioned endpoints are all websocket events that require a certain websocket event payload to be sent to the backend after the frontend has successfully created a websocket connection via this endpoint.
```json
# Minimal event payload
{ "type": "{WsEventType}" }
```
All websocket event payloads need to be a JSON object with a `type` property specifying the websocket event type which the frontend wishes to execute.
The rest of the payload that is required is different for every WS event and specified below

<a name="createGame"/>

#### WS: CreateGame
```json
{ "type": "CreateGame", "players": ["Alice", "Bob", "Cedric"], "gameMode": "301" }
```
This ws event will create a new dart game with the specified players and the given gameMode. Currently the valid `gameModes` are `301` & `501` which specify the leg starting number.
After successful creation of the new dart game, the websocket client will receive updates on the current state of the game in the form of the network game state specified below.

<a name="network_game_state"/>

##### Example Network Game State
```json
{
  "gameID": "123",
  "legFinished": false,
  "playerNames": {
    "exampleUUID1": "Alice",
    "exampleUUID2": "Bob",
  },
  "currentPlayer": "exampleUUID1",
  "scores": {
    "exampleUUID1": ["501", "T20D10S5"],
    "exampleUUID2": ["501", "T20D10S5"]
  },
  "playerOrder": ["exampleUUID1", "exampleUUID2"],
  "playerFinishedOrder": ["exampleUUID2"],
  "currentRoundIndex": 1
}
```
The `scores` key follows the [Dart Score Format](#dartscore_format). While the very first element in each players score list is just the starting number of the round, every element after that has between 1 and 3 dartscores directly together depending on how many darts the player has thrown in that round. After a player has completed his turn, the round scorestring string should contain 3 occurrences of the [Dart Score Format](#dartscore_format). If you see a string with less than 3 occurrences it means the player is still throwing his last darts.

<a name="joinGame"/>

#### WS: JoinGame
```json
{ "type": "JoinGame", "gameID": "{GameUUID}" }
```
When the specified gameID is found within the current active games the client will be added as a subscriber to the game and will receive the newest [network game state](#network_game_state) of the specified dart game from now on.

<a name="nextLeg"/>

#### WS: NextLeg
```json
{ "type": "NextLeg", "gameID": "{GameUUID}" }
```
When the specified gameID is found within the current active games and all players have finished the current game, the next game will be started. In the next game the player who finished last in the last round will start and the winner of the last round will go last.

<a name="lock_with_ink_pen"/>

## :lock_with_ink_pen: License
Distributed under the GNU GPLv3 License. See [LICENSE](LICENSE) for more information.
