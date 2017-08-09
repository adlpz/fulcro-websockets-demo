# Demo Fulcro + WebSockets implementation

This is a short demo of how a functioning (as of today) fulcro with websockets implementation would look like.

The main parts are:

- `src/dev/`
  - `cljs/user.cljs`: Helper functions for the figwheel repl
  - `user.clj`: Builds the server-side. Also helper functions for the repl
- `src/main/fulcro_ws/`
  - `app.cljs`: Entry point for the client-side, with component definitions and building of the fulcro client.
  - `logic.cljs`: Client-side application logic, decoupled from om specifics
  - `mutations.cljs`: Om mutations, for client and server events, for the client-side
  - `server.clj`: Server-side implemetation, which is called from the `dev/user.clj`
  - `operations.clj`: Server-side logic and database
  
Each file has comments explaining what's going on. In order to run the demo app, do:

```
$ lein repl
[... repl loads ...]
user=> (go)
```

This loads the server side. For the client side just run the `figwheel.clj` script provided:

```
$ lein run -m clojure.main script/figwheel.clj
```

Then connect to whatever port the server repl reports, usually `localhost:4050`. You can try opening several windows with the app and clicking things to see how messages are passed around.
