[![Tests](https://github.com/DartCaller/api/actions/workflows/main.yml/badge.svg)](https://github.com/DartCaller/api/actions/workflows/main.yml)
![](https://img.shields.io/github/license/DartCaller/api)
![](https://img.shields.io/tokei/lines/github/DartCaller/api)

# Ktor Backend
This repository contains the Kotlin backend, which acts as a middle man between the dart recognition python server (https://github.com/DartCaller/darts-recognition) and the user-facing frontend (https://github.com/DartCaller/web).


# Table of Contents
- [:package:Tech Stack](#package)  
- [:computer: Running Locally](#computer)
  - [:straight_ruler: Prerequisits](#straight_ruler)
  - [:running: Run](#running)
- [:bug: Testing](#bug)
- [:lock: Authentication](#lock)
- [:telephone_receiver: API Endpoints](#telephone_receiver)
  - [`/game/{gameID}/correctScore`](#correct)
  - [`/board/{boardID}/throw`](#throw)
    - [Dart Score Format Explained](#dartscore_format)
  - [`/ws`](#ws)
    - [CreateGame](#createGame)
      - [Network Game State Explained](#network_game_state) 
    - [JoinGame](#joinGame)
    - [NextLeg](#nextLeg)
- [:file_cabinet: DB Schema](#file_cabinet)
- [:lock_with_ink_pen: License](#lock_with_ink_pen)

<a name="package"/>

## :package: Tech Stack
- Language: [Kotlin](https://kotlinlang.org/)
- Framework: [Ktor](https://ktor.io/)
- Database: [PostgreSQL](https://www.postgresql.org/)
- ORM Framework: [Exposed](https://github.com/JetBrains/Exposed)
- Testing: [JUnit5](https://junit.org/junit5/) & [the Ktor testing tools](https://ktor.io/docs/testing.html)

<a name="computer"/>

## :computer: Running Locally
<a name="straight_ruler"/>

### :straight_ruler: Prerequisits

Kotlin runs on JVM. Hence it is necessary to use JDK 8 for your local Kotlin development.

After that, you can build the project with
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

where DATABASE_URL should follow the following format:

`DATABASE_URL=postgres://{user}:{password}@{hostname}:{port}/{database-name}`

<a name="bug"/>

## :bug: Testing

```bash
# run tests
$ ./gradlew test
```

Currently, this repository is equipped with [the Ktor testing tools](https://ktor.io/docs/testing.html) including [JUnit5](https://junit.org/junit5/).
To quote the official Ktor documentation on their testing tools
> Ktor has a special kind engine TestEngine, that doesn't create a web server, doesn't bind to sockets and doesn't do any real HTTP requests. Instead, it hooks directly into internal mechanisms and processes ApplicationCall directly. This allows for fast test execution at the expense of maybe missing some HTTP processing details. It's perfectly capable of testing application logic, but be sure to set up integration tests as well.

You can find the tests in https://github.com/DartCaller/api/tree/main/test.
The tests are part of the CI / CD pipeline and are run on each push to the repository, as can be seen here https://github.com/DartCaller/api/actions.

<a name="lock"/>

## :lock: Authentication
Most of the routes on this server require authentication, which I implemented with [Auth0](https://auth0.com/) and [JWT](https://jwt.io/). In the paragraph below about [API Endpoint](#telephone_receiver), every endpoint that does require authentication has a :closed_lock_with_key: emoji at the end of the headline. Every other endpoint can be called un-authenticated.

You can currently authenticate through the frontend https://github.com/DartCaller/web using username and password.
Or you can also authenticate, machine to machine like I do within https://github.com/DartCaller/darts-recognition, but here you would have to know the client_secret.

### Authentication on HTTP requests
The authentication of HTTP requests is then pretty straightforward. All that is required is the `access_token` issued by the authentication client that I created on [Auth0](https://auth0.com/) and then to pass that token as a Bearer Token in an HTTP request header ([link to a little HowTo](https://reqbin.com/req/5k564bhv/get-request-bearer-token-authorization-header-example)).

And that's it. You can now access protected HTTP endpoints.

### Authentication on WebSocket requests
Since WebSockets don't have an inbuilt authentication mechanism, I created my own little rule set for that, and it works like this.

Everybody can open a WebSocket connection to my WebSocket Endpoint under `/ws`. But every WS event that you'll send to the server will be interpreted as an authentication event until you successfully passed authentication. After that, you are free to send any [below specified WS Event](#ws)

<a name="telephone_receiver"/>

## :telephone_receiver: API Endpoints

<a name="throw"/>

### POST: `/board/{boardID}/throw` :closed_lock_with_key:
This is the endpoint that is used by the dart recognition python backend https://github.com/DartCaller/darts-recognition to submit scores that were detected on the dartboard. Each dart recognition hardware setup has a so-called `boardID` used to specify on which board this score has been thrown. If the frontend passes the same `boardID` during game creation, this backend knows that any submitted scores using this `boardID` will be added to the Dart Game with the same `boardID`.

:warning: Currently, the frontend does not send the `boardID`, but for the moment, I have hardcoded the same `boardID=proto` (short for prototype) for every game. This is why this backend currently doesn't support multiple parallel running games since one submitted dart score using the `boardID=proto` would be published to all active Dart Games.

All that is needed to call this route is the `boardID` as a query parameter and in the request body as plain text the dart score in the below specified format.

Method|Query Param|Example Request Body|Body Type|
------|-----------|--------------------|---------|
POST  | boardID   | `D14`              |plaintext|

<a name="dartscore_format"/>

#### DartScore Format 
The DartScore Format that is used in all three DartCaller Applications is pretty simple:
One DartScore equals one thrown dart. You have a leading identifier that specifies if the dart hit a single (`S`), double (`D`), or triple field (`T`)
After that, we have the field that has been hit.
So `T20` would mean the triple 20 fields resulting in a thrown score of 60 while `S25` means the single bull and `D25` means bulls-eye (the two small rings right at the center of the dartboard scoring 25 and 50 points, respectively).
For a more detailed explanation with graphic and more examples, please follow this link https://github.com/DartCaller/MainReadMe#score

<a name="correct"/>

### POST: `/game/{gameID}/correctScore` :closed_lock_with_key:
This route is used by the fronend to change a player's past score.
Just specify the playerID and the new score he should have, and his last thrown score will be set to this.

:warning: This action cannot be performed when the player has not finished his turn yet. He hast to first finish the turn, and then you can correct his score.


Method|Query Param|Example Request Body                                     |Body Type|
------|-----------|--------------------                                     |---------|
POST  | gameID    | `{ playerID: "{playerID}", scoreString: "D14S20T10" }`  |plaintext|

<a name="ws"/>

### `/ws`
This is the domain where the WebSocket for the frontend is served. The below-mentioned endpoints are all WebSocket events that require a certain WebSocket event payload to be sent to the backend after the frontend has successfully created a WebSocket connection via this endpoint.
```json
# Minimal event payload
{ "type": "{WsEventType}" }
```
All WebSocket event payloads need to be a JSON object with a `type` property specifying the WebSocket event type which the frontend wishes to execute.
The rest of the payload that is required is different for every WS event and specified below.

<a name="auth_event"/>

#### WS: Authentication
```json
{ "type": "Authenticate", "accessToken": "{access_token}" }
```
This WS event will be the first one you'll need to send. After you authenticate yourself with this event, you can send any other WS event specified below.

<a name="createGame"/>

#### WS: CreateGame :closed_lock_with_key:
```json
{ "type": "CreateGame", "players": ["Alice", "Bob", "Cedric"], "gameMode": "301" }
```
This WS event will create a new dart game with the specified players and the given game mode. Currently, the valid `gameModes` are `301` & `501`, determining the leg starting number.
After successfully creating the new dart game, the WebSocket client will receive updates on the game's current state in the form of the network game state specified below.

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
The `scores` key follows the [Dart Score Format](#dartscore_format). While the very first element in each player's score list is just the starting number of the round, every element after that has between 1 and 3 dart scores directly together depending on how many darts the player has thrown in that round. After a player has completed his turn, the round scorestring string should contain three occurrences of the [Dart Score Format](#dartscore_format). If you see a string with less than three occurrences, it means the player is still throwing his last darts.

<a name="joinGame"/>

#### WS: JoinGame :closed_lock_with_key:
```json
{ "type": "JoinGame", "gameID": "{GameUUID}" }
```
When the specified gameID is found within the current active games, the client will be added as a subscriber to the game and will receive the newest [network game state](#network_game_state) of the specified dart game from now on.

<a name="nextLeg"/>

#### WS: NextLeg :closed_lock_with_key:
```json
{ "type": "NextLeg", "gameID": "{GameUUID}" }
```
When the specified gameID is found within the current active games, and all players have finished the current game, the next game will be started. In the next game, the player who finished last in the last round will start, and the winner of the previous round will go last.

<a name="file_cabinet"/>

## :file_cabinet: DB Schema
![DartBachelor](https://user-images.githubusercontent.com/32591853/120396855-9546e100-c337-11eb-854f-8d6a9a3e0e81.png)

<a name="lock_with_ink_pen"/>

## :lock_with_ink_pen: License
Distributed under the GNU GPLv3 License. See [LICENSE](LICENSE) for more information.
