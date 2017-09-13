import TSim.*;

import java.util.concurrent.Semaphore;
import static TSim.TSimInterface.SWITCH_RIGHT;
import static TSim.TSimInterface.SWITCH_LEFT;

public class Lab1 {

    /**
     * The positions of the station sensors
     */
    private static final int[][] STATION_POSITIONS = {{16,3}, {16, 5}, {16, 11}, {16, 13}};

    //The direction that the train is currently moving in
    private static final int NORTH = 0;
    private static final int SOUTH = 1;

    /**
     * Contains all the track sections current status
     */
    private Semaphore[] trackStatus;

    /**
     * If debug messages should be printed to the console
     */
    private static final boolean DEBUG = false;

    /**
     * Setups the semaphores and the trains for the map
     * @param speed1 Speed for the first train
     * @param speed2 Speed for the second train
     */
    public Lab1(Integer speed1, Integer speed2) {
        trackStatus = new Semaphore[9];
        for (int i = 0; i < trackStatus.length; i++)
            trackStatus[i] = new Semaphore(1);
        new Thread(new Train(1, speed1, SOUTH, 0)).start();
        new Thread(new Train(2, speed2, NORTH, 7)).start();
    }
    
    /**
       Switches the track at the pos to the direction provided
       @param x X coordinate of the track
       @param y Y coordinate of the track
       @param dir The direction to switch to {@link TSim.TSimInterface#SWITCH_RIGHT} or {@link TSim.TSimInterface#SWITCH_LEFT}
       @throws CommandException if the coordinates of the switch were invalid (NO_SUCH_SWITCH) or if there was a train on the switch (TRAIN_ON_SWITCH)
     */
    private static void switchTrack(int x, int y, int dir) throws CommandException {
        TSimInterface.getInstance().setSwitch(x, y, dir);
    }

    /***
     * Check if the event is fired at a position
     * @param e The event that was fired
     * @param x The x coordinate of the location to compare to
     * @param y The y coordinate of the location to compare to
     * @return If the sensor was fired at the provided position
     */
    private static boolean atSensor(SensorEvent e, int x, int y) {
        return e.getXpos() == x && e.getYpos() == y;
    }

    /**
     * The class for a train
     */
    public class Train implements Runnable {

        private int id;
        private int speed;
        private int direction;
        private int ticket;

        /**
         * Creates the train instance and acquires the ticket for the start section
         * @param id The id of the train
         * @param speed The initial speed of the train
         * @param direction The initial direction of the train
         * @param ticket The initial ticket for the train
         */
        public Train(int id, int speed, int direction, int ticket) {
            this.id = id;
            this.speed = speed;
            this.direction = direction;
            this.ticket = ticket;
            trackStatus[ticket].tryAcquire();
        }

        /**
         * The main method that starts when the thread is started
         */
        @Override
        public void run() {
            TSimInterface tsi = TSimInterface.getInstance();

            try {
              tsi.setSpeed(id, speed);
              while(true) {
                  SensorEvent e = tsi.getSensor(id);
                  passSensor(e);
              }
            } catch (CommandException e) {
              e.printStackTrace();    // or only e.getMessage() for the error
              System.exit(1);
            } catch (InterruptedException e) {
              e.printStackTrace();    // or only e.getMessage() for the error
              System.exit(1);
            }
        }

        /**
         * Tries to acquire a ticket with the provided ticket
         * @param ticket The ticket to try to acquire
         * @return If the acquire was successful
         */
        private boolean tryAcc(int ticket) {
            int old = trackStatus[ticket].availablePermits();
            boolean r = trackStatus[ticket].tryAcquire();
            if (DEBUG) System.err.printf("Train: %d\tTried: %d=%b:%d->%d\n", this.id, ticket, r, old, (trackStatus[ticket].availablePermits()));
            return r;
        }

        /**
         * Releases the provided ticket
         * @param ticket The ticket to release
         */
        private void release(int ticket) {
            trackStatus[ticket].release();
            if (DEBUG) System.err.printf("Train: %d\tReleased: %d\n", this.id, ticket);
        }

        /**
         * Acquires the provided ticket, locks until it was successful
         * @param ticket The ticket to acquire
         * @throws InterruptedException if the current thread is interrupted
         */
        private void acc(int ticket) throws InterruptedException {
            trackStatus[ticket].acquire();
            if (DEBUG) System.err.printf("Train: %d\tAcquired: %d\n", this.id, ticket);
        }

        /**
         * Stops and wait for the ticket to be available
         * @param ticket The ticket to wait for and acquire
         * @throws CommandException if the supplied id was false (NO_SUCH_TRAIN), if the speed was illegal (ILLEGAL_SPEED) or if the train had crashed.
         * @throws InterruptedException if the current thread is interrupted
         */
        private void stopWait(int ticket) throws CommandException, InterruptedException {
            if (DEBUG) System.err.printf("Train: %d\tWaiting for track %d\n", this.id, ticket);
            TSimInterface.getInstance().setSpeed(this.id, 0);
            acc(ticket);
            TSimInterface.getInstance().setSpeed(this.id, speed);
            if (DEBUG) System.err.printf("Train: %d\tGot track %d\n", this.id, ticket);
        }

        /**
         * Process the sensor event and makes action based on what sensor that was passed
         * @param e The SensorEvent that was received
         * @throws CommandException if the supplied id was false (NO_SUCH_TRAIN), if the speed was illegal (ILLEGAL_SPEED) or if the train had crashed.
         * @throws InterruptedException if the current thread is interrupted
         */
        private void passSensor(SensorEvent e) throws CommandException, InterruptedException {
            if (e.getStatus() == SensorEvent.ACTIVE) {
                boolean station = isStation(e);
                if (!station) {
                    boolean atPart;

                    // Handles leaving first and second into the third as well as leaving the seventh and eighth into the sixth
                    // Also traveling into the sections
                    if ((atPart = (atSensor(e, 13, 7) || atSensor(e, 13, 8))) || (atSensor(e, 5, 11) || atSensor(e, 5, 13))) {
                        int sec = atPart ? 3 : 6;
                        int dir = sec == 3 ? SOUTH : NORTH;
                        if (direction == dir) {
                            if (atPart)
                                release(2);
                            if (!tryAcc(sec))
                                stopWait(sec);
                            release(ticket);
                            ticket = sec;
                        } else if (atPart && direction == NORTH) {
                            release(3);
                            if (!tryAcc(2))
                                stopWait(2);
                        } else
                            release(sec);
                        processTrackSwitch(e);

                    // Handles the north entry and leaving of the + intersection
                    } else if ((atSensor(e, 9, 5)) || atSensor(e, 6, 6)) {
                        if (direction == SOUTH && !tryAcc(2))
                            stopWait(2);
                        else if (direction == NORTH)
                            release(2);

                    // Handles the entry of the fifth and fourth section
                    } else if ((atPart = atSensor(e, 16, 9)) || atSensor(e, 3, 9)) {
                        int dir = atPart ? SOUTH: NORTH;
                        if (direction == dir) {
                            if (tryAcc(4))
                                ticket = 4;
                            else if (tryAcc(5))
                                ticket = 5;
                            processTrackSwitch(e);
                        }

                    // Handles the entry of the first, second, seventh and eighth
                    } else if ((atPart = atSensor(e, 18 ,7)) || atSensor(e, 1, 11)){
                        int sec = atPart ? 0: 7;
                        int dir = sec == 0 ? NORTH: SOUTH;
                        if (direction == dir) {
                            if (tryAcc(sec))
                                ticket = sec;
                            else if (tryAcc(sec + 1))
                                ticket = sec + 1;
                        }
                        processTrackSwitch(e);

                    // Handles the passing of the middle double track
                    } else if (atSensor(e, 9, 9) || atSensor(e, 9, 10)) {
                        int sec = direction == NORTH ? 3: 6;
                        release(sec == 3 ? 6: 3);
                        if (!tryAcc(sec))
                            stopWait(sec);
                        release(ticket);
                        ticket = sec;
                        processTrackSwitch(e);
                    }

                //If a train arrives at a station this handles the stopping and changing of direction
                } else {
                    if (DEBUG) System.err.printf("Train %d arrived at station\n", id);
                    TSimInterface.getInstance().setSpeed(id, 0);
                    Thread.sleep(1000 + (20 * Math.abs(speed)));
                    if (direction == NORTH)
                        direction = SOUTH;
                    else
                        direction = NORTH;
                    speed = -speed;
                    TSimInterface.getInstance().setSpeed(id, speed);
                }
            }
        }

        /**
         * Processes the location of the sensor event and switches the appropriate junctions
         * @param e The SensorEvent that was received
         * @throws CommandException if the supplied id was false (NO_SUCH_TRAIN), if the speed was illegal (ILLEGAL_SPEED) or if the train had crashed.
         */
        private void processTrackSwitch(SensorEvent e) throws CommandException {
            if (atSensor(e, 18, 7) && direction == NORTH && ticket == 1)
                switchTrack(17, 7, SWITCH_LEFT);
            else if (atSensor(e, 18, 7) && direction == NORTH)
                switchTrack(17, 7, SWITCH_RIGHT);
            else if (atSensor(e, 13, 8) && direction == SOUTH)
                switchTrack(17, 7, SWITCH_LEFT);
            else if (atSensor(e, 13, 7) && direction == SOUTH)
                switchTrack(17, 7, SWITCH_RIGHT);
            else if (atSensor(e, 16, 9) && direction == SOUTH && ticket == 4)
                switchTrack(15, 9, SWITCH_RIGHT);
            else if (atSensor(e, 16, 9) && direction == SOUTH)
                switchTrack(15, 9, SWITCH_LEFT);
            else if (atSensor(e, 3, 9) && direction == NORTH && ticket == 4)
                switchTrack(4, 9, SWITCH_LEFT);
            else if (atSensor(e, 3, 9) && direction == NORTH)
                switchTrack(4, 9, SWITCH_RIGHT);
            else if (atSensor(e, 9, 9) && direction == NORTH)
                switchTrack(15, 9, SWITCH_RIGHT);
            else if (atSensor(e, 9, 9) && direction == SOUTH)
                switchTrack(4, 9, SWITCH_LEFT);
            else if (atSensor(e, 9, 10) && direction == NORTH)
                switchTrack(15, 9, SWITCH_LEFT);
            else if (atSensor(e, 9, 10) && direction == SOUTH)
                switchTrack(4, 9, SWITCH_RIGHT);
            else if (atSensor(e, 1, 11) && direction == SOUTH && ticket == 7)
                switchTrack(3, 11, SWITCH_LEFT);
            else if (atSensor(e, 1, 11) && direction == SOUTH)
                switchTrack(3, 11, SWITCH_RIGHT);
            else if (atSensor(e, 5, 11) && direction == NORTH)
                switchTrack(3, 11, SWITCH_LEFT);
            else if (atSensor(e, 5, 13) && direction == NORTH)
                switchTrack(3, 11, SWITCH_RIGHT);
        }

        /**
         * Helper method if the SensorEvent was received at a station or not
         * @param e The SensorEvent that was received
         * @return If the Train is at a Station or not
         */
        private boolean isStation(SensorEvent e) {
            for (int[] p : STATION_POSITIONS) {
                if (atSensor(e, p[0], p[1])) {
                    return true;
                }
            }
            return false;
        }
    }
}
