import TSim.*;

import java.util.concurrent.Semaphore;
import static TSim.TSimInterface.SWITCH_RIGHT;
import static TSim.TSimInterface.SWITCH_LEFT;

public class Lab1 {

    private static final SensorPos[] SENSOR_POSITIONS = {
            p(18, 7, new int[]{0, 1}, new int[]{3}, new int[]{3}), p(8, 6, new int[]{1}, new int[]{2}, new int[]{1}), p(9, 7, new int[]{2}, new int[]{0}, new int[]{0}),
            p(16, 7, new int[]{0}, new int[]{3}, new int[]{3}),    p(8, 8, new int[]{2}, new int[]{1}, new int[]{1}), p(7, 7, new int[]{0}, new int[]{2}, new int[]{0}),
            p(17, 3, new int[]{1}, new int[]{3}),                  p(16, 9, new int[]{3}, new int[]{4, 5}),           p(15, 10, new int[]{3}, new int[]{5}),
            p(3, 9,  new int[]{4, 5}, new int[]{6}),               p(4, 10, new int[]{5}, new int[]{6}),              p(2, 11, new int[]{6}, new int[]{7, 8}),
            p(3, 12, new int[]{6}, new int[]{8}),                  p(4, 11, new int[]{6}, new int[]{7}),              p(17, 8, new int[]{1}, new int[]{3}, new int[]{3}),
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
              while(true) {
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

        private void passSensor(SensorEvent e, int trainId) throws CommandException, InterruptedException {
            SensorPos p = getSensor(e);
            if (p != null) {
                if (e.getStatus() == SensorEvent.ACTIVE) {
                    if (p.getSections().length != 0) {
                        int[] sections = p.getSections()[direction];
                        for (int i = 0; i < sections.length; i++) {
                            if (ticket != sections[i] && trackStatus[sections[i]].tryAcquire()) {
                                System.err.printf("Left: %d\tEntered: %d\n", ticket, sections[i]);

                                if (tmpTicket == -1 && p.getSections().length == 3) {
                                    if (ticket == 3 && direction == NORTH) {
                                        tmpTicket = ticket;
                                        ticket = sections[i];
                                    } else
                                        tmpTicket = sections[i];
                                } else {
                                    System.err.printf("Released ticket for %d\n", ticket);
                                    trackStatus[ticket].release();
                                    ticket = sections[i];
                                }

                                processSensorPass(e);
                                break;
                            } else if (p.getSections().length == 3 && tmpTicket != -1) {
                                processSensorPass(e);
                                trackStatus[tmpTicket].release();
                                System.err.printf("Left TMP: %d\tEntered: %d\n", tmpTicket, ticket);
                                tmpTicket = -1;
                            } else if (i+1 >= sections.length && ticket != sections[i] && tmpTicket != sections[i]) {
                                System.err.println("Could not find an empty track");
                                TSimInterface.getInstance().setSpeed(id, 0);
                                trackStatus[sections[0]].acquire();
                                ticket = sections[i];
                                TSimInterface.getInstance().setSpeed(id, speed);
                            }
                        }
                    } else if (p.getSections().length == 0) {
                        System.err.printf("Train %d arrived at station\n", id);
                        TSimInterface.getInstance().setSpeed(id, 0);
                    }
                }
            }
        }

        private void processSensorPass(SensorEvent e) throws CommandException {
            if (atSensor(e, 18, 7) && direction == NORTH && ticket == 1)
                switchSensor(17, 7, SWITCH_LEFT);
            else if (atSensor(e, 17, 8) && direction == NORTH)
                switchSensor(17, 7, SWITCH_RIGHT);
            //else if (atSensor(e, 16, 7) && direction == SOUTH)
                //switchSensor(17, 7, SWITCH_RIGHT);


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
