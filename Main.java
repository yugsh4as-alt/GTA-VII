import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║          GTA VII - Vice City Reborn                  ║
 * ║      Developed by: SAHI AHMED YASSIN                 ║
 * ║            © 2024 - All Rights Reserved              ║
 * ╚══════════════════════════════════════════════════════╝
 */
public class Main extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main game = new Main();
            game.setVisible(true);
        });
    }

    public Main() {
        setTitle("GTA VII - Vice City Reborn | By SAHI AHMED YASSIN");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
    }
}

// ══════════════════════════════════════════════════════
class GamePanel extends JPanel implements KeyListener, ActionListener {

    static final int W = 900, H = 650;
    static final int MAP_W = 2400, MAP_H = 2400;

    // Neon Colors
    static final Color NEON_PINK   = new Color(255, 20, 147);
    static final Color NEON_CYAN   = new Color(0, 255, 220);
    static final Color NEON_YELLOW = new Color(255, 220, 0);
    static final Color NEON_ORANGE = new Color(255, 120, 0);
    static final Color ROAD_DARK   = new Color(30, 30, 35);
    static final Color ROAD_LINE   = new Color(255, 220, 0, 120);
    static final Color SIDEWALK    = new Color(60, 55, 70);
    static final Color GRASS_COL   = new Color(20, 80, 40);
    static final Color BLOOD_RED   = new Color(200, 0, 30);

    enum State { SPLASH, PLAYING, PAUSED, GAMEOVER, WIN }
    State state = State.SPLASH;

    Player player;
    List<Building>  buildings  = new ArrayList<>();
    List<CarObj>    cars       = new ArrayList<>();
    List<Coin>      coins      = new ArrayList<>();
    List<Bullet>    bullets    = new ArrayList<>();
    List<Cop>       cops       = new ArrayList<>();
    List<Explosion> explosions = new ArrayList<>();
    List<Particle>  particles  = new ArrayList<>();

    int camX = 0, camY = 0;
    int score = 0, wantedLevel = 0, wantedCooldown = 0;
    int frame = 0;
    long splashStart = 0;
    boolean[] keys = new boolean[256];

    // Double buffer
    BufferedImage buffer;
    Graphics2D    bufG;

    javax.swing.Timer gameTimer;

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        addKeyListener(this);

        buffer = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        bufG   = buffer.createGraphics();
        bufG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        initWorld();
        splashStart = System.currentTimeMillis();
        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();
    }

    // ── WORLD INIT ─────────────────────────────────────
    void initWorld() {
        player = new Player(MAP_W / 2, MAP_H / 2);
        buildings.clear(); cars.clear(); coins.clear();
        cops.clear(); bullets.clear(); explosions.clear(); particles.clear();
        score = 0; wantedLevel = 0; wantedCooldown = 0;

        Random rng = new Random(42);

        int[][] blockPos = {
            {200,200},{500,200},{800,200},{1100,200},{1400,200},{1700,200},{2000,200},
            {200,500},{600,500},{1000,500},{1400,500},{1800,500},
            {200,800},{500,800},{900,800},{1300,800},{1700,800},{2100,800},
            {200,1100},{600,1100},{1000,1100},{1400,1100},{1800,1100},
            {200,1400},{500,1400},{900,1400},{1300,1400},{1700,1400},{2100,1400},
            {200,1700},{600,1700},{1000,1700},{1400,1700},{1800,1700},
            {200,2000},{500,2000},{900,2000},{1300,2000},{1700,2000},{2100,2000},
        };
        Color[] bColors = {
            new Color(60,20,80), new Color(10,50,80), new Color(80,20,20),
            new Color(20,70,50), new Color(70,60,10), new Color(40,10,60)
        };
        Color[] nColors = { NEON_PINK, NEON_CYAN, NEON_YELLOW, NEON_ORANGE };
        for (int[] bp : blockPos) {
            int bw = 80 + rng.nextInt(110);
            int bh = 80 + rng.nextInt(110);
            buildings.add(new Building(bp[0], bp[1], bw, bh,
                bColors[rng.nextInt(bColors.length)],
                nColors[rng.nextInt(nColors.length)]));
        }

        Color[] carColors = { BLOOD_RED, new Color(30,100,255), NEON_YELLOW,
                              new Color(0,200,80), new Color(200,200,200), NEON_PINK };
        int[][] carSpawns = {
            {MAP_W/2-300,MAP_H/2+100},{MAP_W/2+200,MAP_H/2-200},
            {MAP_W/2-500,MAP_H/2-300},{MAP_W/2+400,MAP_H/2+300},
            {MAP_W/2,MAP_H/2+500},    {MAP_W/2-700,MAP_H/2+500},
            {MAP_W/2+600,MAP_H/2-500},{MAP_W/2-200,MAP_H/2-600},
        };
        for (int[] cs : carSpawns)
            cars.add(new CarObj(cs[0], cs[1], carColors[rng.nextInt(carColors.length)], rng.nextInt(4)));

        for (int i = 0; i < 40; i++)
            coins.add(new Coin(200 + rng.nextInt(MAP_W-400), 200 + rng.nextInt(MAP_H-400)));
    }

    // ── GAME LOOP ──────────────────────────────────────
    @Override
    public void actionPerformed(ActionEvent e) {
        frame++;
        if (state == State.PLAYING) update();
        render();
        repaint();
    }

    void update() {
        handleInput();
        player.update(MAP_W, MAP_H);
        for (CarObj c : cars) c.update(MAP_W, MAP_H);
        checkCarProximity();
        updateBullets();
        updateCops();
        updateCoins();
        updateParticles();
        updateExplosions();
        updateCamera();
        wantedCooldown = Math.max(0, wantedCooldown - 1);
        if (wantedCooldown == 0 && wantedLevel > 0 && frame % 300 == 0)
            wantedLevel = Math.max(0, wantedLevel - 1);
        if (score >= 2000) state = State.WIN;
        if (player.hp <= 0) state = State.GAMEOVER;
    }

    void handleInput() {
        double spd = player.inCar ? 4.5 : 2.5;
        if (keys[KeyEvent.VK_SHIFT]) spd *= 1.7;
        boolean up    = keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP];
        boolean down  = keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN];
        boolean left  = keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT];
        boolean right = keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT];
        player.vy = up ? -spd : down  ? spd : 0;
        player.vx = left ? -spd : right ? spd : 0;
        if (up)    player.dir = 0;
        if (right) player.dir = 1;
        if (down)  player.dir = 2;
        if (left)  player.dir = 3;
    }

    void checkCarProximity() {
        for (CarObj c : cars) c.nearPlayer = false;
        if (!player.inCar)
            for (CarObj c : cars)
                if (dist(player.x, player.y, c.x, c.y) < 40) c.nearPlayer = true;
    }

    void updateBullets() {
        Iterator<Bullet> bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next(); b.update();
            if (b.x < 0 || b.x > MAP_W || b.y < 0 || b.y > MAP_H || b.life <= 0) { bi.remove(); continue; }
            boolean removed = false;
            if (b.fromPlayer) {
                for (CarObj c : cars) {
                    if (!removed && dist(b.x, b.y, c.x, c.y) < 22) {
                        c.hp -= 30; bi.remove(); removed = true;
                        if (c.hp <= 0) { explodeCar(c); score += 150; raiseWanted(2); }
                    }
                }
                for (Cop cop : cops) {
                    if (!removed && dist(b.x, b.y, cop.x, cop.y) < 18) {
                        cop.hp -= 40; bi.remove(); removed = true;
                        if (cop.hp <= 0) score += 100;
                    }
                }
            } else {
                if (!removed && dist(b.x, b.y, player.x, player.y) < 18) {
                    player.hp -= 8; bi.remove(); removed = true;
                    spawnParticles((int)player.x, (int)player.y, BLOOD_RED, 5);
                }
            }
        }
        cars.removeIf(c -> c.hp <= 0);
        cops.removeIf(c -> c.hp <= 0);
    }

    void updateCops() {
        if (wantedLevel > 0 && frame % Math.max(30, 120/wantedLevel) == 0 && cops.size() < wantedLevel * 2) {
            double ang = Math.random() * Math.PI * 2;
            cops.add(new Cop((int)(player.x + Math.cos(ang)*380), (int)(player.y + Math.sin(ang)*380)));
        }
        for (Cop cop : cops) {
            cop.update(player);
            if (cop.shootCD <= 0 && dist(cop.x, cop.y, player.x, player.y) < 260) {
                double angle = Math.atan2(player.y - cop.y, player.x - cop.x);
                bullets.add(new Bullet(cop.x, cop.y, angle, false));
                cop.shootCD = 70;
            }
        }
    }

    void updateCoins() {
        coins.removeIf(c -> {
            if (dist(player.x, player.y, c.x, c.y) < 22) {
                score += 50;
                spawnParticles((int)c.x, (int)c.y, NEON_YELLOW, 8);
                return true;
            }
            return false;
        });
    }

    void updateParticles()  { particles.removeIf(p  -> { p.update();  return p.life <= 0; }); }
    void updateExplosions() { explosions.removeIf(ex -> { ex.update(); return ex.done();   }); }

    void updateCamera() {
        camX = Math.max(0, Math.min((int)player.x - W/2, MAP_W - W));
        camY = Math.max(0, Math.min((int)player.y - H/2, MAP_H - H));
    }

    void explodeCar(CarObj c) {
        explosions.add(new Explosion((int)c.x, (int)c.y));
        spawnParticles((int)c.x, (int)c.y, NEON_ORANGE, 20);
        if (player.inCar && player.currentCar == c) { player.inCar = false; player.currentCar = null; }
    }

    void raiseWanted(int n) { wantedLevel = Math.min(5, wantedLevel + n); wantedCooldown = 600; }

    void spawnParticles(int x, int y, Color col, int n) {
        for (int i = 0; i < n; i++) particles.add(new Particle(x, y, col));
    }

    double dist(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1-x2, y1-y2);
    }

    // ── RENDER ─────────────────────────────────────────
    void render() {
        bufG.setColor(Color.BLACK);
        bufG.fillRect(0, 0, W, H);
        switch (state) {
            case SPLASH   -> drawSplash(bufG);
            case PLAYING  -> { drawWorld(bufG); drawHUD(bufG); }
            case PAUSED   -> { drawWorld(bufG); drawHUD(bufG); drawOverlay(bufG, "PAUSED", NEON_PINK, "Press P to Resume"); }
            case GAMEOVER -> drawEndScreen(bufG, "WASTED", BLOOD_RED, new Color(255,60,60));
            case WIN      -> drawEndScreen(bufG, "MISSION COMPLETE!", NEON_YELLOW, NEON_CYAN);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (buffer != null) g.drawImage(buffer, 0, 0, null);
    }

    // ── SPLASH SCREEN ──────────────────────────────────
    void drawSplash(Graphics2D g) {
        // Background gradient
        GradientPaint bg = new GradientPaint(0,0,new Color(5,0,20), W,H,new Color(20,0,50));
        g.setPaint(bg); g.fillRect(0,0,W,H);

        // Scanlines
        g.setColor(new Color(0,0,0,50));
        for (int y=0; y<H; y+=3) g.drawLine(0,y,W,y);

        // City silhouette
        drawSilhouette(g);

        // Animated neon grid
        g.setColor(new Color(0,255,200,15));
        for (int x=0; x<W; x+=40) g.drawLine(x,H/2,x,H);
        for (int y=H/2; y<H; y+=40) g.drawLine(0,y,W,y);

        long elapsed = System.currentTimeMillis() - splashStart;
        float alpha = Math.min(1f, elapsed/1500f);
        int a = (int)(alpha*255);

        // Title glow layers
        g.setFont(new Font("Impact", Font.BOLD, 74));
        FontMetrics fm = g.getFontMetrics();
        int tx = W/2 - fm.stringWidth("GTA VII")/2;
        for (int glow=18; glow>0; glow-=3) {
            g.setColor(new Color(255,20,147, Math.max(0,(int)(alpha*12))));
            g.drawString("GTA VII", tx+glow/3, 210+glow/3);
        }
        g.setColor(new Color(255,20,147,a));
        g.drawString("GTA VII", tx, 210);

        // Subtitle
        g.setFont(new Font("Impact", Font.BOLD, 30));
        String sub = "VICE CITY REBORN";
        g.setColor(new Color(0,255,220, a));
        g.drawString(sub, W/2 - g.getFontMetrics().stringWidth(sub)/2, 252);

        // Developer credit - bsmatek
        g.setFont(new Font("Courier New", Font.BOLD, 19));
        String dev = "DEVELOPED BY  SAHI AHMED YASSIN";
        g.setColor(new Color(255,220,0,a));
        g.drawString(dev, W/2 - g.getFontMetrics().stringWidth(dev)/2, 295);

        // Blink press enter
        if (elapsed > 1200 && (elapsed/500)%2==0) {
            g.setFont(new Font("Courier New", Font.BOLD, 16));
            String ps = "[ PRESS  ENTER  TO  START ]";
            g.setColor(NEON_CYAN);
            g.drawString(ps, W/2 - g.getFontMetrics().stringWidth(ps)/2, 370);
        }

        // Controls
        g.setFont(new Font("Courier New", Font.PLAIN, 13));
        g.setColor(new Color(180,180,200,200));
        String[] ctrl = {
            "WASD / Arrows  -  Move",
            "E              -  Enter / Exit Car",
            "SPACE / F      -  Shoot",
            "SHIFT          -  Sprint / Boost",
            "P              -  Pause"
        };
        int cy = 415;
        for (String s : ctrl) {
            g.drawString(s, W/2-130, cy); cy+=22;
        }

        // Corner watermark
        drawWatermark(g);
    }

    void drawSilhouette(Graphics2D g) {
        g.setColor(new Color(15,5,30));
        int[] xs = {0,0,60,60,90,90,120,120,150,150,190,190,240,240,280,280,330,330,
                    380,380,430,430,480,480,530,530,580,580,640,640,700,700,760,760,
                    810,810,860,860,900,900};
        int[] ys = {H,370,370,310,310,270,270,240,240,300,300,260,260,305,305,220,220,
                    270,270,200,200,250,250,345,345,280,280,230,230,265,265,295,295,
                    240,240,280,280,350,350,H};
        g.fillPolygon(xs,ys,xs.length);
    }

    void drawWatermark(Graphics2D g) {
        g.setFont(new Font("Courier New", Font.BOLD, 11));
        g.setColor(new Color(255,20,147,160));
        g.drawString("SAY", W-55, H-28);
        g.setColor(new Color(0,255,220,160));
        g.drawString("GAMES", W-65, H-14);
    }

    // ── WORLD ──────────────────────────────────────────
    void drawWorld(Graphics2D g) {
        g.translate(-camX, -camY);

        // Grass base
        g.setColor(GRASS_COL);
        g.fillRect(0,0,MAP_W,MAP_H);

        // Roads
        g.setColor(ROAD_DARK);
        for (int x=0; x<MAP_W; x+=300) g.fillRect(x,0,80,MAP_H);
        for (int y=0; y<MAP_H; y+=300) g.fillRect(0,y,MAP_W,80);

        // Road markings
        float[] dash = {20f,20f};
        g.setColor(ROAD_LINE);
        g.setStroke(new BasicStroke(2,BasicStroke.CAP_BUTT,BasicStroke.JOIN_MITER,1,dash,frame*2f));
        for (int x=0; x<MAP_W; x+=300) g.drawLine(x+40,0,x+40,MAP_H);
        for (int y=0; y<MAP_H; y+=300) g.drawLine(0,y+40,MAP_W,y+40);
        g.setStroke(new BasicStroke(1));

        // Sidewalks
        g.setColor(SIDEWALK);
        for (int x=0; x<MAP_W; x+=300) { g.fillRect(x-8,0,8,MAP_H); g.fillRect(x+80,0,8,MAP_H); }
        for (int y=0; y<MAP_H; y+=300) { g.fillRect(0,y-8,MAP_W,8); g.fillRect(0,y+80,MAP_W,8); }

        for (Building b  : buildings)  b.draw(g, frame);
        for (Coin c      : coins)       c.draw(g, frame);
        for (CarObj c    : cars)        c.draw(g, frame);
        for (Cop cop     : cops)        cop.draw(g, frame);
        for (Bullet b    : bullets)     b.draw(g);
        for (Particle p  : particles)   p.draw(g);
        for (Explosion ex: explosions)  ex.draw(g);
        player.draw(g, frame);

        // Car enter prompt
        if (!player.inCar)
            for (CarObj c : cars)
                if (c.nearPlayer) {
                    g.setFont(new Font("Courier New", Font.BOLD, 13));
                    g.setColor(NEON_YELLOW);
                    g.drawString("[E] Enter Car", (int)c.x-30, (int)c.y-32);
                }

        g.translate(camX, camY);
    }

    // ── HUD ────────────────────────────────────────────
    void drawHUD(Graphics2D g) {
        // Top bar
        g.setColor(new Color(0,0,0,170));
        g.fillRect(0,0,W,58);

        // Score
        g.setFont(new Font("Impact", Font.BOLD, 22));
        g.setColor(NEON_YELLOW);
        g.drawString("$" + score, 15, 32);

        // HP bar
        g.setColor(new Color(60,0,0));
        g.fillRoundRect(15,38,130,12,6,6);
        g.setColor(player.hp > 40 ? new Color(0,210,60) : BLOOD_RED);
        g.fillRoundRect(15,38,(int)(130*player.hp/100.0),12,6,6);
        g.setColor(new Color(200,200,200));
        g.setFont(new Font("Courier New", Font.BOLD, 11));
        g.drawString("HP  " + player.hp + "%", 152, 49);

        // Wanted stars
        int sx = W-185;
        g.setFont(new Font("Dialog", Font.BOLD, 20));
        for (int i=0; i<5; i++) {
            g.setColor(i < wantedLevel ? new Color(255,60,60) : new Color(70,70,70));
            g.drawString("★", sx + i*32, 33);
        }
        g.setFont(new Font("Courier New", Font.BOLD, 11));
        g.setColor(new Color(200,180,180));
        g.drawString("WANTED", sx+12, 50);

        // Goal progress bar
        g.setFont(new Font("Courier New", Font.BOLD, 12));
        g.setColor(new Color(200,200,200,180));
        g.drawString("GOAL $2000", W/2-42, 18);
        g.setColor(new Color(40,40,40));
        g.fillRoundRect(W/2-42,22,85,7,4,4);
        g.setColor(NEON_CYAN);
        g.fillRoundRect(W/2-42,22,(int)(85.0*Math.min(score,2000)/2000),7,4,4);

        // In-car indicator
        if (player.inCar) {
            g.setColor(NEON_CYAN);
            g.setFont(new Font("Courier New", Font.BOLD, 13));
            g.drawString("[ IN VEHICLE ]  E = Exit", 15, H-12);
        }

        // Minimap
        drawMinimap(g);

        // Subtle dev watermark
        g.setFont(new Font("Courier New", Font.PLAIN, 10));
        g.setColor(new Color(255,20,147,90));
        g.drawString("SAHI AHMED YASSIN", W-142, H-5);
    }

    void drawMinimap(Graphics2D g) {
        int mx=W-112, my=H-112, mw=102, mh=102;
        g.setColor(new Color(0,0,0,190));
        g.fillRoundRect(mx,my,mw,mh,10,10);
        g.setColor(NEON_PINK);
        g.drawRoundRect(mx,my,mw,mh,10,10);
        float sx=(float)mw/MAP_W, sy=(float)mh/MAP_H;
        g.setColor(new Color(70,50,90));
        for (Building b : buildings)
            g.fillRect(mx+(int)(b.x*sx), my+(int)(b.y*sy), Math.max(2,(int)(b.w*sx)), Math.max(2,(int)(b.h*sy)));
        g.setColor(new Color(60,100,255));
        for (Cop c : cops)  g.fillOval(mx+(int)(c.x*sx)-2, my+(int)(c.y*sy)-2,5,5);
        g.setColor(NEON_YELLOW);
        for (Coin c : coins) g.fillRect(mx+(int)(c.x*sx), my+(int)(c.y*sy),2,2);
        g.setColor(NEON_CYAN);
        g.fillOval(mx+(int)(player.x*sx)-4, my+(int)(player.y*sy)-4,8,8);
    }

    void drawOverlay(Graphics2D g, String title, Color tc, String sub) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0,0,W,H);
        g.setFont(new Font("Impact", Font.BOLD, 60));
        g.setColor(tc);
        int tx = W/2 - g.getFontMetrics().stringWidth(title)/2;
        g.drawString(title, tx, H/2);
        g.setFont(new Font("Courier New", Font.BOLD, 18));
        g.setColor(NEON_CYAN);
        g.drawString(sub, W/2 - g.getFontMetrics().stringWidth(sub)/2, H/2+50);
    }

    void drawEndScreen(Graphics2D g, String title, Color c1, Color c2) {
        GradientPaint bg = new GradientPaint(0,0,new Color(5,0,15),W,H,new Color(20,0,40));
        g.setPaint(bg); g.fillRect(0,0,W,H);
        drawSilhouette(g);
        g.setFont(new Font("Impact", Font.BOLD, 68));
        // Shadow
        g.setColor(new Color(0,0,0,180));
        g.drawString(title, W/2 - g.getFontMetrics().stringWidth(title)/2+4, H/2+4);
        g.setColor(c1);
        g.drawString(title, W/2 - g.getFontMetrics().stringWidth(title)/2, H/2);
        g.setFont(new Font("Impact", Font.BOLD, 26));
        g.setColor(c2);
        String sc = "Score: $" + score;
        g.drawString(sc, W/2 - g.getFontMetrics().stringWidth(sc)/2, H/2+55);
        g.setFont(new Font("Courier New", Font.BOLD, 15));
        g.setColor(new Color(200,200,200));
        String re = "Press ENTER to Play Again";
        g.drawString(re, W/2 - g.getFontMetrics().stringWidth(re)/2, H/2+100);
        g.setFont(new Font("Courier New", Font.BOLD, 14));
        g.setColor(NEON_PINK);
        String dev = "BY SAHI AHMED YASSIN";
        g.drawString(dev, W/2 - g.getFontMetrics().stringWidth(dev)/2, H/2+130);
        drawWatermark(g);
    }

    // ── INPUT ──────────────────────────────────────────
    @Override public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k >= 0 && k < 256) keys[k] = true;
        if (state == State.SPLASH && k == KeyEvent.VK_ENTER) { state = State.PLAYING; return; }
        if ((state == State.GAMEOVER || state == State.WIN) && k == KeyEvent.VK_ENTER) { initWorld(); state = State.PLAYING; return; }
        if (state == State.PLAYING) {
            if (k == KeyEvent.VK_P) state = State.PAUSED;
            if (k == KeyEvent.VK_E) toggleCar();
            if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_F) shoot();
        } else if (state == State.PAUSED && k == KeyEvent.VK_P) {
            state = State.PLAYING;
        }
    }
    @Override public void keyReleased(KeyEvent e) { int k=e.getKeyCode(); if(k>=0&&k<256) keys[k]=false; }
    @Override public void keyTyped(KeyEvent e) {}

    void toggleCar() {
        if (player.inCar) {
            player.x = player.currentCar.x + 42;
            player.y = player.currentCar.y;
            player.inCar = false; player.currentCar = null;
        } else {
            for (CarObj c : cars) {
                if (dist(player.x, player.y, c.x, c.y) < 45) {
                    player.inCar = true; player.currentCar = c;
                    raiseWanted(1); break;
                }
            }
        }
    }

    void shoot() {
        double[] angles = {-Math.PI/2, 0, Math.PI/2, Math.PI};
        bullets.add(new Bullet((int)player.x,(int)player.y, angles[player.dir], true));
        spawnParticles((int)player.x,(int)player.y, NEON_ORANGE, 3);
        if (wantedLevel == 0) raiseWanted(1);
    }
}

// ══════════════════════════════════════════════════════
// PLAYER
// ══════════════════════════════════════════════════════
class Player {
    double x, y, vx, vy;
    int hp=100, dir=2;
    boolean inCar=false;
    CarObj currentCar;

    Player(int x, int y) { this.x=x; this.y=y; }

    void update(int mw, int mh) {
        if (inCar && currentCar != null) {
            currentCar.vx = vx * 1.9;
            currentCar.vy = vy * 1.9;
            currentCar.x += currentCar.vx;
            currentCar.y += currentCar.vy;
            currentCar.x = Math.max(20, Math.min(mw-20, currentCar.x));
            currentCar.y = Math.max(20, Math.min(mh-20, currentCar.y));
            x = currentCar.x; y = currentCar.y;
        } else {
            x = Math.max(10, Math.min(mw-10, x+vx));
            y = Math.max(10, Math.min(mh-10, y+vy));
        }
    }

    void draw(Graphics2D g, int frame) {
        if (inCar) return;
        int px=(int)x, py=(int)y;
        // Shadow
        g.setColor(new Color(0,0,0,90));
        g.fillOval(px-10, py+10, 20, 8);
        // Body
        g.setColor(new Color(0,190,255));
        g.fillRoundRect(px-8, py-12, 16, 22, 5, 5);
        // Head
        g.setColor(new Color(255,200,140));
        g.fillOval(px-7, py-22, 14, 14);
        // Legs
        int lOff = (int)(Math.sin(frame*0.3)*3);
        g.setColor(new Color(20,20,80));
        g.fillRect(px-6, py+10, 5, 8+lOff);
        g.fillRect(px+1,  py+10, 5, 8-lOff);
        // Outline glow
        g.setColor(GamePanel.NEON_CYAN);
        g.drawRoundRect(px-8, py-12, 16, 22, 5, 5);
        // Direction dot
        int[] ddx={0,1,0,-1}, ddy={-1,0,1,0};
        g.setColor(GamePanel.NEON_YELLOW);
        g.fillOval(px+ddx[dir]*11-3, py+ddy[dir]*11-3, 6, 6);
    }
}

// ══════════════════════════════════════════════════════
// CAR
// ══════════════════════════════════════════════════════
class CarObj {
    double x, y, vx, vy;
    Color color;
    int dir, hp=100;
    boolean nearPlayer=false;
    static final int[][] DIRS = {{0,-2},{2,0},{0,2},{-2,0}};

    CarObj(int x, int y, Color col, int dir) {
        this.x=x; this.y=y; this.color=col; this.dir=dir;
        vx=DIRS[dir][0]; vy=DIRS[dir][1];
    }

    void update(int mw, int mh) {
        x+=vx; y+=vy;
        if (x<30||x>mw-30) { vx=-vx; dir=(dir+2)%4; }
        if (y<30||y>mh-30) { vy=-vy; dir=(dir+2)%4; }
        x=Math.max(30,Math.min(mw-30,x));
        y=Math.max(30,Math.min(mh-30,y));
    }

    void draw(Graphics2D g, int frame) {
        int cx=(int)x, cy=(int)y;
        // Shadow
        g.setColor(new Color(0,0,0,100));
        g.fillRoundRect(cx-18, cy-10+9, 36, 20, 6, 6);
        // Body
        g.setColor(color);
        g.fillRoundRect(cx-18, cy-10, 36, 20, 7, 7);
        // Roof
        g.setColor(color.darker());
        g.fillRoundRect(cx-11, cy-8, 22, 16, 4, 4);
        // Windows
        g.setColor(new Color(160,220,255,170));
        g.fillRoundRect(cx-9, cy-7, 18, 14, 3, 3);
        // Headlights
        g.setColor(new Color(255,255,200));
        g.fillOval(cx+14, cy-5, 6, 5); g.fillOval(cx+14, cy+1, 6, 5);
        // Taillights
        g.setColor(new Color(255,0,0));
        g.fillOval(cx-20, cy-5, 5, 5); g.fillOval(cx-20, cy+1, 5, 5);
        // Wheels
        g.setColor(new Color(25,25,25));
        g.fillOval(cx-16,cy-14,8,8); g.fillOval(cx+8,cy-14,8,8);
        g.fillOval(cx-16,cy+6, 8,8); g.fillOval(cx+8,cy+6, 8,8);
        // Neon underglow pulse
        int pulse = 50+(int)(30*Math.sin(frame*0.08+x*0.01));
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),pulse));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(cx-20,cy-12,40,24,9,9);
        g.setStroke(new BasicStroke(1));
        // HP bar if damaged
        if (hp<100) {
            g.setColor(new Color(180,0,0)); g.fillRect(cx-18,cy-22,36,4);
            g.setColor(new Color(0,200,0)); g.fillRect(cx-18,cy-22,(int)(36.0*hp/100),4);
        }
    }
}

// ══════════════════════════════════════════════════════
// BUILDING
// ══════════════════════════════════════════════════════
class Building {
    int x,y,w,h, neonType;
    Color color, neonColor;

    Building(int x,int y,int w,int h,Color c,Color nc) {
        this.x=x;this.y=y;this.w=w;this.h=h;color=c;neonColor=nc;
        neonType=(int)(Math.random()*3);
    }

    void draw(Graphics2D g, int frame) {
        // Shadow
        g.setColor(new Color(0,0,0,90));
        g.fillRect(x+6,y+6,w,h);
        // Body
        g.setColor(color);
        g.fillRect(x,y,w,h);
        // Shine
        g.setPaint(new GradientPaint(x,y,new Color(255,255,255,25),x+w,y+h,new Color(0,0,0,0)));
        g.fillRect(x,y,w,h); g.setPaint(null);
        // Windows
        for (int wx=x+6; wx<x+w-10; wx+=12) {
            for (int wy=y+6; wy<y+h-10; wy+=14) {
                boolean lit = ((wx*3+wy+frame/30)%7 != 0);
                g.setColor(lit ? new Color(255,240,180,110) : new Color(40,50,70,80));
                g.fillRect(wx,wy,8,10);
            }
        }
        // Neon
        int pulse = 160+(int)(90*Math.sin(frame*0.05+x*0.005));
        g.setColor(new Color(neonColor.getRed(),neonColor.getGreen(),neonColor.getBlue(),pulse));
        g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(neonType) {
            case 0 -> g.drawRect(x+4,y+4,w-8,h-8);
            case 1 -> { g.drawLine(x+10,y+10,x+w-10,y+10); g.drawLine(x+10,y+h-10,x+w-10,y+h-10); }
            case 2 -> g.drawOval(x+10,y+10,w-20,h-20);
        }
        g.setStroke(new BasicStroke(1));
        g.setColor(neonColor.darker().darker()); g.drawRect(x,y,w,h);
        // Rooftop antenna
        g.setColor(new Color(neonColor.getRed(),neonColor.getGreen(),neonColor.getBlue(),140));
        g.fillRect(x+w/2-2,y-8,4,10);
        g.fillOval(x+w/2-4,y-13,8,8);
    }
}

// ══════════════════════════════════════════════════════
// COIN
// ══════════════════════════════════════════════════════
class Coin {
    double x,y;
    Coin(int x,int y){this.x=x;this.y=y;}
    void draw(Graphics2D g, int frame) {
        int pulse=(int)(180+70*Math.sin(frame*0.1+x*0.01));
        g.setColor(new Color(255,220,0,50)); g.fillOval((int)x-12,(int)y-12,24,24);
        g.setColor(new Color(255,220,0,pulse)); g.fillOval((int)x-7,(int)y-7,14,14);
        g.setColor(new Color(255,255,150)); g.drawOval((int)x-7,(int)y-7,14,14);
        g.setFont(new Font("Courier New",Font.BOLD,9));
        g.setColor(new Color(160,120,0));
        g.drawString("$",(int)x-3,(int)y+4);
    }
}

// ══════════════════════════════════════════════════════
// BULLET
// ══════════════════════════════════════════════════════
class Bullet {
    double x,y,vx,vy; int life=60; boolean fromPlayer;
    Bullet(double x,double y,double angle,boolean fp) {
        this.x=x;this.y=y;fromPlayer=fp;
        vx=Math.cos(angle)*12; vy=Math.sin(angle)*12;
    }
    void update(){x+=vx;y+=vy;life--;}
    void draw(Graphics2D g){
        g.setColor(fromPlayer?GamePanel.NEON_YELLOW:GamePanel.BLOOD_RED);
        g.fillOval((int)x-3,(int)y-3,7,7);
        g.setColor(fromPlayer?new Color(255,255,0,90):new Color(255,0,0,80));
        g.fillOval((int)x-7,(int)y-7,14,14);
    }
}

// ══════════════════════════════════════════════════════
// COP
// ══════════════════════════════════════════════════════
class Cop {
    double x,y; int hp=100, shootCD=0;
    Cop(int x,int y){this.x=x;this.y=y;}
    void update(Player p){
        double dx=p.x-x, dy=p.y-y, d=Math.hypot(dx,dy);
        if(d>0){x+=dx/d*1.6;y+=dy/d*1.6;}
        shootCD=Math.max(0,shootCD-1);
    }
    void draw(Graphics2D g, int frame){
        int cx=(int)x,cy=(int)y;
        g.setColor(new Color(0,0,0,80)); g.fillOval(cx-10,cy+10,20,8);
        g.setColor(new Color(20,30,165)); g.fillRoundRect(cx-8,cy-12,16,22,4,4);
        g.setColor(new Color(255,200,140)); g.fillOval(cx-7,cy-22,14,14);
        g.setColor(new Color(10,10,110)); g.fillRect(cx-8,cy-25,16,6); g.fillRect(cx-5,cy-30,10,7);
        g.setColor(new Color(255,220,0)); g.fillRect(cx-3,cy-8,6,6);
        // Siren blink
        boolean blink=(frame/8)%2==0;
        g.setColor(blink?new Color(255,50,50,200):new Color(50,50,255,200));
        g.fillOval(cx-4,cy-34,8,6);
        if(hp<100){
            g.setColor(GamePanel.BLOOD_RED); g.fillRect(cx-15,cy-42,30,4);
            g.setColor(new Color(0,200,0)); g.fillRect(cx-15,cy-42,(int)(30.0*hp/100),4);
        }
    }
}

// ══════════════════════════════════════════════════════
// EXPLOSION
// ══════════════════════════════════════════════════════
class Explosion {
    int x,y,tick=0; static final int MAX=28;
    Explosion(int x,int y){this.x=x;this.y=y;}
    void update(){tick++;}
    boolean done(){return tick>=MAX;}
    void draw(Graphics2D g){
        float t=(float)tick/MAX;
        int r=(int)(85*t);
        g.setColor(new Color(255,(int)(100*(1-t)),0,(int)(220*(1-t)))); g.fillOval(x-r,y-r,r*2,r*2);
        int r2=(int)(40*t);
        g.setColor(new Color(255,255,220,(int)(210*(1-t)))); g.fillOval(x-r2,y-r2,r2*2,r2*2);
        g.setColor(new Color(255,80,0,(int)(140*(1-t))));
        g.setStroke(new BasicStroke(3));
        g.drawOval(x-(int)(95*t),y-(int)(95*t),(int)(190*t),(int)(190*t));
        g.setStroke(new BasicStroke(1));
    }
}

// ══════════════════════════════════════════════════════
// PARTICLE
// ══════════════════════════════════════════════════════
class Particle {
    double x,y,vx,vy; int life,maxLife; Color color;
    Particle(int x,int y,Color c){
        this.x=x;this.y=y;color=c;
        double a=Math.random()*Math.PI*2, s=1+Math.random()*5;
        vx=Math.cos(a)*s; vy=Math.sin(a)*s;
        maxLife=20+(int)(Math.random()*20); life=maxLife;
    }
    void update(){x+=vx;y+=vy;vy+=0.15;life--;}
    void draw(Graphics2D g){
        float al=(float)life/maxLife;
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),(int)(al*255)));
        g.fillOval((int)x-3,(int)y-3,6,6);
    }
}
