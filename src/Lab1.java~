import TSim.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Lab1 {

    public static final SensorPos[] SENSOR_POSITIONS = { p(7, 7, 0), p(8, 6, 1), p(9, 7, 2), p(16, 7, 2), p(8, 8, 3),
                                                         p(17, 8, 3), p(18, 7, 4), p(16, 9, 4), p(14, 9, 5), p(5, 9, 5),
                                                         p(15, 10, 6), p(4, 7, 6), p(3, 9, 7), p(2, 11, 7), p(4, 11, 8), 
                                                         p(3, 12, 9), p(16, 3, -1), p(16, 5, -1), p(16, 11, -1), p(16, 13, -1)};
    private static final int TRACK_OCCUPIED = 0;
    private static final int TRACK_UNOCCUPIED = 1;
    private int[] trackStatus;
    private int[] trainPos;
    
    public Lab1(Integer speed1, Integer speed2) {
        trackStatus = new int[10];
        trainPos = new int[2];
        new Thread(new Train(1, speed1)).start();
        //new Thread(new Train(2, speed2)).start();
    }

    public void switchSensor(SensorEvent e, int x, int y, int dx, int dy, int dir) throws CommandException {
      if (e.getXpos() == x && e.getYpos() == y && e.getStatus() == SensorEvent.ACTIVE) {
          TSimInterface.getInstance().setSwitch(x + dx, y + dy, dir);
      }
    }

    private static  SensorPos p(int x, int y, int s) {
        return new SensorPos(x, y, s);
    }
    
    public class Train implements Runnable {

        private int id;
        private int speed;
        
        public Train(int id, int speed) {
            this.id = id;
            this.speed = speed;
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
                    if (trainPos[trainId] != p.section && p.section != -1) {
                        trackStatus[trainPos[trainId]] = TRACK_UNOCCUPIED;
                        System.err.printf("Left: %d\tEntered: %d\n", trainPos[trainId], p.section);
                        trackStatus[p.section] = TRACK_OCCUPIED;
                        trainPos[trainId] = p.section;
                    } else if (p.section == -1) {
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
        private int section;
        
        public SensorPos(int x, int y, int section) {
            this.x = x;
            this.y = y;
            this.section = section;
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getSection() {
            return section;
        }
    }
  
}
