import java.util.concurrent.Semaphore;
import java.util.ArrayList;
import java.util.List;


import TSim.*;


public class Lab1 {
	
  private TSimInterface tsi;
  
  Semaphore trackSwitch15;
  Semaphore trackSwitch4;
  Semaphore trackSwitch3;
  Semaphore trackSwitch17;
  Semaphore trackSwitch17Up;
  Semaphore middleUp;                              //permit that helps us choose between up or down railway after getting a blocking switch permit
  Semaphore trackSwitch4Up;
  Semaphore trackSwitch3Up;                        //array that contains permit names 
  Semaphore interSection;
  String[] permitNames = new String[]{"trackSwitch15","trackSwitch4","trackSwitch3","trackSwitch17", "middleup", "trackSwitch3up","trackSwitch17up","intersection"};

  public Lab1(int speed1, int speed2) {
    tsi = TSimInterface.getInstance();  
    trackSwitch15 = new Semaphore(1);
    trackSwitch4 = new Semaphore(1);
    trackSwitch3 = new Semaphore(1);
    trackSwitch17 = new Semaphore(1);
    middleUp = new Semaphore(1);
    trackSwitch4Up = new Semaphore(1);
    trackSwitch3Up = new Semaphore(1);
    trackSwitch17Up = new Semaphore(1);
    interSection = new Semaphore(1);
    
    
    Thread train1 = new train(1,speed1, false,true);
    Thread train2 = new train(2,speed2, true,true);
    
  
    train1.start();
    train2.start();
   
  }
  
  public class train extends Thread {
	  int id;
	  int speed;
	  int maxSpeed = 15;
	  boolean southStation;     // true if train started at south station, false if started at north
	  List<String> permitsGranted = new ArrayList<>();
	  int timeCounter = 0;
	  boolean firstRun = true;
	  boolean stationUp;
	  
	  
  public train(int trainid, int speed, boolean station, boolean Up) {
	  this.id = trainid;
	  if(speed <= maxSpeed) {
		  this.speed = speed;
	  }
	  else {
		  this.speed = maxSpeed;
	  }
	  this.southStation = station; 
	  this.stationUp = Up;
  }
  
  public void acquirePermit(Semaphore permit, int i) throws InterruptedException {
	  permit.acquire();                      
	  permitsGranted.add(permitNames[i]);
	  
  }
  
  public void releasePermit(Semaphore permit,int i) {
	  permit.release();
	  permitsGranted.remove(permitNames[i]);
  }
  
  
  public void waitAtTrainStation(boolean station) throws InterruptedException, CommandException {
	  tsi.setSpeed(id, 0);
	  this.southStation = station;
	  sleep(1000 +(20*Math.abs(speed)));
	  this.speed = -speed;
	  tsi.setSpeed(id, speed);
  }
  
  //acquire the permit of a switch, get access to either the upper or lower railway connected to given switch
  public void chooseBetweenTwoPaths(Semaphore switchPermit,Semaphore railwayPermit, int railwayPermitIndex ,int dir, int dir2, int semaphoreIndex, int xpos, int ypos) throws InterruptedException, CommandException {
	  acquirePermit(switchPermit, semaphoreIndex);
      if(permitsGranted.contains(permitNames[semaphoreIndex])) {
    	 if(railwayPermit.tryAcquire()) {
    	  permitsGranted.add(permitNames[railwayPermitIndex]);
    	  tsi.setSwitch(xpos, ypos, dir);
    	  
      }
    	 else {
    		 tsi.setSwitch(xpos,ypos,dir2 );
    	 }	 
    }
   }
  
  //get permit for switch, get access to single railway connected to switch
  public void chooseOnePath(Semaphore switchPermit, int dir, int semaphoreIndex, int xpos, int ypos) throws CommandException, InterruptedException {
	  tsi.setSpeed(id, 0);                      // then train must acquire permit for switch 15 and 17
	  acquirePermit(switchPermit, semaphoreIndex);
      if(permitsGranted.contains(permitNames[semaphoreIndex])) {
    	  tsi.setSwitch(xpos, ypos, dir);
    	  
    }  
  }
  
  //routine for all sensors
  public void checkSensor(SensorEvent sensors) throws CommandException, InterruptedException {
	  if(sensors.getStatus() == SensorEvent.ACTIVE) {
		  
 //south & north station -> wait at the station, update southStation instance boolean variable,true if from south station, false if from north
		  
		  //north station
		  if(sensors.getXpos() == 15 && sensors.getYpos() == 3 || sensors.getXpos() == 15 && sensors.getYpos() == 5) {
			  if(southStation) {
			  waitAtTrainStation(false); }
		}
		  
		  //south station
		  if(sensors.getXpos() == 15 && sensors.getYpos() == 11 || sensors.getXpos() == 15 && sensors.getYpos() == 13) {
			  
			  if(!southStation) {
				  waitAtTrainStation(true);  
			  }
		 }
		  
		  
 //routine for intersection sensors -> move only when acquired intersection permit
		  if(sensors.getXpos() == 10 && sensors.getYpos() == 7 || sensors.getXpos() == 6 && sensors.getYpos() == 7 || sensors.getXpos() == 10 && sensors.getYpos() == 8 || sensors.getXpos() == 8 && sensors.getYpos() == 5  ) {
			  if(permitsGranted.contains(permitNames[7])) {
			  releasePermit(interSection,7);
			  }
			  else {
				  tsi.setSpeed(id,0);
				  acquirePermit(interSection,7);
				  tsi.setSpeed(id, speed);
			  }  
		  }
		  
//sensors near switch 17: train coming from north station upper or lower railway -> get permit for switch 17 & 15, if middle-upper railway available then get middleUp permit
//train coming from south -> release switch 15 and 17 permits return switches to default direction
		  if(sensors.getXpos() == 14 && sensors.getYpos() == 7 || sensors.getXpos() == 15 && sensors.getYpos() == 8) {
			  if(!southStation) {   
			   if(sensors.getXpos() == 14 && sensors.getYpos() == 7) {
			  chooseOnePath(trackSwitch17,TSimInterface.SWITCH_RIGHT,3,17,7);}
			   else {
				   chooseOnePath(trackSwitch17,TSimInterface.SWITCH_LEFT,3,17,7);   
			   }
			  chooseBetweenTwoPaths(trackSwitch15,middleUp, 4,TSimInterface.SWITCH_RIGHT,TSimInterface.SWITCH_LEFT,0,15,9);
			  tsi.setSpeed(id, speed);
			 }
			  else {
				  releasePermit(trackSwitch17,3);
				  releasePermit(trackSwitch15,0);                          	 
				  tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
				  tsi.setSwitch(17,7,TSimInterface.SWITCH_LEFT );
			  }
		  }
		  
//single sensor on the single railway connected to each switch, used to drop the upper railway permits connected to each switch when exiting 
		  if(sensors.getXpos() == 16 && sensors.getYpos() == 9 || sensors.getXpos() == 3 && sensors.getYpos() == 9 || sensors.getXpos() == 18 && sensors.getYpos() == 7 || sensors.getXpos() == 2 && sensors.getYpos() == 11) {
			  if(!southStation) {   
			   if(sensors.getXpos() == 3 && sensors.getYpos() == 9 && permitsGranted.contains(permitNames[4])) {
					 releasePermit(middleUp,4);
			    }
			   if(sensors.getXpos() == 18 && sensors.getYpos() == 7 && permitsGranted.contains(permitNames[6])) { 
					 releasePermit(trackSwitch17Up,6);
					 }
			   }
			  else {
				  if(sensors.getXpos() == 16 && sensors.getYpos() == 9 && permitsGranted.contains(permitNames[4])) {
						 releasePermit(middleUp,4);
				   }  
				  if(sensors.getXpos() == 2 && sensors.getYpos() == 11 && permitsGranted.contains(permitNames[5]) ){ 
						releasePermit(trackSwitch3Up,5);}
			  }
		 }
	 	  
// sensors near switch 15: train coming from north -> release switch 15 & 17 permit, switches return to default direction	
//train coming from south -> upper or lower railway get permit to switch 15 & 17, get permit for trackSwitch17Up, if not available then use bottom railway connected to switch 17		  
		  if(sensors.getXpos() == 12 && sensors.getYpos() == 9 || sensors.getXpos() == 13 && sensors.getYpos() == 10) {
			  if(!southStation) {                            //if train is coming from north station
				    releasePermit(trackSwitch15,0);
				    releasePermit(trackSwitch17,3);                           
				    tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
					tsi.setSwitch(17,7,TSimInterface.SWITCH_LEFT );
				 }
			  
		    else {
		    	if(sensors.getXpos() == 12 && sensors.getYpos() == 9){
		    	chooseOnePath(trackSwitch15,TSimInterface.SWITCH_RIGHT,0,15,9);
		    	}
		    	else {
		    		chooseOnePath(trackSwitch15,TSimInterface.SWITCH_LEFT,0,15,9);	
		    	}
		    	chooseBetweenTwoPaths(trackSwitch17,trackSwitch17Up, 6,TSimInterface.SWITCH_RIGHT,TSimInterface.SWITCH_LEFT,3,17,7);
		    	tsi.setSpeed(id, speed);
				}                                                          
		    }
		 		  
//sensors near switch 4: train coming from north -> middle upper or middle bottom railway gets permit for switch 4 & 3, acquire permit for upper railway connected to switch 3 if available if not choose lower railway	
//train coming from south -> release permit for switch 3 & 4 ,switches return to default direction	  
		  if(sensors.getXpos() == 7 && sensors.getYpos() == 9 || sensors.getXpos() == 6 && sensors.getYpos() == 10) {
			  if(!southStation) {   
				     if(sensors.getXpos() == 7 && sensors.getYpos() == 9) {
				    	 chooseOnePath(trackSwitch4,TSimInterface.SWITCH_LEFT,1,4,9);
				     }
				     else {
				    chooseOnePath(trackSwitch4,TSimInterface.SWITCH_RIGHT,1,4,9);
				    }
				    chooseBetweenTwoPaths(trackSwitch3, trackSwitch3Up,5,TSimInterface.SWITCH_LEFT,TSimInterface.SWITCH_RIGHT,2,3,11);
				    tsi.setSpeed(id, speed);
				 }
			  
			  else {
				  releasePermit(trackSwitch4,1);
				  releasePermit(trackSwitch3,2);                           
				  tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
				  tsi.setSwitch(3,11,TSimInterface.SWITCH_LEFT );
			  }
		    }
		  
// sensors near switch 3: train coming from north -> release permit for switch 3 & 4, return switch to default direction
//train coming from south -> get permit for switch 3 & 4, try to get permit for middle up railway otherwise choose bottom railway connected to switch 4		  
		  if(sensors.getXpos() == 6 && sensors.getYpos() == 11 || sensors.getXpos() == 5 && sensors.getYpos() == 13) {
			  if(southStation) {                            
				  if(sensors.getXpos() == 6 && sensors.getYpos() == 11 ) {
				  chooseOnePath(trackSwitch3,TSimInterface.SWITCH_LEFT,2,3,11);
				  }
				  else {
					  chooseOnePath(trackSwitch3,TSimInterface.SWITCH_RIGHT,2,3,11);  
				  }
				  chooseBetweenTwoPaths(trackSwitch4, middleUp,4,TSimInterface.SWITCH_LEFT,TSimInterface.SWITCH_RIGHT,1,4,9);
				  tsi.setSpeed(id, speed);
			 }
			  else {
				  releasePermit(trackSwitch4,1);
				  releasePermit(trackSwitch3,2);                          
				  tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
				  tsi.setSwitch(3,11,TSimInterface.SWITCH_LEFT );
			  }
		  }
	}//sensor event active	  
}//check sensor method
  public void run() {
	  
	  try {
		  if(firstRun) {
			  if(this.stationUp == true) {              // if station is initially on stations upper railway
			  if(this.southStation == true) {
				  acquirePermit(trackSwitch3Up,5);
			  }
			  else {
				  acquirePermit(trackSwitch17Up,6);
			  }
			}
			  this.firstRun = false;
		  }
      tsi.setSpeed(id,speed);
      
      while(true) {
      SensorEvent sensors = tsi.getSensor(id); // returns a sensor event representing info about event
      checkSensor(sensors);
      }
    }
    catch (CommandException e) {
      e.printStackTrace();    // or only e.getMessage() for the error
      System.exit(1);
    } catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		System.exit(1);
	}
   }
   }
  }

