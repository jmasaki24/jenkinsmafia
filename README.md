# Battlecode 2020 - jenkinsmafia

I suppose this can serve as our "notes"/brainstorming document.

### Order of priorities/actions
- Build a few miners (10 perhaps?)
- Build a refinery if the soup is far (more than HQ radius) away
-

## Random Ideas

- Reconize blockchain transactions that aren't our own and resubmit them with a few random changes
    - As a result, we need to implement checksums in our transactions to combat this "cyberattack"

- Put dirt on enemy structures. Let's not try to attack HQ at the moment. *Intead:*
  Attack refineries by flying in a

- Dig out enemy wall. Fly in landscapers toward enemy HQ.

- Put the entire map (or what our robots have seen so far) into the blockchain

- Build a vaporator near HQ or refineries if there happens to be a lot of pollution

- Scan topography around HQ to see if there's a natural wall we could build off of

-

## Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client.
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.

