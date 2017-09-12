import TSim.*;

import java.util.concurrent.Semaphore;
import static TSim.TSimInterface.SWITCH_RIGHT;
import static TSim.TSimInterface.SWITCH_LEFT;

public class Lab1 {

    private static final SensorPos[] STATION_POSITIONS = {p(16, 3), p(16, 5), p(16, 11), p(16, 13)};

    private static final int NORTH = 0;
    private static final int SOUTH = 1;

    private static final boolean DEBUG = true;

    private Semaphore[] trackStatus;

    /**
       TODO! : Explain constructor.
       @param speed1 Integer, 
       @param speed2 Integer,
     */
    public Lab1(Integer speed1, Integer speed2) {
        trackStatus = new Semaphore[9];
        for (int i = 0; i < trackStatus.length; i++)
            trackStatus[i] = new Semaphore(1);
        new Thread(new Train(1, speed1, SOUTH, 0)).start();
        new Thread(new Train(2, speed2, NORTH, 7)).start();
    }
    
    /**
       
       @param x int
       @param y int
       @param dir int
       @throws CommandException,
     */
    private static void switchSensor(int x, int y, int dir) throws CommandException {
        TSimInterface.getInstance().setSwitch(x, y, dir);
    }

    private static boolean atSensor(SensorEvent e, int x, int y) {
        return e.getXpos() == x && e.getYpos() == y;
    }

    private static SensorPos p(int x, int y) {
        return new SensorPos(x, y);
    }
    
    /**
       
     */
    public class Train implements Runnable {

        private int id;
        private int speed;
        private int direction;
        private int ticket;

        public Train(int id, int speed, int direction, int ticket) {
            this.id = id;
            this.speed = speed;
            this.direction = direction;
            this.ticket = ticket;
            trackStatus[ticket].tryAcquire();
        }
        
        @Override
        public void run() {
            TSimInterface tsi = TSimInterface.getInstance();

            try {
              tsi.setSpeed(id, speed);
              while(true) {
                  SensorEvent e = tsi.getSensor(id);
                  passSensor(e);
              }
            }
            catch (CommandException e) {
              e.printStackTrace();    // or only e.getMessage() for the error
              System.exit(1);
            } catch (InterruptedException e) {
              e.printStackTrace();    // or only e.getMessage() for the error
              System.exit(1);
            }
        }

        private boolean tryAcc(int id) {
            int old = trackStatus[id].availablePermits();
            boolean r = trackStatus[id].tryAcquire();
            if (DEBUG) System.err.printf("Train: %d\tTried: %d=%b:%d->%d\n", this.id, id, r, old, (trackStatus[id].availablePermits()));
            return r;
        }

        private void release(int id) {
            trackStatus[id].release();
            if (DEBUG) System.err.printf("Train: %d\tReleased: %d\n", this.id, id);
        }

        private void acc(int id) throws InterruptedException {
            trackStatus[id].acquire();
            if (DEBUG) System.err.printf("Train: %d\tAcquired: %d\n", this.id, id);
        }

        private void stopWait(int id) throws CommandException, InterruptedException {
            if (DEBUG) System.err.printf("Train: %d\tWaiting for track %d\n", this.id, id);
            TSimInterface.getInstance().setSpeed(this.id, 0);
            acc(id);
            TSimInterface.getInstance().setSpeed(this.id, speed);
            if (DEBUG) System.err.printf("Train: %d\tGot track %d\n", this.id, id);
        }

        private void passSensor(SensorEvent e) throws CommandException, InterruptedException {
            if (e.getStatus() == SensorEvent.ACTIVE) {
                boolean station = isStation(e);
                if (!station) {
                    boolean atPart;

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
                        processSensorPass(e);
                    } else if ((atSensor(e, 9, 5)) || atSensor(e, 6, 6)) {
                        if (direction == SOUTH && !tryAcc(2))
                            stopWait(2);
                        else if (direction == NORTH)
                            release(2);
                    } else if ((atPart = atSensor(e, 16, 9)) || atSensor(e, 3, 9)) {
                        int dir = atPart ? SOUTH: NORTH;
                        if (direction == dir) {
                            if (tryAcc(4))
                                ticket = 4;
                            else if (tryAcc(5))
                                ticket = 5;
                            processSensorPass(e);
                        }
                    } else if ((atPart = atSensor(e, 18 ,7)) || atSensor(e, 1, 11)){
                        int sec = atPart ? 0: 7;
                        int dir = sec == 0 ? NORTH: SOUTH;
                        if (direction == dir) {
                            if (tryAcc(sec)) {
                                ticket = sec;
                            } else if (tryAcc(sec + 1)) {
                                ticket = sec + 1;
                            }
                        }
                        processSensorPass(e);
                    } else if (atSensor(e, 9, 9) || atSensor(e, 9, 10)) {
                        int sec = direction == NORTH ? 3: 6;
                        release(sec == 3 ? 6: 3);
                        if (!tryAcc(sec)) {
                            stopWait(sec);
                        }
                        release(ticket);
                        ticket = sec;
                        processSensorPass(e);
                    }

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

        private void processSensorPass(SensorEvent e) throws CommandException {
            if (atSensor(e, 18, 7) && direction == NORTH && ticket == 1)
                switchSensor(17, 7, SWITCH_LEFT);
            else if (atSensor(e, 18, 7) && direction == NORTH)
                switchSensor(17, 7, SWITCH_RIGHT);
            else if (atSensor(e, 13, 8) && direction == SOUTH)
                switchSensor(17, 7, SWITCH_LEFT);
            else if (atSensor(e, 13, 7) && direction == SOUTH)
                switchSensor(17, 7, SWITCH_RIGHT);
            else if (atSensor(e, 16, 9) && direction == SOUTH && ticket == 4)
                switchSensor(15, 9, SWITCH_RIGHT);
            else if (atSensor(e, 16, 9) && direction == SOUTH)
                switchSensor(15, 9, SWITCH_LEFT);
            else if (atSensor(e, 3, 9) && direction == NORTH && ticket == 4)
                switchSensor(4, 9, SWITCH_LEFT);
            else if (atSensor(e, 3, 9) && direction == NORTH)
                switchSensor(4, 9, SWITCH_RIGHT);
            else if (atSensor(e, 9, 9) && direction == NORTH)
                switchSensor(15, 9, SWITCH_RIGHT);
            else if (atSensor(e, 9, 9) && direction == SOUTH)
                switchSensor(4, 9, SWITCH_LEFT);
            else if (atSensor(e, 9, 10) && direction == NORTH)
                switchSensor(15, 9, SWITCH_LEFT);
            else if (atSensor(e, 9, 10) && direction == SOUTH)
                switchSensor(4, 9, SWITCH_RIGHT);
            else if (atSensor(e, 1, 11) && direction == SOUTH && ticket == 7)
                switchSensor(3, 11, SWITCH_LEFT);
            else if (atSensor(e, 1, 11) && direction == SOUTH)
                switchSensor(3, 11, SWITCH_RIGHT);
            else if (atSensor(e, 5, 11) && direction == NORTH)
                switchSensor(3, 11, SWITCH_LEFT);
            else if (atSensor(e, 5, 13) && direction == NORTH)
                switchSensor(3, 11, SWITCH_RIGHT);
        }
        
        private boolean isStation(SensorEvent e) {
            for (SensorPos p : STATION_POSITIONS) {
                if (atSensor(e, p.getX(), p.getY())) {
                    return true;
                }
            }
            return false;
        }
        
    }
    
    /**
       
     */
    public static class SensorPos {
        private int x;
        private int y;

        public SensorPos(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

    }
  
}
