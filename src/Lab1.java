import TSim.*;

import java.util.concurrent.Semaphore;
import static TSim.TSimInterface.SWITCH_RIGHT;
import static TSim.TSimInterface.SWITCH_LEFT;

public class Lab1 {

    private static final SensorPos[] SENSOR_POSITIONS = {
            p(6, 5, new int[]{0}, new int[]{2}, new int[]{0}),     p(10, 5, new int[]{1}, new int[]{2}, new int[]{1}),   p(13, 7, new int[]{2}, new int[]{3}, new int[]{0}),
            p(13, 8, new int[]{2}, new int[]{3}, new int[]{1}),    p(18, 7, new int[]{0,1}, new int[]{3}),               p(16, 9, new int[]{3}, new int[]{4,5}),
            p(9, 9, new int[]{3}, new int[]{6}),                   p(9, 10, new int[]{3}, new int[]{6}),                 p(3, 9, new int[]{4,5}, new int[]{6}),
            p(1, 11, new int[]{6}, new int[]{7,8}),                p(8, 11,  new int[]{6}, new int[]{7}),                p(8, 13, new int[]{6}, new int[]{8}),
            //Stations
            p(16, 3), p(16, 5), p(16, 11), p(16, 13)};

    private static final int NORTH = 0;
    private static final int SOUTH = 1;

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
    public static void switchSensor(int x, int y, int dir) throws CommandException {
        TSimInterface.getInstance().setSwitch(x, y, dir);
    }

    public static boolean atSensor(SensorEvent e, int x, int y) {
        return e.getXpos() == x && e.getYpos() == y && e.getStatus() == SensorEvent.ACTIVE;
    }

    private static SensorPos p(int x, int y, int[]... i) {
        return new SensorPos(x, y, i);
    }
    
    /**
       
     */
    public class Train implements Runnable {

        private int id;
        private int speed;
        private int direction;
        private int ticket;
        private int tmpTicket = -1;

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
              while(true) { //TODO: while statement can't complete without throwing an exception?
                  SensorEvent e = tsi.getSensor(id);
                  passSensor(e, id - 1);
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

        public boolean tryAcc(int id) {
            int old = trackStatus[id].availablePermits();
            boolean r = trackStatus[id].tryAcquire();
            System.err.println("Train: " + this.id + "\tTried: " + id + " = " + r + ":" + old + "->" + (trackStatus[id].availablePermits()));
            return r;
        }

        public void release(int id) {
            trackStatus[id].release();
            System.err.println("Train: " + this.id + "\tReleased: " + id);
        }

        public void acc(int id) throws InterruptedException {
            trackStatus[id].acquire();
            System.err.println("Train: " + this.id + "\tAcquired: " + id);
        }

        private void passSensor(SensorEvent e, int trainId) throws CommandException, InterruptedException { // TODO: trainId is never used
            SensorPos p = getSensor(e);
            if (p != null) {
                if (e.getStatus() == SensorEvent.ACTIVE) {
                    if (p.getSections().length != 0) {
                        boolean gotTrack = false;
                        int[] sections = p.getSections()[direction];

                        if ((atSensor(e, 13, 7) || atSensor(e, 13, 8)) || (atSensor(e, 8, 11) || atSensor(e, 8, 13))) {
                            int sec = (atSensor(e, 13, 7) || atSensor(e, 13, 8)) ? 3: 6;
                            int dir = sec == 3 ? SOUTH: NORTH;
                            if (direction == dir) {
                                if (sec == 3) trackStatus[2].release();
                                if (!tryAcc(sec)) {
                                    TSimInterface.getInstance().setSpeed(id, 0);
                                    acc(sec);
                                    TSimInterface.getInstance().setSpeed(id, speed);
                                }
                                release(ticket);
                                ticket = sec;
                            } else
                                release(sec);
                            tmpTicket = -1;
                            gotTrack = true;
                            processSensorPass(e);
                        } else if (atSensor(e, 16, 9) || atSensor(e, 3, 9)) {
                            int dir = atSensor(e, 16, 9) ? SOUTH: NORTH;
                            if (direction == dir) {
                                int oldTicket = ticket;
                                if (tryAcc(4))
                                    ticket = 4;
                                else if (tryAcc(5))
                                    ticket = 5;
                                release(oldTicket);
                                gotTrack = true;
                                processSensorPass(e);
                            } else {

                                if (ticket == (SOUTH == dir ? 3: 6)) {
                                    gotTrack = true;
                                }
                            }
                        } else if (atSensor(e, 18 ,7) || atSensor(e, 1, 11)){
                            int sec = atSensor(e, 18, 7) ? 0: 7;
                            int dir = sec == 0 ? NORTH: SOUTH;
                            if (direction == dir) {
                                if (tryAcc(sec)) {
                                    ticket = sec;
                                } else if (tryAcc(sec + 1)) {
                                    ticket = sec + 1;
                                }
                            }
                            tmpTicket = -1;
                            gotTrack = true;
                            processSensorPass(e);
                        } else if (atSensor(e, 9, 9) || atSensor(e, 9, 10)) {
                            int sec = direction == NORTH ? 3: 6;
                            if (!tryAcc(sec)) {
                                TSimInterface.getInstance().setSpeed(id, 0);
                                acc(sec);
                                TSimInterface.getInstance().setSpeed(id, speed);
                            }
                            release(ticket);
                            ticket = sec;
                            tmpTicket = -1;
                            gotTrack = true;
                            processSensorPass(e);
                        }

                        if (!gotTrack) {
                            for (int i = 0; i < sections.length; i++) {
                                if (tmpTicket == -1 && trySectionAcquire(sections, i, p, e)) {
                                    gotTrack = true;
                                    processSensorPass(e);
                                    break;
                                } else if (tmpTicket != -1) {
                                    release(tmpTicket);
                                    System.err.printf("Train: %d\tLeft TMP: %d\tEntered: %d\n", id, tmpTicket, ticket);
                                    tmpTicket = -1;

                                    if (trySectionAcquire(sections, i, p, e) || sections[i] == ticket) {
                                        gotTrack = true;
                                        processSensorPass(e);
                                    }
                                    break;
                                } else if (sections[i] == ticket || sections[i] == tmpTicket) {
                                    gotTrack = true;
                                    processSensorPass(e);
                                    break;
                                }
                            }
                        }

                        if (!gotTrack) {
                            System.err.printf("Train: %d\tCould not find an empty track\n", id);
                            TSimInterface.getInstance().setSpeed(id, 0);
                            trackStatus[sections[0]].acquire();
                            trackStatus[ticket].release();
                            ticket = sections[0];
                            processSensorPass(e);
                            TSimInterface.getInstance().setSpeed(id, speed);
                        }
                    } else if (p.getSections().length == 0) {
                        System.err.printf("Train %d arrived at station\n", id);
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
        }

        private boolean trySectionAcquire(int[] sections, int i, SensorPos p, SensorEvent e) throws CommandException {
            System.err.println("Trackstatus for ticket " + sections[i] + " is status=" + trackStatus[sections[i]].availablePermits());
            if (ticket != sections[i] && trackStatus[sections[i]].tryAcquire()) {
                System.err.printf("Train: %d\tLeft: %d\tEntered: %d\tstatus=%d\n", id, ticket, sections[i], trackStatus[sections[i]].availablePermits());

                if (p.getSections().length == 3 && tmpTicket == -1) { //TODO: tmp ticket is always true?
                    System.err.printf("Train: %d\tEntered tmp track: %d\n", id, sections[i]);
                    tmpTicket = sections[i];
                } else {
                    System.err.printf("Train: %d\tReleased ticket for %d\n", id, ticket);
                    trackStatus[ticket].release();
                    ticket = sections[i];
                }
                return true;
            }
            return false;
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
            else if (atSensor(e, 8, 11) && direction == NORTH)
                switchSensor(3, 11, SWITCH_LEFT);
            else if (atSensor(e, 8, 13) && direction == NORTH)
                switchSensor(3, 11, SWITCH_RIGHT);
        }
        
        private SensorPos getSensor(SensorEvent e) {
            for (SensorPos p : SENSOR_POSITIONS) {
                if (p.getX() == e.getXpos() && p.getY() == e.getYpos()) {
                    return p;
                }
            }
            return null;
        }
        
    }
    
    /**
       
     */
    public static class SensorPos {
        private int x;
        private int y;
        private int[][] sections;
        
        public SensorPos(int x, int y, int[][] sections) {
            this.x = x;
            this.y = y;
            this.sections = sections;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int[][] getSections() {
            return sections;
        }
    }
  
}
