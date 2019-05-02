
import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

public class SpaceGame extends Applet implements Runnable, MouseMotionListener, MouseListener, KeyListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// locations of the mouse on the screen
	int mousex = 0;
	
	int mousey = 0;

	public class object {// generic object for everything in this game
		// x and y are the positions of the object, time is when the object was
		// previosly "processed" by the game, type is what the object is
		double x;
		double y;

		long time;
		String type;

		public object(String type, double x, double y, long time) { // constructor for the generic object
			this.x = x;
			this.y = y;
			this.time = time;
			this.type = type;
		}
	}

	public class HP extends object {// HP of the player ship
		int health;// the total health of the ship during that time

		public HP(int health) {
			super("h", 800, 0, System.nanoTime());// calls superclass constructor
			this.health = health;
		}
	}

	public class bullet extends object {// the bullet fired from the player ship
		// dirx and y is used to calculate the slope of the bullet (how it moves)
		int dirx;
		int diry;
		double slope;

		public bullet(double x, double y, long time, int dirx, int diry, double slope) {
			super("b", x, y, time);// calls superclass constructor
			this.dirx = dirx;
			this.diry = diry;
			this.slope = slope;

		}

	}

	public class ship extends object {// the ship of the player

		public ship(int x, int y) {
			super("s", x, y, System.nanoTime());// calls superclass constructor

		}

	}

	public class asteroid extends object {// the asteroids that go downwards from the top and block bullets
		int health;// current health of the asteroid

		public asteroid(int x, int y, int health) {
			super("a", x, y, System.nanoTime());// calls superclass constructor
			this.health = health;
		}
	}

	public class suicider extends object {// the ships that try to collide with the player
		int health;// current health of the enemy

		public suicider(double x, double y, int health) {
			super("e1", x, y, System.nanoTime());// calls superclass constructor
			this.health = health;
		}

	}

	public class shoot extends object {// the ship that try to shoot the player
		int health;// the health of te ship
		double mode;// what firing "mode" the ship is in
		// where the ship is aiming (x and y)
		int aimx;
		int aimy;

		public shoot(double x, double y, int health, double mode, int aimx, int aimy) {
			super("e2", x, y, System.nanoTime());// calls superclass constructor
			this.health = health;
			this.mode = mode;
			this.aimx = aimx;
			this.aimy = aimy;
		}
	}

	public class points extends object {// the points that the player has accumalated
		long score;// current score of the player.

		public points(long nextScore) {
			super("p", 0, 0, System.nanoTime());// calls superclass constructor
			score = nextScore;
		}
	}

	public class laser extends object {// the laser that signals the coming of a rocket

		public laser(int x, int y) {
			super("e3", x, y, System.nanoTime());// calls superclass constructor

		}
	}

	public class missile extends object {// the actual missile after the laser times out
		public missile(int x, int y) {
			super("M", x, y, System.nanoTime());// calls superclass constructor
		}
	}

	Image OS;// the backScreen
	int pendingDamage = 0;// damage dealt to the player, processed in the next process cycle
	int y = 500;// where the player is currently at
	int accumulate = 0;// points scored by the player, processed in the next process cycle
	boolean heal = false;// signals a full heal (from the 5000 point milestones)
	int nextBar = 5000;// where the next 5000 point milestone is at
	Graphics OG;// the graphics processor for the

	Thread myThread;// the game thread
	String esc = "";// what the most recent keypress is
	int diff = 1;// the difficulty (level) of the current game
	Queue<object> queue = new LinkedList<object>();// the queue of the items waiting to be processed and shown
	MediaTracker track = new MediaTracker(this);// tracks loading of imgaes
	HashMap<String, Integer> stuff = new HashMap<String, Integer>();// checks to see if numerous actions (key pressed,
																	// mouse presseds, end games) are true or false
	boolean won = false;// checks if won
	Image craft, background, asteroid, suicide, shoot, missileShot, win, lose;// various images used in game
	boolean start = false;// signals the player pressing start
	Font newFont = new Font("lol", Font.ITALIC, 20);// the font in the game
	AudioClip shot, STrack, explosion; // varios sounds used in the game

	public void init() {
		// STrack is the soundtrack played throughout
		STrack = getAudioClip(getCodeBase(), "SoundTrack.wav");
		STrack.play();
		// shot is the soundtrack played when you fire
		shot = getAudioClip(getCodeBase(), "shot.wav");
		// explosion is th soundtrack played when an enemy dies
		explosion = getAudioClip(getCodeBase(), "shoot.wav");
		Font title = new Font("title", Font.ITALIC, 100);// font for the title of game

		System.out.println(getCodeBase().toString());// gives address to put the media
		// loads all images
		shoot = getImage(getCodeBase(), "shooter.png");
		missileShot = getImage(getCodeBase(), "laser.png");
		win = getImage(getCodeBase(), "win.jpg");
		lose = getImage(getCodeBase(), "dead.jpg");
		asteroid = getImage(getCodeBase(), "asteroid.png");
		craft = getImage(getCodeBase(), "ship.png");
		background = getImage(getCodeBase(), "Background.jpg");
		suicide = getImage(getCodeBase(), "suicide.png");
		// lets mediaTracker track the loading
		track.addImage(missileShot, 0);
		track.addImage(craft, 0);
		track.addImage(asteroid, 0);
		track.addImage(win, 0);
		track.addImage(background, 0);
		track.addImage(lose, 0);
		track.addImage(suicide, 0);
		track.addImage(shoot, 0);
		while (track.checkAll(true) != true) {
		}
		if (track.isErrorAny()) {
			System.out.println("oof");
		}
		// initializes objects to track mouse and keyboard
		addMouseMotionListener(this);
		addMouseListener(this);
		addKeyListener(this);
		// begins backscreen and graphics processor
		OS = createImage(2000, 4000);
		OG = OS.getGraphics();
		// draws the front page
		OG.setColor(Color.lightGray);
		OG.drawImage(background, 0, 0, this);
		OG.fillOval(-20, y - 20, 80, 80);
		OG.drawImage(craft, 0, y - 6, this);
		OG.setFont(title);
		OG.drawString("Lost In Space", 600, 100);
		OG.setFont(newFont);
		
		OG.setColor(Color.green);
		OG.drawString("This is your ship", 90, y + 30);
		OG.drawString("W to go up, S to go down.", 90, y + 50);
		OG.drawString("Left click to fire at your cursor location (E to autofire)", 90, y + 70);
		OG.drawString("Do not press X. Just don't", 90, y + 90);
		OG.drawRect(1600, 700, 200, 100);
		OG.drawString("Go!", 1680, 760);
		OG.drawImage(suicide, 700, 200, this);
		OG.drawImage(shoot, 700, 400, this);
		OG.setColor(Color.blue);
		OG.drawLine(700, 600, 1000, 600);
		OG.drawLine(700, 630, 1000, 630);
		OG.drawImage(asteroid, 700, 700, this);
		OG.setColor(Color.green);
		OG.drawString(
				"These little things try to hit you straight on. More meat shields to protect the gunships than enemies.",
				600, 300);
		OG.drawString(
				"These gunships aim at you with a green laser. When the green laser ain't green anymore, stay out of its way.",
				600, 500);
		OG.drawString("These blue paths signal the coming of a DANGEROUS missile. YOU HAVE BEEN WARNED.", 600, 670);
		OG.drawString("This is an asteroid. Why shoot asteroids?", 600, 800);
		OG.drawString("Every 5000 points, you restore to full health,", 5, 50);
		OG.drawString("but enemies get MUCH harder.", 5, 70);
		OG.drawString("Reach 15000 to win!", 5, 90);
		OG.drawString("Your health is this green line at the bottom", 10, 850);
		OG.setColor(Color.cyan);
		OG.fillRect(0, 0, 100, 30);
		OG.setColor(Color.black);
		OG.drawString("Points: 0", (int) 10, (int) 20);
		OG.setColor(Color.red);
		OG.setColor(Color.green);
		OG.fillRect(0, 900, 2000, 10);
		// adds beginning objects (asteroids, health, points, and whatnot
		queue.add(new asteroid(500, 100, 100));
		queue.add(new asteroid(400, 50, 100));
		queue.add(new asteroid(300, 10, 100));
		queue.add(new points(0));
		queue.add(new HP(100));
		// begins tracking of varios keyboard and mouse clicks
		stuff.put("x", 0);
		stuff.put("b", 0);
		stuff.put("w", 0);
		stuff.put("s", 0);
		stuff.put("e", 0);
		// shot.play();
		stuff.put("mouse", 0);
		// paints
		repaint();
	}

	public void paint(Graphics g) {// defines paint
		g.drawImage(OS, 0, 0, this);
	}

	public void start() {// create thread starting method
		if (myThread == null) {
			myThread = new Thread(this);
			myThread.start();
		}
	}

	public void stop() {// create thread ending method
		if (myThread != null) {
			myThread = null;
		}
	}

	public void run() {
		System.out.println("start");
		while (!start) {// shows start screen until they click go
			// constantly refreshes start objects so keypresses won't change their position
			queue.clear();
			queue.add(new asteroid(500, 100, 100));
			queue.add(new asteroid(400, 50, 100));
			queue.add(new asteroid(300, 10, 100));
			y = 500;
			queue.add(new ship(0, y - 6));
			queue.add(new laser(500, 500));
			queue.add(new points(0));
			// queue.add(new shoot(2000, 500, 100, 0, 0, 0));
			queue.add(new HP(100));
			stuff.put("x", 0);
			stuff.put("b", 0);
			stuff.put("w", 0);
			stuff.put("s", 0);
			stuff.put("e", 0);
			stuff.put("mouse", 0);
			stuff.put("hack", 1);
		}

		long start = System.nanoTime();// tracks when the game actually started
		while (stuff.get("x") == 0) {// runs game while x key is not pressed or victory/death acheived
			// System.out.println("run");
			long currentTime = System.nanoTime();// takes the time each loop
			long end = currentTime - start;// the time that has passed within the game
			long end2 = (int) ((double) ((end / (250000000 / 2))));// converts it to the refresh rate of the system
			// System.out.println(end2);
			if (end2 >= stuff.get("hack")) {// checks to see if the next bullet should be fired (bullet fires once every
							// refresh time
				start = currentTime;// restarts the start time to check for next bullet

				if (stuff.get("mouse") == 1) {// only fires if the person is firing
					// adds spray (inaccuracy) of the bullet
					int spray = (int) (Math.random() * 50) - 25;
					int spray2 = (int) (Math.random() * 50) - 25;
					double slope = ((double) mousey + spray - (double) y - 6) / ((double) mousex - spray2);// implements
																											// trajectory
																											// of the
																											// bullet
					queue.add(new bullet(10, y + 20, System.nanoTime(), mousex, mousey, slope));// adds the new bullet
																								// object into the
																								// process queue
					shot.stop();// stops the last bullet sound (stops eventual white noise)
					shot.play();// starts the bullet sound
				}
				// System.out.println("done");
			}
			try {
				Thread.sleep(33);// delay in the program
			} catch (Exception a) {
			}
			// System.out.println();

			refresh();// computes cpu
			// computes the sending of enemies
			sendS();
			sendL();
			sendM();
			repaint();// repaints
		}
		OG.setFont(newFont);// initializes the font for the run function

		if (won) {// activiates win screen when won
			OG.setColor(Color.green);
			OG.drawImage(win, 0, 0, this);
			OG.drawString("WELCOME HOME", 1400, 700);
		} else {// activates loss screen when lost
			OG.setColor(Color.red);
			OG.drawImage(lose, 0, 0, this);
			OG.drawString("THE SPACE HAS CLAIMED YOU", 1400, 700);
		}

	}

	public void mouseDragged(MouseEvent arg0) {// activates bullet firing when the mouse if held down and moved
		// TODO Auto-generated method stub
		mousex = arg0.getX();
		mousey = arg0.getY();
		stuff.put("mouse", 1);// mouse is clicked

	}

	public void mouseMoved(MouseEvent arg0) {// activates bullet firing when the mouse is moving, and auto fire is
												// activated
		// TODO Auto-generated method stub
		// System.out.println("move");
		if (stuff.get("e") == 1) { // checks if E (autofire) was clicked
			// System.out.println("active");
			mousex = arg0.getX();
			mousey = arg0.getY();
			stuff.put("mouse", 1);// mouse is clicked
			return;
		}
		stuff.put("mouse", 0);// mouse is unclicked

	}

	public void mousePressed(MouseEvent e) {// activates bullet firing once the mouse is clicked
		if (e.getX() < 1800 && e.getX() > 1600 && e.getY() < 800 && e.getY() > 700) {// starts the game if they click go
																						// (in the specified boundaries)
			start = true;
		}
		mousex = e.getX();
		mousey = e.getY();
		stuff.put("mouse", 1);// mouse is clicked
	}

	public void mouseReleased(MouseEvent e) {// checks when the mouse in unclicked
		stuff.put("mouse", 0);// mouse is unclicked
	}

	public void keyPressed(KeyEvent e) {// activates when a key is pressed
		// TODO Auto-generated method stub
		esc = "" + (char) e.getKeyChar();// get the key pressed
		// System.out.println(esc);
		if(esc.equals("M")) {
			stuff.put("hack",0);
			return;
		}
		if (esc.equals("s") && stuff.get("s") == 0 && y < 900) {// activates when player presses "s"
			System.out.println("s");
			// System.out.println("++++++++++++++++++++++++++++++++++++");
			y += 10;// ship goes down
		}
		if (esc.equals("w") && stuff.get("w") == 0 && y >= 0) {// activate when player presses "w"
			y -= 10;// ship goes up
			// stuff.put("x",1);

			// System.out.println(y);
		}
		if (esc.equals("e")) {// activate when player presses "e"
			// System.out.println("e");
			if (stuff.get(esc) == 0) {
				stuff.put(esc, 1);// activates auto-fire
			} else {
				stuff.put(esc, 0);// deactivates auto-fire
			}
			// System.out.println(stuff.get(esc));
			return;
		}
		stuff.put(esc, 1);// signals that whatever key was clicked
	}

	public void keyReleased(KeyEvent e) {// checks when the key was released
		// TODO Auto-generated method stub
		esc = "" + (char) e.getKeyChar();
		if (esc.equals("e")) {// does not work for auto-fire, which is managed in keypressed instead
			return;
		}
		stuff.put(esc, 0);// signals that whatever key was unclicked
	}

	public void refresh() {// cpu that refreshes and computes all ai.
		// System.out.println("lol");
		// System.out.println(time);
		// System.out.println("fresh");
		// initializes the graphics processor
		OG = OS.getGraphics();
		int max = queue.size();// checks how many items are in queue
		if (stuff.get("s") == 1 && y < 900) {// moves ship down
			// System.out.println(y);
			y += 10;
		}
		if (stuff.get("w") == 1 && y > 0) {// moves ship up
			y -= 10;
		}
		OG.setColor(Color.BLACK);
		OG.drawImage(background, 0, 0, this);// draws backscreen

		OG.setColor(Color.RED);

		for (int count = 0; count < max; count++) {// activates for every object
			object next = queue.poll();// gets the next object to be processed
			int max2;
			boolean sense;// senses if an object died
			int newhealth;// sets newhealth for objects
			double nextx, nexty, slope;// sets coordinates for objects
			switch (next.type) {// gives cases for each type of object
			case "b":// if it is a bullet:
				bullet then = (bullet) next;// wraps object into bullet
				// refreshes bullet positions
				nextx = (int) (next.x + (30));
				nexty = (next.y + (then.slope * 30) * (System.nanoTime() - then.time) / 30000000);
				// System.out.println(nexty);
				// draws the bullet
				OG.setColor(Color.red);
				OG.fillOval((int) nextx, (int) nexty, 5, 5);
				// System.out.println(next);
				if (next.x <= 2000) {// adds updated bullet back into the queue (for the next run) if the bullet did
										// not reach max range (2000)
					queue.add(new bullet(nextx, nexty, System.nanoTime(), then.dirx, then.diry, then.slope));
				}
				break;
			case "a":// if it is an asteroid
				asteroid newast = (asteroid) next;// wraps object into asteroid
				// System.out.println("asteroid"+newast.x);
				// refreshes asteroid position
				int nexty2 = (int) (next.y + (System.nanoTime() - next.time) / 7000000);
				// draws the asteroid
				OG.setColor(Color.red);
				OG.drawImage(asteroid, (int) next.x, nexty2, this);
				if (newast.health != 100) {// draws asteroid health if it took damage
					OG.setColor(Color.red);
					OG.fillRect((int) next.x, nexty2 + 100, 100, 10);
					OG.setColor(Color.green);
					OG.fillRect((int) next.x, nexty2 + 100, newast.health, 10);
				}
				// checks if anything hit the asteroid by rerunning the queue
				max2 = queue.size();
				sense = false;// has not died (yet)
				newhealth = newast.health;// sets new health to the asteroid's health
				for (int count2 = 0; count2 < max2; count2++) {// runs every object in the queue
					object comp = queue.poll();// gets next object
					bullet maybe;

					if (comp.type.equals("b")) {// activates when the object is a bullet
						maybe = (bullet) comp;// wraps bullet
						if (collision(asteroid.getWidth(this), asteroid.getHeight(this), (int) next.x, (int) next.y, 5,
								5, (int) comp.x, (int) comp.y)) {// checks if the bullet is in contact with the asteroid
							newhealth -= 20;// decreases asteroid health
									// by the outside loop anymore
							if (newhealth <= 0) {// checks if the asteroid (died)
								accumulate += 10;// adds points (pending for the next point cycle)
								sense = true;// shows that the asteroid died in the outside loop
								break;// leaves loop
							}
							
						} queue.add(maybe);// adds the bullet back into the queue if it did not hit asteroid
						

					} else {
						queue.add(comp);// adds object back into the queue if it is not a bullet
					}
				}
				if (!sense) {// re-adds the asteroid back into the queue with updated positions and health if
								// it is not dead
					if (nexty2 < 900) {// gets the asteroid back into the top of the map if it reached the bottom
						queue.add(new asteroid((int) next.x, nexty2, newhealth));
					} else {// puts the asteroid a bit lower with the new health
						queue.add(new asteroid((int) (Math.random() * 500 + 500), -150, 100));
					}

				} else {// if it died, add a new asteroid back to the top (unending asteroids)
					queue.add(new asteroid((int) (Math.random() * 500 + 500), -150, 100));
				}
				break;// leaves switch
			case "s":// if it is the ship

				OG.drawImage(craft, 0, y - 6, this);// draws the ship
				queue.add(new ship(0, y - 6));// adds the ship back into the process queue

				break;// leaves switch
			case "e1":
				// System.out.println("activate");
				suicider newsui = (suicider) next;// wraps a new suicider ship
				newhealth = newsui.health;// sets new health to the suicider current healt
				sense = false;// suicider has not died (yet)
				// System.out.println(newhealth);

				// OG.fillRect((int)next.x, (int)next.y, 500, 500);
				OG.drawImage(suicide, (int) next.x, (int) next.y, this);// draws the suicider
				if (newsui.health != 100) {// draws health bar if the health is not full
					//health bar drawn by overlaying a red rectangle with a green one of specific lengths
					OG.setColor(Color.red);
					OG.fillRect((int) next.x - 7, (int) next.y + 30, 50, 10);
					OG.setColor(Color.green);
					OG.fillRect((int) next.x - 7, (int) next.y + 30, newsui.health / 2, 10);
				}
				max2 = queue.size();// checks to see if any bullets hit the suIcider
				for (int count2 = 0; count2 < max2; count2++) {// loops once for each object in the queue
					object comp = queue.poll();//gets next object
					bullet maybe;

					if (comp.type.equals("b")) {// activates if the next object is a bullet
						maybe = (bullet) comp;// wraps bullet
						if (collision(suicide.getWidth(this), suicide.getHeight(this), (int) next.x, (int) next.y, 5, 5,
								(int) comp.x, (int) comp.y)) {// bullet hit the ship
							newhealth -= (int) ((double) 100 / (diff + 1)) + 0.5;//decreases health
							if (newhealth <= 0) {// checks to see if the suicide ship died
								accumulate += 50;//adds to points (pending till next point cycle)
								explosion.play();// plays death sound
								sense = true;//tells outer loop that ship has died
								break;//leaves this loop
							}

						} queue.add(maybe);//if bullet did not hit ship, no need to remove it
						

					} else {
						queue.add(comp);//if object is no a bullet, no need to remove it
					}
				}
				if (!sense) {//if suicide ship lived
					//moves the ship to its new position
					int distance = (int) next.y - y;
					if (Math.abs(distance) < 5) {//stops moving up or down when in line with player ship
						slope = 0;
					} else if (Math.abs(distance) < 200) {//moves slower down or up when close to player ship
						if (next.y > y) {
							slope = -10;

						} else {
							slope = 10;
						}

					} else {
						if (next.y > y) {//moves fast down or up when far from player ship
							slope = -50;

						} else {
							slope = 50;
						}
					}

					// System.out.println(slope);
					//updates the position of the ship
					nextx = (int) (next.x - (5));
					nexty = (next.y + (slope) * (System.nanoTime() - next.time) / 300000000);
					if (nextx == 0) {// removes the ship if it reaches the left side of the screen
						if (Math.abs(y - nexty) < 10) {//deals damage if it lands close enough to the ship
							pendingDamage += 10;
						}
					} else {//adds the updated suicide ship back into the process queue
						queue.add(new suicider(nextx, nexty, newhealth));
					}

				}
				break;//leaves switch
			case "p":// if the net object is the points
				OG.setColor(Color.cyan);
				points scoreBoard = (points) next;//wraps a scoreboard
				long nextScore = scoreBoard.score + (System.nanoTime() - next.time) / 30000000;// processes point gain over tim
				nextScore += accumulate;// adds pending score to the scoreboard
				accumulate = 0;// resets pending score
				//draws the scoreboard
				OG.fillRect(0, 0, 100, 30);
				OG.setColor(Color.black);
				OG.drawString("Points: " + nextScore, (int) next.x + 20, (int) next.y + 20);
				queue.add(new points(nextScore));// adds the point object back into the process queue
				if (nextScore > nextBar) {//processes the 5000 point checkpoints
					nextBar += 5000;//sets the next milestone
					heal = true;//heals player back to full
					diff++;//raises the difficulty

				}
				if (nextScore >= 15000) {//checks to see if the player won
					stuff.put("x", 1);//stop signal raised for the game
					won = true;//signals a win
				}
				break;//leaves switch
			case "e2"://if it is a shooting enemy
				// System.out.println("done");
				shoot nextGun = (shoot) next;//wraps next shoot enemy
				int ShootHP = nextGun.health;//sets the shoot's HP
				
				sense = false;//ship has not died (yet)
				OG.drawImage(shoot, (int) next.x, (int) next.y, this);//draws the ship
				// System.out.println(next.x+" "+next.y);
				if (nextGun.health != 100) {// draws a health bar when the ship does not have full health
					OG.setColor(Color.red);
					OG.fillRect((int) next.x, (int) next.y + 50, 100, 10);
					OG.setColor(Color.green);
					OG.fillRect((int) next.x, (int) next.y + 50, nextGun.health, 10);
				}
				max2 = queue.size();//the number of objects in the queue (see if any bullets hit the ship)
				for (int count2 = 0; count2 < max2; count2++) {// operates on every object in the queue
					object comp = queue.poll();// gets the next object
					bullet maybe;
					
					if (comp.type.equals("b")) {//activates if the next object is a bullet
						maybe = (bullet) comp;// wraps the next bullet
						if (collision(shoot.getWidth(this), shoot.getHeight(this), (int) next.x, (int) next.y, 5, 5,
								(int) comp.x, (int) comp.y)) {//checks if the bullet hit the ship
							ShootHP -= (int) (20);//decreases ship health
							if (ShootHP <= 0) {//checks to see if the ship is dead
								explosion.play();//plays death sound
								accumulate += 500;//adds points to pending bar
								sense = true;//tells outside loop that ship is dead
								break;//leaves loop
							}

						}	queue.add(maybe);//add bullet back into queue
						

					}

					else {
						queue.add(comp);//adds object back into queue
					}
				}
				if (!sense) {// if ship did not die

					if (next.x > 1600) {//moves forward until it reaches 1600
						double x = (next.x - 10 * (System.nanoTime() - next.time) / 30000000);

						queue.add(new shoot(x, next.y, ShootHP, 0, 0, 0));//adds updated position ship back into queue
					} else {//starts shooting when it hits 1600
						int posy = 0;//sets where the ship aims on default
						double mode = 0;//sets its firing mode
						if (nextGun.aimy != 0) {// proceeds with the fire if the ship is already aiming at somebody
							mode = nextGun.mode + (double) ((double) (System.nanoTime() - next.time) / 300000000);// mode is increased according to how much time passed
							int mode2 = (int) mode;
							// System.out.println(mode);
							switch (mode2) {
							case 0:// be green during modes 0 and 1, red during modes 2 and 3
								OG.setColor(Color.green);
								break;
							case 1:
								OG.setColor(Color.green);
								break;
							case 2:
								OG.setColor(Color.red);
								break; 
							case 3:
								OG.setColor(Color.red);
								break;
							default:// sets a default
								// OG.setColor(Color.white);
								posy = 0;
								mode = 0;
							}
							OG.drawLine(0, nextGun.aimy, (int) next.x + 20, (int) next.y + 25);// "shoots" a laser at where the ship is aiming
							if (Math.abs((y + 20) - nextGun.aimy) < 10 && mode >= 2) {//deals damage if the laser is red and collides with the ship
								pendingDamage += 1 * diff;// damage dealt scales based on the difficulty
							}
							posy = nextGun.aimy;//resets where the ship is aiming
							if (mode2 == 4) {// resets the aim of the ship when the laser times out
								posy = 0;
								mode = 0;
							}
						} else {// sets the aim of the ship to where the player is
							// System.out.println("refresh");
							posy = y + 20;
						}
						queue.add(new shoot(next.x, next.y, ShootHP, mode, 0, posy));// adds the ship back into the process queue

					}

				}
				break;//leaves switch
			case "h":// if next object is the player health

				HP nextHP = (HP) next;// wraps object into health
				if (nextHP.health <= 0) {//loses game if health under 0
					stuff.put("x", 1);
				}
				//draws the health on the bottom of the screen by overlapping a red rectangle with a smaller green rectangle
				OG.setColor(Color.red);
				OG.fillRect(0, 900, 2000, 10);
				OG.setColor(Color.green);
				OG.fillRect(0, 900, nextHP.health * 2000 / 100, 10);
				// System.out.println((nextHP.health/100*2000));
				int newHP = nextHP.health - pendingDamage;// processes pending damage of the ship
				if (heal) {// heals back to full if heal is activates
					newHP = 100;
					heal = !heal;//deactivates heal
				}
				queue.add(new HP(newHP));// adds hp back into process queue
				pendingDamage = 0;
				break;
			case "e3":// if the next object is the laser

				laser nextLaze = (laser) next;// wraps object into laser
				long now = System.nanoTime() - next.time;// see what the time consructed of the laser is right now
				now = (long) (now * 0.00000002);// sets the time passed into more manageable numbers

				OG.setColor(Color.blue);
				//draws two lines 30 units away from each other
				OG.drawLine(0, (int) (next.y + 15), 2000, (int) (next.y + 15));
				OG.drawLine(0, (int) (next.y - 15), 2000, (int) (next.y - 15));
				//draws the lines that converge towards the center
				OG.drawLine(0, (int) (next.y + 15 - now), 2000, (int) (next.y + 15 - now));
				OG.drawLine(0, (int) (next.y - 15 + now), 2000, (int) (next.y - 15 + now));
				if (now < 30) {//continues adding updated lasers into the queue
					queue.add(nextLaze);
				} else {// when now times out, fires a missile down the line by adding a missile into the process queue
					queue.add(new missile(2000, (int) next.y));
				}
				break;//leaves switch
			case "M":// if the next object is a missile
				double MISX = next.x;// checks the x position of the laser

				MISX -= ((System.nanoTime() - next.time) / 100000);// updates the missile's x position by the time passed
				// System.out.println(((System.nanoTime()-next.time)/1000000));
				// System.out.println(MISX);
				//draws the missile
				OG.drawImage(missileShot, (int) MISX, (int) next.y, this);
				
				if (MISX > 0) {//adds another updated missile back into the process queue
					queue.add(new missile((int) MISX, (int) next.y));
				} else {//explodes when x hits 0, removing missile from queue and damaging the ship if nearby
					// System.out.println(Math.abs(next.y-y));
					if (Math.abs(next.y - y) < 25) {
						pendingDamage += 50;
					}
				}
				break;//leaves switch

			}

		}

	}

	public boolean collision(int w1, int h1, int x1, int y1, int w2, int h2, int x2, int y2) {// checks to see if two objects collide
		Rectangle r1, r2;
		r1 = new Rectangle(x1, y1, w1, h1);
		r2 = new Rectangle(x2, y2, w2, h2);
		return r1.intersects(r2);
	}

	public void update(Graphics g) {//redefines the paint function
		paint(g);
	}
	//sets the spawntimer of the suicider ships
	long Senemyspawn = System.nanoTime();
	double SspawnTime = 5;

	public void sendS() {
		long newtime = System.nanoTime();
		if ((newtime - Senemyspawn) / 300000000 * SspawnTime > 5) {//whenever 5 ticks pass, sends another ship towards the player
			Senemyspawn = System.nanoTime();
			int x = 2000;
			int y = (int) (Math.random() * 850);//finds a random y position to deploy in
			queue.add(new suicider(x, y, 100));//adds the ship into the process queue
		}
	}
//sets the spawntimer of the shooting ships
	long Lenemyspawn = System.nanoTime();
	double LspawnTime = 5;

	public void sendL() {
		// System.out.println("trying");
		long newtime = System.nanoTime();
		if ((newtime - Lenemyspawn) / 300000000 * LspawnTime > 100) {//whenever 100 ticks pass, send another ship towards the player
			Lenemyspawn = System.nanoTime();
			int x = 2000;
			int y = (int) (Math.random() * 850);//finds a random y position to deploy in

			queue.add(new shoot(x, y, 100, 0, 0, 0));//adds the ship into the process queue
		}
	}
	//sets the spawntimer of the missiles
	long Menemyspawn = System.nanoTime();
	double MspawnTime = 5;

	public void sendM() {
		// System.out.println("trying");
		long newtime = System.nanoTime();
		if ((newtime - Menemyspawn) / 30000000 * MspawnTime > 100 - (15 * diff)) {//whenever ticks pass, send another missile (frequency increases with difficulty)
			Menemyspawn = System.nanoTime();

			int y = (int) (Math.random() * 850);//finds random y position

			queue.add(new laser(500, y));//adds the missile into te process queue
		}
	}

	
	
	//various overridden and unused methods
	@Override
	public void mouseClicked(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent arg0) {
		// TODO Auto-generated method stub

	}

}
