import TSim.*;

import java.util.concurrent.Semaphore;

public class Lab1 {

    private static final SensorPos[] SENSOR_POSITIONS = {
            p(18, 7, new int[]{0, 1}, new int[]{3}), p(8, 6, new int[]{1}, new int[]{2}, new int[]{1}), p(9, 7, new int[]{2}, new int[]{0}, new int[]{0}),
            p(16, 7, new int[]{0}, new int[]{3}),    p(8, 8, new int[]{2}, new int[]{1}, new int[]{1}), p(7, 7, new int[]{0}, new int[]{2}, new int[]{0}),
            p(17, 3, new int[]{1}, new int[]{3}),    p(16, 9, new int[]{3}, new int[]{4, 5}),           p(15, 10, new int[]{3}, new int[]{5}),
            p(3, 9, new int[]{4, 5}, new int[]{6}),  p(4, 10, new int[]{5}, new int[]{6}),              p(2, 11, new int[]{6}, new int[]{7, 8}),
            p(3, 12, new int[]{6}, new int[]{8}),    p(4, 11, new int[]{6}, new int[]{7}),
            p(16, 3), p(16, 5), p(16, 11), p(16, 13)};

    private static final int NORTH = 0;
    private static final int SOUTH = 1;

    private Semaphore[] trackStatus;
    private int[] trainPos;
    
    public Lab1(Integer speed1, Integer speed2) {
        trackStatus = new Semaphore[9];
        for (int i = 0; i < trackStatus.length; i++)
            trackStatus[i] = new Semaphore(1);
        trainPos = new int[]{0, 9};
        new Thread(new Train(1, speed1, SOUTH)).start();
        //new Thread(new Train(2, speed2, NORTH)).start();
    }

    public void switchSensor(SensorEvent e, int x, int y, int dx, int dy, int dir) throws CommandException {
      if (e.getXpos() == x && e.getYpos() == y && e.getStatus() == SensorEvent.ACTIVE) {
          TSimInterface.getInstance().setSwitch(x + dx, y + dy, dir);
      }
    }

    private static SensorPos p(int x, int y, int[]... i) {
        return new SensorPos(x, y, i);
    }
    
    public class Train implements Runnable {

        private int id;
        private int speed;
        private int direction;
        
        public Train(int id, int speed, int direction) {
            this.id = id;
            this.speed = speed;
            this.direction = direction;
        }
        
        @Override
        public void run() {
            TSimInterface tsi = TSimInterface.getInstance();

            try {
              tsi.setSpeed(id, speed);
              while(true) {
                  SensorEvent e = tsi.getSensor(id);
                  passSensor(e, id - 1);
                  switchSensor(e, 16, 7, 1, 0, TSimInterface.SWITCH_RIGHT);
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

        private void passSensor(SensorEvent e, int trainId) throws CommandException {
            SensorPos p = getSensor(e);
            if (p != null) {
                if (e.getStatus() == SensorEvent.ACTIVE) {
                    if (p.getSections().length != 0) {

                        //trackStatus[trainPos[trainId]];
                        //System.err.printf("Left: %d\tEntered: %d\n", trainPos[trainId], );
                        //trainPos[trainId] = p.section;
                    } else if (p.getSections().length == 0) {
                        System.err.printf("Train %d arrived at station\n", id);
                        TSimInterface.getInstance().setSpeed(id, 0);
                    }
                }
            }
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
