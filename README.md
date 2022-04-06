Aruco Remote
============

## Scan Aruco markers using phone camera and forward them for further processing

Aruco Remote was designed as a generic companion app for board games and educational exercises. You will need a set of printed cards and a server running on your computer to make it useful.

If you were sent here from a game or by a teacher, you'd better follow their instructions. :) Otherwise the main idea is that each card or token as well as the corners of the board are marked with an individual Aruco code (like a QR code, but smaller). When the cards and tokens are placed on the board, you can scan them with this app to detect the markers and send them to your computer or an online server. The program running there will reconstruct the placement of cards and tokens and implement the game logic.

Technical details:
To use this app for a new game, you should pick one of the standard Aruco dictionaries and print a distinct marker on each unique card. Run a server that accepts POST requests with a JSON payload over either http or https. Print a QR code with the server url and the dictionary name (e.g. `DICT_6X6_100`) separated by a space. When running the app for the first time, scan this QR code to configure the server url and to load the dictionary. At this point Aruco markers should show up with a green outline in the app. Touching the screen will send a JSON payload to your server with the image frame size, the touch coordinates and the coordinates for each detected marker along with its orientation and identifier. The image itself is not sent over the network. When processing the request on the server, you can send a short line of text in your http response and it will show up as a message in the app.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="200">](https://play.google.com/store/apps/details?id=app.aruco)

Screenshots:

<img src="https://raw.githubusercontent.com/bndeff/aruco/master/static/screenshot1.jpg" alt="screenshot 1" width="480"> <img src="https://raw.githubusercontent.com/bndeff/aruco/master/static/screenshot2.jpg" alt="screenshot 2" width="480">

