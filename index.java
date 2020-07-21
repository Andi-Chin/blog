import java.util.*;

final int frame_rate = 60;

// number of communities
int num_communities = 3;

// flocking_dist: distance two particles have to be within to trigger flocking
int flocking_dist = 50;
// the distance 2 particles have to be within to trigger social distancing
float repulsion_dist = 10;

// radius of the particles, used for collision detection, sometimes it's used in drawing
// recover_time: time needed for particle to recover; for each particle, the time count since infection is incremented every frame, once infected_time goes over recover_time, the particle recovers
float particle_radius = 10, recover_time = 1000;

// infection_radius: radius to particles have to be within to spread the virus
float infection_radius = 20;
float infection_radius_maximum = 50;

// infection_chance: chance of infection, a number from 0 to 1, on initial contact and every half second after
// chance_of_death: chance that a particle will die rather than recover
// chance_of_losing_immunity: chance of a particle going back to the normal, susceptible state after recovering rather than becoming immune
float default_infection_chance = 0.2;
float infection_chance_maximum = 1;
float chance_of_death = 0.1;
float chance_of_losing_immunity = 0.05;

boolean show_infection_radius = true;
boolean socialDistancingDefault = false;
boolean quarantine_default = false;
boolean haveShoppers_default = true;

// list of communities containing particles
Community[] communities;

// community position technicalities
int community_size = 500;
int community_offset_y = 200;
int community_offset_x =  100;
int community_columns = 2;

// graph variables and arrays
int num = 50;
int lastInfected = 0;
int everyFrame = 100;
int goingUp = 0;
float[] array = new float[40];
float[] arrayC = new float[40];
float[] lastPoints = new float[3];
color arr = color(255, 0, 0);
float[] deadArray = new float[40];
color dea = color(120);
boolean same = false;
boolean started = false;
int startFrame = 0;
float[] linRegVal = new float[40];
color lin = color(150, 150, 0);
float[] quadRegVal = new float [40];
color qua = color(0, 150, 150);
float[] expRegVal = new float[40];
color exp = color (150, 0, 150);
float[] time = new float[40];
boolean reg = false;
int timesDone = 0;
boolean isInfection = false;
int lastX = 0;
int lastY = 0;

// graph location, label locations, and size
int graph_origin_x = 800;
int graph_origin_y = 1245;
int graph_size_y = 445;

// slider techincalities
int slider_length_y = 40;
int slider_offset_y = 120;

// sliders
VerticalSlider infectionRadius;
Slider[] infectionChances;

// toggle buttons
toggleButton[] socialDistancingToggles;
toggleButton[] quarantineToggles;
toggleButton[] haveShoppers;

toggleButton panningToggle;
toggleButton startButton;
toggleButton graphPredictions;

// toggle button technicalities
float button_offset_y = 15;
int button_offset_x = 10;
int button_size_y = 30;
int button_size_x = 160;

// panning variables
float mx,my,ratio,xpt,ypt,xzt,yzt,swt,zoom;

// current relative origin: (xpt - xzt, ypt - yzt)
// current relative mouseX, mouseY: (xpt - xzt + mouseX, ypt - yzt + mouseY)

// this is only called once, at the beginning of the program
void setup() {
  // these numbers have to be hard-coded
  // width and height is the size of the entire screen, it is automatically set after calling size
  size(1500, 1500);
  frameRate(frame_rate);

  // setting up panning variables
  zoom=1.0;
  mx=width/60;
  my=mx*height/width;
  ratio=mx/my;

  // constructing communities
  communities = new Community[num_communities];
	for (int i = 0; i < num_communities; i ++) {
    float left = 100 + community_offset_x + (community_size + community_offset_x)*(i % community_columns);

    float bottom = -100 + community_offset_y + (community_size + community_offset_y)*(i / community_columns) - (i % community_columns)  * (community_size + community_offset_y) / community_columns;

    float right = left + community_size;
    float top = bottom + community_size;

    communities[i] = new Community(left, right, bottom, top, default_infection_chance, socialDistancingDefault,quarantine_default,haveShoppers_default);
  }

  infectionChances = new Slider[num_communities];

  socialDistancingToggles = new toggleButton[num_communities];
  quarantineToggles = new toggleButton[num_communities];
  haveShoppers = new toggleButton[num_communities];

  startButton = new toggleButton("Start Simulation",false,300,300,true,0);
  panningToggle = new toggleButton("Panning",false,0,45,true,0);

  graphPredictions = new  toggleButton("Predictions",false,graph_origin_x,graph_origin_y +  20,false,20);

  for (int i = 0; i < communities.length; i++) {
    Community c = communities[i];
    infectionChances[i] = new Slider("Chance of Infection", c.infection_chance,
        c.community_left,c.community_right, c.community_top, infection_chance_maximum);

    socialDistancingToggles[i] = new toggleButton("Distancing",c.socialDistancing,c.community_left-10,c.community_top, false,20);

    quarantineToggles[i] = new toggleButton("Quarantine",c.quarantine,c.community_left + button_offset_x + button_size_x-10,c.community_top,false,20);

    haveShoppers[i] = new toggleButton("Shoppers",c.shopping,c.community_left + button_offset_x * 2 + button_size_x * 2-10, c.community_top,false,20);
  }

  infectionRadius = new VerticalSlider("Radius of Infection", infection_radius, 100, 
                                        610,100,infection_radius_maximum);

}


// this is called every frame
void draw() { 

  background(255,239,213);

  if(!startButton.value){
    startButton.update();
  }
  
  panningToggle.update();

  // panning
  scale(zoom);
  translate(xpt-xzt,ypt-yzt);

  textSize(20);
  textAlign(LEFT);
  strokeWeight(2);
  stroke(0);
  
  if(startButton.value) {
    if(!started) {
      startFrame = frameCount;
      started = true;
    }
    int total_currently_infected = 0;
    int total_currently_dead = 0;

    for (Community c : communities) {
      c.nextFrame();
      total_currently_infected += c.num_currently_infected;
      total_currently_dead += c.dead_particles.size();

      fill(0,0,0);
      
      if (c.num_recovered > 0) {
        c.R0 = round(c.overall_infected/c.num_recovered*100)/100.0;
      }
      
      text("R{0} = " + c.R0, c.community_left,c.community_bottom - 5);
    }
    
    // graph stuff
    noFill();
    line(graph_origin_x, graph_origin_y-graph_size_y, graph_origin_x, graph_origin_y);
    line(graph_origin_x,graph_origin_y,1300,graph_origin_y);
    fill(1,1,1);
    text("400", 750, 810);
    text("0", 775, 1250);
    text("current", 1300, 1275);
    text("-60", 790, 1275);
    text("time (sec)", 1025, 1300);
    noFill();
    rect(1170, 1300, 210, 200);
    fill(1,1,1);
    text("Key", 1200, 1325);
    text("Dead", 1200, 1350);
    text("Infected", 1200, 1375);
    text("Linear Regression", 1200, 1400);
    text("Quadratic Regression", 1200, 1425);
    text("Exponential Regression", 1200, 1450);
    fill(arr);
    ellipse(1180, 1367, 10, 10);
    fill(dea);
    ellipse(1180, 1342, 10, 10);
    fill(lin);
    ellipse(1180, 1392, 10, 10);
    fill(qua);
    ellipse(1180, 1417, 10, 10);
    fill(0, 255, 255);
    ellipse(1180, 1442, 10, 10);
    if((frameCount - startFrame) > 2000){
      if((array[39] - array[30] == 0) && (deadArray[39] - deadArray[30] == 0)){
        same = true;
      }
    }
    if(frameCount % everyFrame == 0 && !same){
      timesDone++;
      arrayC = array;
      for(int i = 0; i < array.length-1; i++){
         linRegVal[i] = 0;
         expRegVal[i] = 0;
        quadRegVal[i] = 0;
      }
      for( int i = 0; i < lastPoints.length-1; i++){
        for(int j = lastPoints.length; j > 0; j--){
          lastPoints[i] = array[array.length - j];
        }
      }
			int lastPoints_i = 0;
			for (int i = array.length - lastPoints.length; i < array.length; i ++) {
				lastPoints[lastPoints_i ++] = array[i];
			}

      float[] linReg = linearRegression(time, arrayC);
      float linM = linReg[0];
      float linB = linReg[1];
      float[] expReg = expRegression(time, arrayC);
      float expA = expReg[0];
      float expR = expReg[1];
      float[] quadReg = quadRegression(time, arrayC);
      float quadA = quadReg[0];
      float quadB = quadReg[1];
      float quadC = quadReg[2];
      for (int i=0; i<array.length-1; i++) {
        time[i] = time[i+1];
        array[i] = array[i+1];
        deadArray[i] = deadArray[i+1];
        linRegVal[i] = linRegVal[i+1];
        expRegVal[i] = expRegVal[i+1];
        quadRegVal[i] = quadRegVal[i+1];
      }
      array[array.length-1] = (float) total_currently_infected;
      deadArray[array.length-1] = (float) total_currently_dead;
      time[time.length-1] = millis()/1000;
      for (int i=0; i < 40; i++){
        if((frameCount - startFrame) > 100){
          linRegVal[i] = (linM * time[i]) + linB;
          //expRegVal[i] = (expA*(pow(expR, time[i])));
          quadRegVal[i] = (quadA*pow(time[i], 2)) + (quadB * time[i]) + quadC;
        }
      }

    }
    if(same){
      text("Graph is now constant, Graph updates have ceased ", 1000, 900);
    }
    isInfection = true;
    graphing(array, arr);
    isInfection = false;
    graphing(deadArray, dea);
    reg = true;
    if(graphPredictions.value){
      graphing(linRegVal, lin);
      graphing(quadRegVal, qua);
    }

    reg = false;
    
    fill(0,0,0);
    text("Current Infected: " + (int) total_currently_infected, 800, 850+500);
    text("People Dead: " + (int) total_currently_dead, 830, 850+550);
  }

  // sliders and toggle buttons
  infectionRadius.update();
  infection_radius = infectionRadius.get();

  graphPredictions.update();

  for(int i = 0; i < communities.length; i++){
    infectionChances[i].update();
    communities[i].infection_chance = infectionChances[i].get();

    socialDistancingToggles[i].update();
    communities[i].socialDistancing = socialDistancingToggles[i].value;

    haveShoppers[i].update();
    communities[i].shopping = haveShoppers[i].value;

    boolean prevQuarVal = quarantineToggles[i].value;
    quarantineToggles[i].update();
    communities[i].quarantine = quarantineToggles[i].value;
    if (!prevQuarVal && quarantineToggles[i].value) {
      communities[i].quarantine_distancing = true;
    }
  }
	projectionLinear(lastX, lastY);
	projectionExponential(lastX, lastY);
	projectionQuad(lastX, lastY);
}

// returns the linear equation of best fit in the form of y = m * x + b
// returns m and b in an array of float
// (time, infection number)
// x = {times}
// y = {infection numbers}
float[] linearRegression(float[] x, float[] y) {
	float n = x.length;
	float Ex = 0, Ey = 0, xy = 0, x2 = 0;
	for (int i = 0; i < n; i ++) {
		Ex += x[i];
		Ey += y[i];
		xy += x[i] * y[i];
		x2 += x[i] * x[i];
	}
	float m = (n * xy - Ex * Ey) / ((n * x2) - Ex * Ex);
	float b = (Ey - m * Ex) / n;
	return new float[] {m, b};
}

// returns equation in the form y = A * r ^ x
float[] expRegression(float[] x, float[] yy) {
	float y[] = new float[yy.length];
	for (int i = 0; i < yy.length; i ++) y[i] = yy[i];
	for (int i = 0; i < x.length; i ++) y[i] = (float) Math.log10(y[i]);
	float[] linear = linearRegression(x, y);
	float r = (float) Math.pow(10, linear[0]);
	float A = (float) Math.pow(10, linear[1]);
	return new float[] {A, r};
}

// accepts some previous points (x, y) and outputs the best fit quadratic function to predict future
// in the form of a * x ^ 2 + b * x + c
float[] quadRegression(float[] x, float[] y) {
	float n = x.length;
	float Ex = 0, Ey = 0, Ex2 = 0, Ex3 = 0, Ex4 = 0, Exy = 0, Ex2y = 0;
	for (int i = 0; i < n; i ++) {
		Ex += x[i];
		Ey += y[i];
		Ex2 += x[i] * x[i];
		Ex3 += Math.pow(x[i], 3);
		Ex4 += Math.pow(x[i], 4);
		Exy += x[i] * y[i];
		Ex2y += x[i] * x[i] * y[i];
	}
	float Exx = Ex2 - Ex * Ex / n;
	Exy = Exy - Ex * Ey / n;
	float Exx2 = Ex3 - Ex2 * Ex / n;
	Ex2y = Ex2y - Ex2 * Ey / n;
	float Ex2x2 = Ex4 - Ex2 * Ex2 / n;
	float a = (Ex2y * Exx - Exy * Exx2) / (Exx * Ex2x2 - Exx2 * Exx2);
	float b = (Exy * Ex2x2 - Ex2y * Exx2) / (Exx * Ex2x2 - Exx2 * Exx2);
	float c = Ey / n - b * Ex / n - a * Ex2 / n;
	return new float[] {a, b, c};
}

// handling panning 
void mouseDragged() {
  if(panningToggle.value){
    xpt-=(pmouseX-mouseX)/zoom;
    ypt-=(pmouseY-mouseY)/zoom;
  }
}

// handling zooming
// accepts 1 or -1 for count
// 1 is zoom out, -1 is zoom in
void keyPressed() {
  int count;
  // see "https://keycode.info/" for getting character keycodes
  // if the user presses a
  if (keyCode == 65) count = 1;
  // if the user presses d
  else if (keyCode == 68) count = -1;
  else return;

  swt-=count;
  if (swt==0) {
    zoom=1.0;
  } else if (swt>=1 && swt<=10) {
    zoom=pow(2, swt);
  } else if (swt<=-1 && swt>=-10) {
    zoom=1/pow(2, abs(swt));
  }
  xzt=((zoom-1)*width/2)/zoom;
  yzt=((zoom-1)*height/2)/zoom;
  if(count<=-1){
    xpt-=(mouseX-width/2)/zoom;
    ypt-=(mouseY-height/2)/zoom;
  } else {
    xpt+=(mouseX-width/2)/(pow(2, swt+1));
    ypt+=(mouseY-height/2)/(pow(2, swt+1));
  }
}

class Community {
  float infection_chance, overall_infected, num_recovered, R0;
  boolean socialDistancing, quarantine, quarantine_distancing, shopping;

  // num_centers: number of party centers
  int num_centers = 2, num_currently_infected;

  // default_num_particles: starting amount of particles
  // quar_distancing_frames: used to keep track of how long a community has been quarantining for
  int default_num_particles = 100, quar_distancing_frames;
  // locations of the left and right and top and bottom of the community
  // note that process uses a different coordinate system
  // so (0, 0) is the top left corner
  // which mean that community_top will refer to the 
  float community_left, community_right, community_top, community_bottom;

  ArrayList<Particle> particles;
  ArrayList<Particle> dead_particles;

  // radius of centers
  int center_radius = 50;
  // location of centers
  PVector[] centers;

  Community(float com_left, float com_right, float com_bot, float com_top, float infect_chance, boolean socialDistancing, boolean quarantine, boolean isShopping) {
    infection_chance=infect_chance;
    this.socialDistancing = socialDistancing;
    this.quarantine = quarantine;
    quarantine_distancing = quarantine;
    shopping = isShopping;

    quar_distancing_frames = 0;
    num_recovered = 0;
    overall_infected = 0;
    R0 = 0;
    dead_particles = new ArrayList<Particle>();

    community_left = com_left; community_right = com_right;
    community_top = com_top; community_bottom = com_bot;
    centers = new PVector[num_centers];
    for (int i = 0; i < num_centers; i ++) {
      centers[i] = new PVector(random(community_left + center_radius, community_right - center_radius), random(community_bottom + center_radius, community_top - center_radius));
    }
    particles = new ArrayList<Particle>();
    for (int i = 0; i < default_num_particles; i ++) {
      float xVal = random(community_left + particle_radius, community_right - particle_radius);
      float yVal = random(community_bottom + particle_radius, community_top - particle_radius);
      particles.add(new Particle(new PVector(xVal, yVal), this));
    }
    // one unlucky person is the source of infection
    particles.get((int) random(particles.size())).state = 1;
  }

  void startGathering() {
    for(int i = 0; i < particles.size()/8; i++) {
      Particle p = particles.get((int) random(particles.size()));
      if (!p.gatherer && !p.shopper && !p.adventurer) {
        p.makeGatherer();
      }
    }
  }

  void socialDistance(int i, int j) {
    // the difference vector between the 2 particles
    PVector diff = PVector.sub(particles.get(i).pos, particles.get(j).pos);
    // scale the vector so the attraction force is the same every time
    diff.normalize(); diff.div(2);

    // give particle i a little push away from particle j
    particles.get(i).applyForce(diff);
    // reverses the force
    diff.mult(-1);
    // give particle j a little push towards particle i
    particles.get(j).applyForce(diff);
  }
  void flocking(Particle p1, Particle p2) {
    // the difference vector between the 2 particles
    PVector diff = PVector.sub(p1.pos, p2.pos);
    // scale the vector so the attraction force is the same every time
    diff.normalize(); diff.div(12);
    // give particle j a little push towards particle i
    p2.applyForce(diff);
    // reverses the force
    diff.mult(-1);
    // give particle i a little push towards particle j
    p1.applyForce(diff);
  }
  void collisionCheck() {
    // check collision with other particles by considering them pair by pair
    // i: the first person in consideration
    for (int i = 0; i < particles.size(); i ++) {
      Particle p1 = particles.get(i);
      // j: the second person in consideration
      for (int j = 0; j < i; j ++) {
        Particle p2 = particles.get(j);
        // distance between person i and j
        float dis = p1.pos.dist(p2.pos);
        
        // social distance (not perfect since irl it isn't strictly enforced)
        if ((socialDistancing || quarantine_distancing) && dis < particle_radius * 2 + repulsion_dist) {
          socialDistance(i, j);
        }
        
        if (quarantine_distancing && i == particles.size()-1 && j == i-1) {
          quar_distancing_frames++;
          if (quar_distancing_frames > 60) {
            quarantine_distancing = false;
            quar_distancing_frames = 0;
          }
        }

        // flocking
        // if both gatherers and close enough
        else if (p1.gatherer && p2.gatherer && dis < flocking_dist) {  
          flocking(p1, p2);
        }
        // infection
        // if one is infected
        boolean p1_infected = p1.state == 1;
        boolean p2_infected = p2.state == 1;
        // if not a recovered patient
        boolean not_recovered = p1.state != 2 && p2.state != 2;
        // if close enough
        boolean close = dis < infection_radius;
        // if the two particles have failed to infect each other in the last half second
        boolean recently_failed = p1.recent_infect_fails.containsKey(p2);

        if(recently_failed) {
          // .replace() wasn't working in replit
          // current number of frames since last infection attempt
          int curr_val = p1.recent_infect_fails.get(p2);
          // increases number of frames since last infection attempt by 1
          p1.recent_infect_fails.remove(p2);
          p1.recent_infect_fails.put(p2, curr_val+1);

          // removes particle from recentInfectFails after half a second
          if(p1.recent_infect_fails.get(p2) >= (frame_rate/2)) {
            p1.recent_infect_fails.remove(p2);
          }
        }

        // checks if infection is possible
        if (((p1_infected && !p2_infected) || (!p1_infected && p2_infected)) && not_recovered && close) {
          if (!recently_failed) {
            // attempts to infect
            if (random(0, 1) < infection_chance) {
              if (p1_infected) {
                p1.infect(p2);
              }
              else p2.infect(p1);
            }
            // adds particle to recent_infect_fails if infection fails
            else p1.recent_infect_fails.put(p2, 0);
          }
        }
      }
    }
  }
  // next frame for every community center
  void nextFrame() {
    collisionCheck();
    // draw community border
    noFill();
    stroke(139,69,19);

    rect(community_left, community_bottom, community_right - community_left, community_top - community_bottom);

    // draw the party centers
    for (PVector p : centers) {
      fill(255, 255, 0);
      ellipse(p.x, p.y, center_radius, center_radius);
    }

    if (!quarantine && random(0, 1) < 0.002) {
      startGathering();
    }

    stroke(1);
    num_currently_infected = 0;
    for (int i = 0; i < particles.size(); i ++) {
      particles.get(i).nextFrame();

      if(particles.get(i).state == 3) {
        particles.remove(i);
        continue;
      }
      else if (particles.get(i).state == 1) {
        num_currently_infected++;
      }

      // checking if the travelling particle is at its destination community
      if (particles.get(i).traveller && particles.get(i).pos.dist(particles.get(i).target_location) < 10) {
        particles.get(i).traveller = false; 
        particles.get(i).init_pos = particles.get(i).pos.get();
        particles.get(i).vel.mult(0);
        
        if (particles.get(i).adventurer == true) {
           particles.get(i).adventurer = false;
           Community second = particles.get(i).to_community; 
           particles.get(i).community = second;
           second.particles.add(particles.get(i));
           particles.get(i).recent_infect_fails.clear();
           particles.remove(i);
           i--;
        }
      }
    }

    for (Particle p : dead_particles) {
      p.nextFrame();
    }
  }
}

class Particle {
  // pos: current position
  // vel: current velocity
  // acc: current acceleration
  // init_pos: initial position of the particle, used to determine where it's quarantined
  // target center: if this person is a partier, they will go to this center
  // target_location: a place the person is trying to reach, whether it's going back to their house or to a party center
  PVector pos, vel, acc, init_pos, target_center, target_location;
  // partier: goes to a centeral location
  // gatherer: tends to group together with other gatherers and exhibits flocking behavior
  // traveller: a person who travels from one community to another
  // self_isolation: an infected person realizes it and stays in one place
  // debug: special treatment for particles that needs debugging
  boolean shopper, gatherer, traveller, adventurer, self_isolation, debug;
  // state: 0 is susceptible, 1 is infected, 2 is recoverd
  // infected_time: time elapsed since initially infected
  int state, infected_time, num_infected, gatherer_time;

  // community: the community it belongs to
  // to_community: the community it is travelling to if it is a traveller
  Community community, to_community;

  // recentInfectFails: the particles which this has recently failed to infect or be infected by and the number of frames since the failed infection
  Map<Particle, Integer> recent_infect_fails;

  Particle(PVector pp, Community com) {
    community = com;
    pos = pp.get(); init_pos = pp.get();
    vel = PVector.random2D(); vel.normalize();
    vel.mult(10);
    acc = new PVector(0, 0);
    state = 0;
    adventurer = false;
    infected_time = -1;
    recent_infect_fails = new HashMap<Particle, Integer>();
  }

  void infect(Particle p) {
    p.state = 1;
    p.num_infected = 0;
    num_infected++;
  }

  void randomMove() {
    if(random(0, 1) > 0.02) return;
    
    Community dest = community;
    if(num_communities > 1 && random(0, 1) < 0.008) {
      while (dest == community) {
        dest = communities[(int) random(num_communities)]; 
      }
      adventurer = true;
    }
    
    this.traveller = true;
    this.to_community = dest;
    this.target_location = new PVector(random(dest.community_left, dest.community_right), random(dest.community_bottom,dest.community_top));
  }

  // select a center to party at
  void selectCenter() {
      target_center = community.centers[(int) random(community.num_centers)];
      target_location = target_center;
  }

  void makeShopper() {
    shopper = true;
    selectCenter();
  }

  void makeGatherer() {
    gatherer = true;
    gatherer_time = 0;
  }

  void applyForce(PVector f) { 
    acc.add(f);
  } 
  // people don't walk in one direction forever
  void slowDown() {
    PVector f = PVector.mult(vel, 0.01);
    vel.sub(f);

    // make sure people don't gain too much velocity
    vel.limit(2);
  }
  
  void goTo(float speed) {
    PVector diff = PVector.sub(target_location, pos);
    diff.normalize(); diff.mult(speed);
    vel.add(diff);
  }

  // people go to parties
  void shop() {
    boolean forth = target_location == target_center;
    // people gather at a central location like a party
    if (forth && pos.dist(target_center) < 5) target_location = init_pos;
    else if (!forth && pos.dist(init_pos) < 5) {
      shopper = false;
    }
    goTo(3);
  }

  void bounds() {
    // bounces off of the sides
    if (pos.x - particle_radius < community.community_left || community.community_right < pos.x + particle_radius) {
      vel.x *= -1;
    }
    if (pos.y - particle_radius < community.community_bottom || community.community_top < pos.y + particle_radius) {
      vel.y *= -1;
    }

    // do not glitch out of bounds, sometimes the physics gets wacky
    pos.x = max(community.community_left + particle_radius, pos.x); pos.x = min(community.community_right - particle_radius, pos.x);
    pos.y = max(community.community_bottom + particle_radius, pos.y); pos.y = min(community.community_top - particle_radius, pos.y);

  }

  // things related to changes to the position, velocity or acceleration of the particles
  void updatePosition() {
    // people move around randomly
    if(!community.quarantine && !gatherer && !shopper && !adventurer) {
      randomMove();
    }
    else if (!community.quarantine_distancing && traveller && community.quarantine && !adventurer) {
      vel.mult(0);
    }
    
    slowDown();
    if (shopper) {
      shop();
    }
    if (traveller) {
      goTo(0.05);
    }
    vel.add(acc);
    pos.add(vel);
    acc.mult(0);
    if (!adventurer) bounds();
  }

  // this is the function being called every frame
  void nextFrame() {
    if (community.shopping && !gatherer && !adventurer && random(0, 1) < 0.001) {
      if (!community.quarantine) {
        makeShopper();
      }
      else if (random(0, 1) < 0.2) {
        makeShopper();
      }
    }

    if (gatherer) {
      gatherer_time++;
      if (gatherer_time > frame_rate * 3.5) {
        gatherer = false;
      }
    }

    if (state != 3) {
      updatePosition();
    }
    if (state == 1) {
      if(show_infection_radius) {
        stroke(199,21,133);
        fill(0, 0, 0, 0);
        ellipse(pos.x, pos.y, infection_radius, infection_radius);
      }
      stroke(1);
      fill(255, 0, 0);
      // there's a chance the the person will self isolate
      if (random(0, 1) < 0.01) {
        self_isolation = true;
      }
      // handling recovery {
      if (infected_time == -1) {
        infected_time = 0;
      }
      else infected_time ++;
      if (recover_time <= infected_time) {
        if(random(1) < chance_of_death) {
          state = 3;
          community.dead_particles.add(this);
        }
        else if (random(1) < chance_of_losing_immunity) {
			  	state = 0;
		  	}
        else {
          state = 2;
        }

        infected_time = -1;
        community.num_recovered++;
        community.overall_infected += num_infected;
      }
      // } handling recovery
    }
    else if (state == 2) {
      fill(0, 0, 255);
    }
    else if (state == 3) {
      fill(120);
    }
    else {
      fill(255);
    }
    ellipse(pos.x, pos.y, 7, 7);
  }
}

boolean clicked = false;
void mouseClicked() {
  clicked = true;
}

class toggleButton{
  boolean isSidebar;
  boolean value;

  float left_bound, bottom_bound;
  float button_top, button_left;

  String on;
  String thingToggled;

  int radius;
  int transparency = 0;

  public toggleButton(String thingToggled, boolean value, float left, float bottom, boolean sidebar, int r){
    this.value = value;
    left_bound = left;
    bottom_bound = bottom;

    isSidebar = sidebar;

    button_left = left_bound + button_offset_x;
    button_top = bottom_bound + button_offset_y;

    radius = r;
    this.thingToggled = thingToggled;
  }
  
  void update(){
    stroke(0,0,255);
    strokeWeight(3);
    textSize(18);
    textAlign(CENTER);

    float top, left;
    if(isSidebar){
      top = button_top;
      left = button_left;
    }else{
      top = button_top + ypt - yzt;
      left = button_left + xpt - xzt;
    }
    boolean mouseInYRange = mouseY >= top && mouseY <= top + button_size_y;
    boolean mouseInXRange = mouseX >= left && mouseX <= left + button_size_x;

    if(mouseInYRange && mouseInXRange){
      transparency = 100;
    }else{transparency = 0;}

    if(value){
      fill(0,128,255,255-transparency);
    }else{
      fill(102,178,255,255-transparency);
    }
    rect(button_left,button_top,button_size_x,button_size_y,radius);
    
    if(mouseInYRange && mouseInXRange && clicked){
      value = !value;
      clicked = false;
    }

    fill(0,0,0);
    if(value){
      on = "On";
    }else{
      on = "Off";
    }
    text(thingToggled + ": " + on, button_left + button_size_x / 2, button_top + button_size_y / 1.4);
  }
}

void graphing(float[] graphingArray, color graphColor) {
  int num = 50;
  int everyFrame = 100;
  int goingUp = 0;
  int tempX = 800;

    for (int i = 0; i < graphingArray.length; i++) {
      int currX = ((width/2)*i/graphingArray.length+(width/2)/num)*(2/3)+800;
      int currY = (int)(height/2-graphingArray[i]+495);
    
      fill(150);

      if(i != 0){
        strokeWeight(1);
        if(i != 0 && graphingArray[i] - graphingArray[i-1] > 5){
          if(goingUp != 1) {
            curveTightness(2);
            goingUp = 1;
          }
          else {
            curveTightness(1);
         }
       }
        else if(i != 0 && graphingArray[i] - graphingArray[i-1] > -5){
          goingUp = 0;
          curveTightness(0);
        }
        else{
          if(goingUp != 2){
            curveTightness(2);
            goingUp = 2;
          }
          else{
            curveTightness(1);
          }
        }
        if (!reg || i > 40 - timesDone){
          stroke(graphColor, 100);
          fill(graphColor, 100);
          quad(tempX, (height/2)-graphingArray[i-1]+495, currX, currY, currX,  1245, tempX, 1245);
          strokeWeight(4);
          stroke(graphColor);
          curve(currX, (height/2)-graphingArray[i-1]+495, tempX, (height/2)-graphingArray[i-1]+495, currX, currY, currX, (height/2)-graphingArray[i-1]+495);
          strokeWeight(1);
          stroke(0);
          fill(0);
          ellipse(currX, currY, 5, 5);
          if (i == 39 && isInfection){
              lastX = currX;
              lastY = currY;
            }
          }
        tempX = currX;
        }
  }
}

void projectionLinear(int x_offset, int y_offset) {
	float[] x = new float[lastPoints.length];
	for (int i = 0; i < lastPoints.length; i ++) {
		x[i] = 20 * i;
	}
	float[] res = linearRegression(x, lastPoints);
	float m = res[0], b = res[1];
	stroke(0, 100, 100);
	strokeWeight(1);
	float xloc = 200;
	float yloc = m * xloc;
	line(x_offset, y_offset, xloc + x_offset, -yloc + y_offset);
}


void projectionExponential(int x_offset, int y_offset) {
	float[] x = new float[lastPoints.length];
	for (int i = 0; i < lastPoints.length; i ++) {
		x[i] = 20 * i;
	}
	float[] res = expRegression(x, lastPoints);
	float A = res[0], r = res[1];
	strokeWeight(1);
	float xstart = 0;
	float ydiff = A * pow(r, xstart);
	for (float xloc = xstart; xloc < 200; xloc ++) {
		stroke(0, 255, 255);
		float yloc = A * pow(r, xloc);
		ellipse(x_offset + xloc, -yloc + y_offset + ydiff, 1, 1);
	}
}

void projectionQuad(int x_offset, int y_offset) {
	float[] x = new float[lastPoints.length];
	for (int i = 0; i < lastPoints.length; i ++) {
		x[i] = 20 * i;
	}
	float[] res = quadRegression(x, lastPoints);
	float a = res[0], b = res[1], c = res[2];
	strokeWeight(1);
	stroke(255, 100, 0);
	float xstart = 0;
	for (float xloc = xstart; xloc < 200; xloc ++) {
		float yloc = a * xloc * xloc + b * xloc;
		ellipse(x_offset + xloc, -yloc + y_offset, 1, 1);
	}
}

class Slider {
  float value;
  float leftBound, rightBound, topBound;
  float currentPos;
  float maximum;
  String thing_controlled;

  public Slider(String type, float value, float left, float right, float top, float max){
    this.value = value;
    leftBound = left + 20;
    rightBound = right - 20;
    topBound = top;
    currentPos = (value/(max/(rightBound-leftBound)))+leftBound;
    maximum = max;
    thing_controlled = type;
  }

  public float get() {
    return value;
  }

  void update(){
    strokeWeight(3);
    textFont(createFont("Times New Roman",20));
    textAlign(LEFT);

    float sliderTop = topBound + slider_offset_y - slider_length_y/2;
    float sliderBottom = topBound + slider_offset_y + slider_length_y/2;

    // baselines
    stroke(0,0,0);
    line(leftBound,topBound+slider_offset_y,rightBound,topBound+slider_offset_y);
    line(leftBound,sliderTop,leftBound,sliderBottom);
    line(rightBound,sliderTop,rightBound,sliderBottom);

    // dynamic line
    stroke(0,0,255);

    // checks whether mouse in range of slider
    boolean mouseInYRange = mouseY >= sliderTop + ypt - yzt && mouseY <= sliderBottom + ypt - yzt;
    boolean mouseInXRange = mouseX >= leftBound - 25 + xpt - xzt && mouseX <= rightBound + 25 + xpt - xzt;

    if(mouseInYRange && mouseInXRange && mousePressed && !panningToggle.value){
      if(mouseX > rightBound + xpt - xzt){
        mouseX = (int) (rightBound + xpt - xzt);
      }
      if(mouseX < leftBound + xpt - xzt){
        mouseX = (int) (leftBound + xpt - xzt);
      }
      currentPos = mouseX - (xpt - xzt);
    }

    fill(0,128,255);
    stroke(0,0,255);
    ellipse(currentPos,sliderTop + slider_length_y/2,20,20);

    value = (currentPos-leftBound) * maximum/(rightBound - leftBound);

    fill(0,0,0);
    text(thing_controlled+": "+value,leftBound + 20,sliderTop - 10);
  }
}

class VerticalSlider extends Slider{
  public VerticalSlider(String type, float value, int top, int bottom, int right, float max){
    super(type,value,top,bottom,right,max);
  }

  void update(){
    strokeWeight(3);
    textFont(createFont("Times New Roman",20));
    textAlign(CENTER);
    
    float sliderTop = topBound + slider_length_y/2;
    float sliderBottom = topBound - slider_length_y/2;

    // baselines
    stroke(0,0,0);
    line(topBound,leftBound,topBound,rightBound);

    // dynamic line
    line(sliderBottom,leftBound,sliderTop,leftBound);
    line(sliderBottom,rightBound,sliderTop,rightBound);

    // checks whether mouse in range of slider
    boolean mouseInXRange = mouseX >= sliderBottom + xpt - xzt && mouseX <= sliderTop + xpt - xzt;
    boolean mouseInYRange = mouseY >= leftBound - 25 + ypt - yzt && mouseY <= rightBound + 25 + ypt - yzt;

    if(mouseInYRange && mouseInXRange && mousePressed && !panningToggle.value){
      if(mouseY > rightBound + ypt - yzt){
         mouseY = (int) (rightBound + ypt - yzt);
      }
       if(mouseY < leftBound + ypt - yzt){
         mouseY = (int) (leftBound + ypt - yzt);
       }
       currentPos = mouseY - (ypt - yzt);
    }

    fill(0,128,255);
    stroke(0,0,255);
    ellipse(sliderBottom + slider_length_y/2,currentPos,20,20);
    
    value = (currentPos-leftBound) * maximum/(rightBound - leftBound);
    fill(0,0,0);
    text(thing_controlled+": "+value, topBound, leftBound - 10);
  }
}