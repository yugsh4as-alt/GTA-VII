import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.awt.image.BufferedImage;

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
//  GAME PANEL - Core engine
// ══════════════════════════════════════════════════════
class GamePanel extends JPanel implements KeyListener, ActionListener {

    static final int W = 900, H = 650;
    static final int TILE = 40;

    // Game States
    enum State { SPLASH, PLAYING, PAUSED, GAMEOVER, WIN }
    State state = State.SPLASH;

    // Player
    Player player;

    // World
    List<Building> buildings = new ArrayList<>();
    List<Car> cars = new ArrayList<>();
    List<Coin> coins = new ArrayList<>();
    List<Bullet> bullets = new ArrayList<>();
    List<Cop> cops = new ArrayList<>();
    List<Explosion> explosions = new ArrayList<>();
    List<Particle> particles = new ArrayList<>();

    // Camera
    int camX = 0, camY = 0;
    static final int MAP_W = 2400, MAP_H = 2400;

    // UI
    int score = 0;
    int wantedLevel = 0;
    long splashTimer = 0;
    float titleAlpha = 0f;
    boolean[] keys = new boolean[256];

    // Wanted system
    int wantedCooldown = 0;

    // Timers
    javax.swing.Timer gameTimer;
    int frame = 0;

    // Fonts
    Font pixelFont;
    Font titleFont;
    Font uiFont;

    // Colors - Neon Vice City Palette
    static final Color NEON_PINK   = new Color(255, 20, 147);
    static final Color NEON_CYAN   = new Color(0, 255, 220);
    static final Color NEON_YELLOW = new Color(255, 220, 0);
    static final Color NEON_ORANGE = new Color(255, 120, 0);
    static final Color ROAD_DARK   = new Color(30, 30, 35);
    static final Color ROAD_LINE   = new Color(255, 220, 0, 120);
    static final Color SIDEWALK    = new Color(60, 55, 70);
    static final Color GRASS       = new Color(20, 80, 40);
    static final Color BLOOD_RED   = new Color(200, 0, 30);

    GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setFocusable(true);
        addKeyListener(this);

        pixelFont  = new Font("Courier New", Font.BOLD, 13);
        titleFont  = new Font("Impact", Font.BOLD, 72);
        uiFont     = new Font("Courier New", Font.BOLD, 16);

        initWorld();

        gameTimer = new javax.swing.Timer(16, this);
        gameTimer.start();
        splashTimer = System.currentTimeMillis();
    }

    void initWorld() {
        player  = new Player(MAP_W / 2, MAP_H / 2);
        buildings.clear(); cars.clear(); coins.clear(); cops.clear();

        Random rng = new Random(42);

        // City blocks
        int[][] blockGrid = {
            {200,200}, {400,200}, {600,200}, {800,200}, {1000,200}, {1200,200}, {1600,200}, {1800,200},
            {200,500}, {600,500}, {1000,500}, {1400,500}, {1800,500},
            {200,800}, {400,800}, {800,800}, {1200,800}, {1600,800}, {2000,800},
            {200,1100},{600,1100},{1000,1100},{1400,1100},{1800,1100},
            {200,1400},{400,1400},{800,1400},{1200,1400},{1600,1400},{2000,1400},
            {200,1700},{600,1700},{1000,1700},{1400,1700},{1800,1700},
            {200,2000},{400,2000},{800,2000},{1200,2000},{1600,2000},{2000,2000},
        };

        Color[] buildingColors = {
            new Color(60,20,80), new Color(10,50,80), new Color(80,20,20),
            new Color(20,70,50), new Color(70,60,10), new Color(40,10,60)
        };

        for (int[] b : blockGrid) {
            int bw = 80 + rng.nextInt(120);
            int bh = 80 + rng.nextInt(120);
            Color bc = buildingColors[rng.nextInt(buildingColors.length)];
            Color nc = rng.nextBoolean() ? NEON_PINK : (rng.nextBoolean() ? NEON_CYAN : NEON_YELLOW);
            buildings.add(new Building(b[0], b[1], bw, bh, bc, nc));
        }

        // Cars on roads
        int[][] carSpawns = {
            {MAP_W/2-300, MAP_H/2+100}, {MAP_W/2+200, MAP_H/2-200},
            {MAP_W/2-500, MAP_H/2-300}, {MAP_W/2+400, MAP_H/2+300},
            {MAP_W/2, MAP_H/2+500},     {MAP_W/2-700, MAP_H/2+500},
            {MAP_W/2+600, MAP_H/2-500}, {MAP_W/2-200, MAP_H/2-600},
        };
        Color[] carColors = {
            BLOOD_RED, new Color(30,100,255), new Color(255,180,0),
            new Color(0,200,80), new Color(200,200,200), NEON_PINK
        };
        for (int[] cs : carSpawns) {
            cars.add(new Car(cs[0], cs[1], carColors[rng.nextInt(carColors.length)], rng.nextInt(4)));
        }

        // Coins
        for (int i = 0; i < 40; i++) {
            int cx = 200 + rng.nextInt(MAP_W - 400);
            int cy = 200 + rng.nextInt(MAP_H - 400);
            coins.add(new Coin(cx, cy));
        }

        score = 0; wantedLevel = 0; wantedCooldown = 0;
        bullets.clear(); explosions.clear(); particles.clear();
    }

    // ── UPDATE ─────────────────────────────────────────
    @Override public void actionPerformed(ActionEvent e) {
        frame++;
        if (state == State.PLAYING) update();
        repaint();
    }

    void update() {
        handleInput();
        player.update(MAP_W, MAP_H);
        updateCars();
        updateBullets();
        updateCops();
        updatePickups();
        updateParticles();
        updateExplosions();
        updateCamera();
        wantedCooldown = Math.max(0, wantedCooldown - 1);
        if (wantedCooldown == 0 && wantedLevel > 0 && frame % 300 == 0)
            wantedLevel = Math.max(0, wantedLevel - 1);
        if (score >= 2000) state = State.WIN;
    }

    void handleInput() {
        double speed = player.inCar ? 4.5 : 2.5;
        if (keys[KeyEvent.VK_SHIFT]) speed *= 1.8;

        if (keys[KeyEvent.VK_W] || keys[KeyEvent.VK_UP])    { player.vy = -speed; player.dir = 0; }
        else if (keys[KeyEvent.VK_S] || keys[KeyEvent.VK_DOWN])  { player.vy =  speed; player.dir = 2; }
        else player.vy = 0;
        if (keys[KeyEvent.VK_A] || keys[KeyEvent.VK_LEFT])  { player.vx = -speed; player.dir = 3; }
        else if (keys[KeyEvent.VK_D] || keys[KeyEvent.VK_RIGHT]) { player.vx =  speed; player.dir = 1; }
        else player.vx = 0;
    }

    void updateCars() {
        for (Car c : cars) c.update(MAP_W, MAP_H);

        // Car collision / enter
        if (!player.inCar) {
            for (Car c : cars) {
                if (dist(player.x, player.y, c.x, c.y) < 35) {
                    // Player near car - show prompt (handled in render)
                    c.nearPlayer = true;
                } else c.nearPlayer = false;
            }
        }
    }

    void updateBullets() {
        Iterator<Bullet> bi = bullets.iterator();
        while (bi.hasNext()) {
            Bullet b = bi.next();
            b.update();
            if (b.x < 0 || b.x > MAP_W || b.y < 0 || b.y > MAP_H || b.life <= 0) { bi.remove(); continue; }
            // Hit cars
            for (Car c : cars) {
                if (dist(b.x, b.y, c.x, c.y) < 20) {
                    c.hp -= b.fromPlayer ? 25 : 0;
                    bi.remove();
                    if (c.hp <= 0) { explodeCar(c); score += 150; raiseWanted(2); }
                    break;
                }
            }
            // Hit cops
            if (b.fromPlayer) {
                for (Cop cop : cops) {
                    if (dist(b.x, b.y, cop.x, cop.y) < 16) {
                        cop.hp -= 40;
                        bi.remove(); break;
                    }
                }
            }
            // Hit player
            if (!b.fromPlayer && dist(b.x, b.y, player.x, player.y) < 18) {
                player.hp -= 10;
                bi.remove();
                spawnParticles((int)player.x, (int)player.y, BLOOD_RED, 6);
                if (player.hp <= 0) state = State.GAMEOVER;
            }
        }
        cars.removeIf(c -> c.hp <= 0);
        cops.removeIf(c -> c.hp <= 0);
    }

    void updateCops() {
        // Spawn cops based on wanted level
        if (wantedLevel > 0 && frame % (120 / wantedLevel) == 0 && cops.size() < wantedLevel * 2) {
            double ang = Math.random() * Math.PI * 2;
            double spawnR = 350;
            cops.add(new Cop((int)(player.x + Math.cos(ang)*spawnR), (int)(player.y + Math.sin(ang)*spawnR)));
        }
        for (Cop cop : cops) {
            cop.update(player, frame);
            if (cop.shootCooldown <= 0 && dist(cop.x, cop.y, player.x, player.y) < 250) {
                double angle = Math.atan2(player.y - cop.y, player.x - cop.x);
                bullets.add(new Bullet(cop.x, cop.y, angle, false));
                cop.shootCooldown = 60;
            }
        }
    }

    void updatePickups() {
        Iterator<Coin> ci = coins.iterator();
        while (ci.hasNext()) {
            Coin c = ci.next();
            if (dist(player.x, player.y, c.x, c.y) < 22) {
                score += 50; ci.remove();
                spawnParticles((int)c.x, (int)c.y, NEON_YELLOW, 8);
            }
        }
    }

    void updateParticles() {
        particles.removeIf(p -> { p.update(); return p.life <= 0; });
    }

    void updateExplosions() {
        explosions.removeIf(ex -> { ex.update(); return ex.done(); });
    }

    void updateCamera() {
        camX = (int)(player.x - W / 2);
        camY = (int)(player.y - H / 2);
        camX = Math.max(0, Math.min(camX, MAP_W - W));
        camY = Math.max(0, Math.min(camY, MAP_H - H));
    }

    void explodeCar(Car c) {
        explosions.add(new Explosion((int)c.x, (int)c.y));
        spawnParticles((int)c.x, (int)c.y, NEON_ORANGE, 20);
        if (player.inCar && player.currentCar == c) { player.inCar = false; player.currentCar = null; }
    }

    void raiseWanted(int amount) {
        wantedLevel = Math.min(5, wantedLevel + amount);
        wantedCooldown = 600;
    }

    void spawnParticles(int x, int y, Color color, int count) {
        for (int i = 0; i < count; i++) particles.add(new Particle(x, y, color));
    }

    double dist(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx*dx + dy*dy);
    }

    // ── RENDER ─────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        switch (state) {
            case SPLASH   -> drawSplash(g2);
            case PLAYING  -> drawGame(g2);
            case PAUSED   -> { drawGame(g2); drawPause(g2); }
            case GAMEOVER -> drawGameOver(g2);
            case WIN      -> drawWin(g2);
        }
    }

    void drawSplash(Graphics2D g) {
        // Background
        GradientPaint bg = new GradientPaint(0,0, new Color(5,0,15), W,H, new Color(15,0,40));
        g.setPaint(bg); g.fillRect(0,0,W,H);

        // Scanlines
        g.setColor(new Color(0,0,0,40));
        for (int y = 0; y < H; y += 3) g.drawLine(0,y,W,y);

        // City silhouette
        drawCitySilhouette(g);

        // Animated neon grid
        g.setColor(new Color(0,255,200,20));
        for (int x = 0; x < W; x+=40) g.drawLine(x,H/2,x,H);
        for (int y = H/2; y < H; y+=40) g.drawLine(0,y,W,y);

        // Title glow
        long elapsed = System.currentTimeMillis() - splashTimer;
        titleAlpha = Math.min(1f, elapsed / 1500f);

        // Shadow layers for glow effect
        for (int glow = 20; glow > 0; glow -= 2) {
            g.setFont(titleFont);
            g.setColor(new Color(255,20,147, (int)(titleAlpha * 8)));
            int tx = W/2 - g.getFontMetrics().stringWidth("GTA VII")/2;
            g.drawString("GTA VII", tx + glow/2, 200 + glow/2);
        }
        g.setFont(titleFont);
        g.setColor(new Color(255,20,147, (int)(titleAlpha*255)));
        int tx = W/2 - g.getFontMetrics().stringWidth("GTA VII")/2;
        g.drawString("GTA VII", tx, 200);

        // Subtitle
        g.setFont(new Font("Impact", Font.BOLD, 28));
        String sub = "VICE CITY REBORN";
        g.setColor(new Color(0,255,220, (int)(titleAlpha*200)));
        g.drawString(sub, W/2 - g.getFontMetrics().stringWidth(sub)/2, 245);

        // Developer watermark
        g.setFont(new Font("Courier New", Font.BOLD, 18));
        String dev = "DEVELOPED BY  SAHI AHMED YASSIN";
        g.setColor(new Color(255,220,0, (int)(titleAlpha*255)));
        g.drawString(dev, W/2 - g.getFontMetrics().stringWidth(dev)/2, 290);

        // Press start
        if (elapsed > 1000 && (elapsed/500) % 2 == 0) {
            g.setFont(uiFont);
            String ps = "[ PRESS ENTER TO START ]";
            g.setColor(NEON_CYAN);
            g.drawString(ps, W/2 - g.getFontMetrics().stringWidth(ps)/2, 380);
        }

        // Controls
        g.setFont(pixelFont);
        g.setColor(new Color(200,200,200,180));
        String[] controls = {
            "WASD / ARROWS  - Move",
            "E              - Enter / Exit Car",
            "SPACE / F      - Shoot",
            "P              - Pause",
            "SHIFT          - Sprint / Boost"
        };
        int cy2 = 430;
        for (String ctrl : controls) {
            g.drawString(ctrl, W/2 - 130, cy2);
            cy2 += 22;
        }

        // Corner logo
        drawCornerLogo(g);
    }

    void drawCitySilhouette(Graphics2D g) {
        g.setColor(new Color(20,5,35));
        int[] xp = {0,0,60,60,80,80,110,110,130,130,160,160,200,200,240,240,280,280,
                    320,320,370,370,420,420,460,460,500,500,550,550,600,600,650,650,
                    700,700,750,750,800,800,850,850,900,900,900};
        int[] yp = {H,380,380,320,320,280,280,250,250,310,310,260,260,300,300,220,220,
                    270,270,200,200,240,240,350,350,280,280,230,230,260,260,300,300,
                    240,240,280,280,310,310,270,270,350,350,H};
        g.fillPolygon(xp, yp, xp.length);
    }

    void drawCornerLogo(Graphics2D g) {
        g.setFont(new Font("Courier New", Font.BOLD, 11));
        g.setColor(new Color(255,20,147,180));
        g.drawString("SAY", W-55, H-28);
        g.setColor(new Color(0,255,220,180));
        g.drawString("GAMES", W-65, H-14);
    }

    void drawGame(Graphics2D g) {
        // World
        g.translate(-camX, -camY);
        drawWorld(g);
        g.translate(camX, camY);
        // HUD
        drawHUD(g);
    }

    void drawWorld(Graphics2D g) {
        // Base - grass
        g.setColor(GRASS);
        g.fillRect(0, 0, MAP_W, MAP_H);

        // Roads (grid)
        g.setColor(ROAD_DARK);
        for (int x = 0; x < MAP_W; x += 300) g.fillRect(x, 0, 80, MAP_H);
        for (int y = 0; y < MAP_H; y += 300) g.fillRect(0, y, MAP_W, 80);

        // Road markings
        g.setColor(ROAD_LINE);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{20,20}, frame*2));
        for (int x = 0; x < MAP_W; x += 300) g.drawLine(x+40, 0, x+40, MAP_H);
        for (int y = 0; y < MAP_H; y += 300) g.drawLine(0, y+40, MAP_W, y+40);
        g.setStroke(new BasicStroke(1));

        // Sidewalks
        g.setColor(SIDEWALK);
        for (int x = 0; x < MAP_W; x += 300) { g.fillRect(x-8, 0, 8, MAP_H); g.fillRect(x+80, 0, 8, MAP_H); }
        for (int y = 0; y < MAP_H; y += 300) { g.fillRect(0, y-8, MAP_W, 8); g.fillRect(0, y+80, MAP_W, 8); }

        // Buildings
        for (Building b : buildings) b.draw(g, frame);

        // Coins
        for (Coin c : coins) c.draw(g, frame);

        // Cars
        for (Car c : cars) c.draw(g, frame);

        // Cops
        for (Cop c : cops) c.draw(g);

        // Particles
        for (Particle p : particles) p.draw(g);

        // Explosions
        for (Explosion ex : explosions) ex.draw(g);

        // Player
        player.draw(g, frame);

        // Car enter prompt
        if (!player.inCar) {
            for (Car c : cars) {
                if (c.nearPlayer) {
                    g.setFont(new Font("Courier New", Font.BOLD, 12));
                    g.setColor(NEON_YELLOW);
                    g.drawString("[E] Enter Car", (int)c.x - 30, (int)c.y - 30);
                }
            }
        }
    }

    void drawHUD(Graphics2D g) {
        // Dark HUD panel top
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0, 0, W, 55);

        // Score
        g.setFont(new Font("Impact", Font.BOLD, 20));
        g.setColor(NEON_YELLOW);
        g.drawString("$" + score, 15, 30);

        // HP bar
        g.setFont(pixelFont);
        g.setColor(BLOOD_RED);
        g.fillRoundRect(15, 36, 120, 12, 6, 6);
        g.setColor(new Color(0,220,60));
        g.fillRoundRect(15, 36, (int)(120 * player.hp / 100.0), 12, 6, 6);
        g.setColor(Color.WHITE);
        g.drawString("HP", 140, 47);

        // Wanted stars
        int sx = W - 180;
        g.setFont(new Font("Courier New", Font.BOLD, 22));
        for (int i = 0; i < 5; i++) {
            g.setColor(i < wantedLevel ? new Color(255,60,60) : new Color(80,80,80));
            g.drawString("★", sx + i*30, 32);
        }
        g.setFont(pixelFont);
        g.setColor(new Color(200,200,200));
        g.drawString("WANTED", sx+8, 47);

        // Mini-map
        drawMinimap(g);

        // Car indicator
        if (player.inCar) {
            g.setColor(NEON_CYAN);
            g.setFont(new Font("Courier New", Font.BOLD, 13));
            g.drawString("🚗 IN VEHICLE  [E] Exit", 15, H - 15);
        }

        // Dev watermark (subtle)
        g.setFont(new Font("Courier New", Font.PLAIN, 10));
        g.setColor(new Color(255,20,147,100));
        g.drawString("SAHI AHMED YASSIN", W - 140, H - 5);

        // Score goal
        g.setFont(pixelFont);
        g.setColor(new Color(200,200,200,180));
        g.drawString("GOAL: $2000", W/2 - 45, 20);
        g.setColor(NEON_CYAN);
        g.fillRoundRect(W/2 - 45, 25, (int)(90.0 * score / 2000), 6, 3, 3);
        g.setColor(new Color(100,100,100));
        g.drawRoundRect(W/2 - 45, 25, 90, 6, 3, 3);
    }

    void drawMinimap(Graphics2D g) {
        int mx = W - 110, my = H - 110, mw = 100, mh = 100;
        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(mx, my, mw, mh, 10, 10);
        g.setColor(NEON_PINK);
        g.drawRoundRect(mx, my, mw, mh, 10, 10);

        float sx = (float)mw / MAP_W, sy = (float)mh / MAP_H;
        // Buildings
        g.setColor(new Color(80,60,100));
        for (Building b : buildings)
            g.fillRect(mx + (int)(b.x*sx), my + (int)(b.y*sy), Math.max(1,(int)(b.w*sx)), Math.max(1,(int)(b.h*sy)));
        // Cops
        g.setColor(new Color(50,100,255));
        for (Cop c : cops) g.fillOval(mx+(int)(c.x*sx)-2, my+(int)(c.y*sy)-2, 4, 4);
        // Coins
        g.setColor(NEON_YELLOW);
        for (Coin c : coins) g.fillRect(mx+(int)(c.x*sx), my+(int)(c.y*sy), 2, 2);
        // Player
        g.setColor(NEON_CYAN);
        g.fillOval(mx+(int)(player.x*sx)-3, my+(int)(player.y*sy)-3, 6, 6);
    }

    void drawPause(Graphics2D g) {
        g.setColor(new Color(0,0,0,160));
        g.fillRect(0,0,W,H);
        g.setFont(new Font("Impact", Font.BOLD, 56));
        g.setColor(NEON_PINK);
        String t = "PAUSED";
        g.drawString(t, W/2 - g.getFontMetrics().stringWidth(t)/2, H/2);
        g.setFont(uiFont);
        g.setColor(NEON_CYAN);
        String r = "Press P to Resume";
        g.drawString(r, W/2 - g.getFontMetrics().stringWidth(r)/2, H/2 + 50);
    }

    void drawGameOver(Graphics2D g) {
        g.setColor(new Color(10,0,0));
        g.fillRect(0,0,W,H);
        drawCitySilhouette(g);
        g.setFont(new Font("Impact", Font.BOLD, 72));
        g.setColor(BLOOD_RED);
        String t = "WASTED";
        int tx = W/2 - g.getFontMetrics().stringWidth(t)/2;
        g.drawString(t, tx+3, H/2+3);
        g.setColor(new Color(255,60,60));
        g.drawString(t, tx, H/2);
        g.setFont(uiFont);
        g.setColor(NEON_YELLOW);
        g.drawString("Score: $" + score, W/2 - 50, H/2 + 60);
        g.setColor(new Color(200,200,200));
        g.setFont(pixelFont);
        String r = "Press ENTER to Try Again";
        g.drawString(r, W/2 - g.getFontMetrics().stringWidth(r)/2, H/2+100);
        drawCornerLogo(g);
    }

    void drawWin(Graphics2D g) {
        GradientPaint bg = new GradientPaint(0,0,new Color(0,10,30),W,H,new Color(10,0,30));
        g.setPaint(bg); g.fillRect(0,0,W,H);
        // Flash effect
        g.setColor(new Color(255,220,0, (int)(80 + 60*Math.sin(frame*0.1))));
        g.fillRect(0,0,W,H);
        g.setFont(new Font("Impact", Font.BOLD, 60));
        g.setColor(NEON_YELLOW);
        String t = "MISSION COMPLETE!";
        g.drawString(t, W/2 - g.getFontMetrics().stringWidth(t)/2, H/2 - 40);
        g.setFont(new Font("Impact", Font.BOLD, 28));
        g.setColor(NEON_CYAN);
        String s = "Final Score: $" + score;
        g.drawString(s, W/2 - g.getFontMetrics().stringWidth(s)/2, H/2 + 20);
        g.setFont(uiFont);
        g.setColor(NEON_PINK);
        String dev = "SAHI AHMED YASSIN";
        g.drawString(dev, W/2 - g.getFontMetrics().stringWidth(dev)/2, H/2+70);
        g.setFont(pixelFont);
        g.setColor(new Color(200,200,200));
        String r = "Press ENTER to Play Again";
        g.drawString(r, W/2 - g.getFontMetrics().stringWidth(r)/2, H/2+110);
        drawCornerLogo(g);
    }

    // ── INPUT ──────────────────────────────────────────
    @Override public void keyPressed(KeyEvent e) {
        int kc = e.getKeyCode();
        if (kc < 256) keys[kc] = true;

        if (state == State.SPLASH && kc == KeyEvent.VK_ENTER) { state = State.PLAYING; return; }
        if ((state == State.GAMEOVER || state == State.WIN) && kc == KeyEvent.VK_ENTER) { initWorld(); state = State.PLAYING; return; }
        if (state == State.PLAYING) {
            if (kc == KeyEvent.VK_P) state = State.PAUSED;
            if (kc == KeyEvent.VK_E) toggleCar();
            if (kc == KeyEvent.VK_SPACE || kc == KeyEvent.VK_F) shoot();
        } else if (state == State.PAUSED && kc == KeyEvent.VK_P) {
            state = State.PLAYING;
        }
    }

    @Override public void keyReleased(KeyEvent e) { if (e.getKeyCode() < 256) keys[e.getKeyCode()] = false; }
    @Override public void keyTyped(KeyEvent e) {}

    void toggleCar() {
        if (player.inCar) {
            player.inCar = false;
            player.x = player.currentCar.x + 40;
            player.y = player.currentCar.y;
            player.currentCar = null;
        } else {
            for (Car c : cars) {
                if (dist(player.x, player.y, c.x, c.y) < 45) {
                    player.inCar = true;
                    player.currentCar = c;
                    raiseWanted(1);
                    break;
                }
            }
        }
    }

    void shoot() {
        double[] dirAngles = {-Math.PI/2, 0, Math.PI/2, Math.PI};
        double angle = dirAngles[player.dir];
        bullets.add(new Bullet((int)player.x, (int)player.y, angle, true));
        if (wantedLevel == 0) raiseWanted(1);
        spawnParticles((int)player.x, (int)player.y, NEON_ORANGE, 3);
    }
}

// ══════════════════════════════════════════════════════
// PLAYER
// ══════════════════════════════════════════════════════
class Player {
    double x, y, vx, vy;
    int hp = 100, dir = 2;
    boolean inCar = false;
    Car currentCar;

    Player(int x, int y) { this.x = x; this.y = y; }

    void update(int mw, int mh) {
        if (inCar && currentCar != null) {
            currentCar.vx = vx * 1.8;
            currentCar.vy = vy * 1.8;
            currentCar.x += currentCar.vx;
            currentCar.y += currentCar.vy;
            currentCar.x = Math.max(20, Math.min(mw-20, currentCar.x));
            currentCar.y = Math.max(20, Math.min(mh-20, currentCar.y));
            x = currentCar.x;
            y = currentCar.y;
        } else {
            x += vx; y += vy;
            x = Math.max(10, Math.min(mw-10, x));
            y = Math.max(10, Math.min(mh-10, y));
        }
    }

    void draw(Graphics2D g, int frame) {
        if (inCar) return; // car draws player
        int px = (int)x, py = (int)y;
        // Shadow
        g.setColor(new Color(0,0,0,80));
        g.fillOval(px-10, py+10, 20, 8);
        // Body
        g.setColor(new Color(0,200,255));
        g.fillRoundRect(px-8, py-12, 16, 22, 4, 4);
        // Head
        g.setColor(new Color(255,200,140));
        g.fillOval(px-7, py-22, 14, 14);
        // Legs
        int legOff = (int)(Math.sin(frame*0.3)*3);
        g.setColor(new Color(30,30,80));
        g.fillRect(px-6, py+10, 5, 8 + legOff);
        g.fillRect(px+1, py+10, 5, 8 - legOff);
        // Neon outline
        g.setColor(GamePanel.NEON_CYAN);
        g.drawRoundRect(px-8, py-12, 16, 22, 4, 4);
        // Direction indicator
        int[] dx = {0,1,0,-1}, dy = {-1,0,1,0};
        g.setColor(GamePanel.NEON_YELLOW);
        g.fillOval(px + dx[dir]*10-3, py + dy[dir]*10-3, 6, 6);
    }
}

// ══════════════════════════════════════════════════════
// CAR
// ══════════════════════════════════════════════════════
class Car {
    double x, y, vx, vy;
    Color color;
    int dir, hp = 100;
    boolean nearPlayer = false;
    Player occupant = null;
    static final int[][] DIRS = {{0,-2},{2,0},{0,2},{-2,0}};

    Car(int x, int y, Color color, int dir) {
        this.x = x; this.y = y; this.color = color; this.dir = dir;
        vx = DIRS[dir][0]; vy = DIRS[dir][1];
    }

    void update(int mw, int mh) {
        x += vx; y += vy;
        if (x < 30 || x > mw-30) { vx = -vx; dir = (dir+2)%4; }
        if (y < 30 || y > mh-30) { vy = -vy; dir = (dir+2)%4; }
        x = Math.max(30, Math.min(mw-30, x));
        y = Math.max(30, Math.min(mh-30, y));
    }

    void draw(Graphics2D g, int frame) {
        int cx = (int)x, cy = (int)y;
        // Shadow
        g.setColor(new Color(0,0,0,100));
        g.fillRoundRect(cx-18, cy-10+8, 36, 20, 5, 5);
        // Car body
        g.setColor(color);
        g.fillRoundRect(cx-18, cy-10, 36, 20, 6, 6);
        // Roof
        Color roofColor = color.darker();
        g.setColor(roofColor);
        g.fillRoundRect(cx-12, cy-8, 24, 16, 4, 4);
        // Windows
        g.setColor(new Color(150,220,255,180));
        g.fillRoundRect(cx-10, cy-7, 20, 14, 3, 3);
        // Headlights
        g.setColor(new Color(255,255,200));
        g.fillOval(cx+14, cy-5, 6, 5);
        g.fillOval(cx+14, cy+1, 6, 5);
        // Taillights
        g.setColor(new Color(255,0,0));
        g.fillOval(cx-20, cy-5, 5, 5);
        g.fillOval(cx-20, cy+1, 5, 5);
        // Wheels
        g.setColor(new Color(30,30,30));
        g.fillOval(cx-16, cy-14, 8, 8); g.fillOval(cx+8, cy-14, 8, 8);
        g.fillOval(cx-16, cy+6, 8, 8);  g.fillOval(cx+8, cy+6, 8, 8);
        // Neon underglow
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g.setStroke(new BasicStroke(3));
        g.drawRoundRect(cx-20, cy-12, 40, 24, 8, 8);
        g.setStroke(new BasicStroke(1));
        // HP bar if damaged
        if (hp < 100) {
            g.setColor(new Color(200,0,0));
            g.fillRect(cx-18, cy-20, 36, 4);
            g.setColor(new Color(0,200,0));
            g.fillRect(cx-18, cy-20, (int)(36.0*hp/100), 4);
        }
    }
}

// ══════════════════════════════════════════════════════
// BUILDING
// ══════════════════════════════════════════════════════
class Building {
    int x, y, w, h;
    Color color, neonColor;
    int neonType;

    Building(int x, int y, int w, int h, Color c, Color nc) {
        this.x=x; this.y=y; this.w=w; this.h=h; color=c; neonColor=nc;
        neonType = (int)(Math.random()*3);
    }

    void draw(Graphics2D g, int frame) {
        // Shadow
        g.setColor(new Color(0,0,0,80));
        g.fillRect(x+6, y+6, w, h);
        // Main body
        g.setColor(color);
        g.fillRect(x, y, w, h);
        // Gradient overlay
        GradientPaint shine = new GradientPaint(x,y,new Color(255,255,255,30),x+w,y+h,new Color(0,0,0,0));
        g.setPaint(shine);
        g.fillRect(x,y,w,h);
        g.setPaint(null);
        // Windows grid
        g.setColor(new Color(200,230,255,80));
        for (int wx = x+6; wx < x+w-10; wx+=12) {
            for (int wy = y+6; wy < y+h-10; wy+=14) {
                boolean lit = ((wx+wy+frame/30) % 7 != 0);
                g.setColor(lit ? new Color(255,240,180,120) : new Color(50,60,80,80));
                g.fillRect(wx, wy, 8, 10);
            }
        }
        // Neon sign
        int pulse = (int)(180 + 75*Math.sin(frame*0.05 + x));
        g.setColor(new Color(neonColor.getRed(), neonColor.getGreen(), neonColor.getBlue(), pulse));
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch(neonType) {
            case 0 -> g.drawRect(x+4, y+4, w-8, h-8);
            case 1 -> { g.drawLine(x+10,y+10,x+w-10,y+10); g.drawLine(x+10,y+h-10,x+w-10,y+h-10); }
            case 2 -> g.drawOval(x+10,y+10,w-20,h-20);
        }
        g.setStroke(new BasicStroke(1));
        // Outline
        g.setColor(neonColor.darker().darker());
        g.drawRect(x,y,w,h);
        // Rooftop details
        g.setColor(new Color(neonColor.getRed(),neonColor.getGreen(),neonColor.getBlue(),150));
        g.fillRect(x+w/2-2, y-8, 4, 10);
        g.fillOval(x+w/2-4, y-12, 8, 8);
    }
}

// ══════════════════════════════════════════════════════
// COIN
// ══════════════════════════════════════════════════════
class Coin {
    double x, y;
    Coin(int x, int y) { this.x=x; this.y=y; }
    void draw(Graphics2D g, int frame) {
        int pulse = (int)(180+75*Math.sin(frame*0.1+x));
        // Glow
        g.setColor(new Color(255,220,0,60));
        g.fillOval((int)x-12,(int)y-12,24,24);
        // Coin
        g.setColor(new Color(255,220,0,pulse));
        g.fillOval((int)x-7,(int)y-7,14,14);
        g.setColor(new Color(255,255,150));
        g.drawOval((int)x-7,(int)y-7,14,14);
        g.setFont(new Font("Courier New", Font.BOLD, 9));
        g.setColor(new Color(180,140,0));
        g.drawString("$", (int)x-3, (int)y+4);
    }
}

// ══════════════════════════════════════════════════════
// BULLET
// ══════════════════════════════════════════════════════
class Bullet {
    double x, y, vx, vy;
    int life = 60;
    boolean fromPlayer;
    Bullet(double x, double y, double angle, boolean fromPlayer) {
        this.x=x; this.y=y;
        double spd = 12;
        vx = Math.cos(angle)*spd; vy = Math.sin(angle)*spd;
        this.fromPlayer = fromPlayer;
    }
    void update() { x+=vx; y+=vy; life--; }
    void draw(Graphics2D g) {
        g.setColor(fromPlayer ? GamePanel.NEON_YELLOW : GamePanel.BLOOD_RED);
        g.fillOval((int)x-3,(int)y-3,6,6);
        g.setColor(fromPlayer ? new Color(255,255,0,100) : new Color(255,0,0,80));
        g.fillOval((int)x-6,(int)y-6,12,12);
    }
}

// ══════════════════════════════════════════════════════
// COP
// ══════════════════════════════════════════════════════
class Cop {
    double x, y;
    int hp = 100, shootCooldown = 0;
    Cop(int x, int y) { this.x=x; this.y=y; }
    void update(Player p, int frame) {
        double dx = p.x - x, dy = p.y - y;
        double dist = Math.sqrt(dx*dx+dy*dy);
        if (dist > 0) { x += dx/dist*1.5; y += dy/dist*1.5; }
        shootCooldown = Math.max(0, shootCooldown-1);
    }
    void draw(Graphics2D g) {
        int cx=(int)x, cy=(int)y;
        // Shadow
        g.setColor(new Color(0,0,0,80));
        g.fillOval(cx-10, cy+10, 20, 8);
        // Body - police blue
        g.setColor(new Color(20,30,160));
        g.fillRoundRect(cx-8, cy-12, 16, 22, 4, 4);
        // Head
        g.setColor(new Color(255,200,140));
        g.fillOval(cx-7, cy-22, 14, 14);
        // Hat
        g.setColor(new Color(10,10,100));
        g.fillRect(cx-8, cy-24, 16, 5);
        g.fillRect(cx-6, cy-29, 12, 7);
        // Badge
        g.setColor(new Color(255,220,0));
        g.fillRect(cx-3, cy-8, 6, 6);
        // HP bar
        if (hp < 100) {
            g.setColor(GamePanel.BLOOD_RED);
            g.fillRect(cx-15, cy-36, 30, 4);
            g.setColor(new Color(0,200,0));
            g.fillRect(cx-15, cy-36, (int)(30.0*hp/100), 4);
        }
        // Siren light
        g.setColor(new Color(255,60,60, 150+100*(shootCooldown%10<5?1:0)));
        g.fillOval(cx-4, cy-32, 8, 5);
    }
}

// ══════════════════════════════════════════════════════
// EXPLOSION
// ══════════════════════════════════════════════════════
class Explosion {
    int x, y, tick=0;
    static final int MAX=25;
    Explosion(int x, int y) { this.x=x; this.y=y; }
    void update() { tick++; }
    boolean done() { return tick >= MAX; }
    void draw(Graphics2D g) {
        float t = (float)tick/MAX;
        int r = (int)(80*t);
        // Outer
        g.setColor(new Color(255,(int)(100*(1-t)),0,(int)(220*(1-t))));
        g.fillOval(x-r, y-r, r*2, r*2);
        // Inner white
        int r2 = (int)(40*t);
        g.setColor(new Color(255,255,220,(int)(200*(1-t))));
        g.fillOval(x-r2, y-r2, r2*2, r2*2);
        // Ring
        g.setColor(new Color(255,80,0,(int)(150*(1-t))));
        g.setStroke(new BasicStroke(3));
        g.drawOval(x-(int)(90*t), y-(int)(90*t), (int)(180*t), (int)(180*t));
        g.setStroke(new BasicStroke(1));
    }
}

// ══════════════════════════════════════════════════════
// PARTICLE
// ══════════════════════════════════════════════════════
class Particle {
    double x, y, vx, vy;
    int life, maxLife;
    Color color;
    Particle(int x, int y, Color color) {
        this.x=x; this.y=y; this.color=color;
        double angle = Math.random()*Math.PI*2;
        double speed = 1+Math.random()*4;
        vx = Math.cos(angle)*speed; vy = Math.sin(angle)*speed;
        maxLife = 20+(int)(Math.random()*20); life=maxLife;
    }
    void update() { x+=vx; y+=vy; vy+=0.15; life--; }
    void draw(Graphics2D g) {
        float alpha = (float)life/maxLife;
        g.setColor(new Color(color.getRed(),color.getGreen(),color.getBlue(),(int)(alpha*255)));
        g.fillOval((int)x-3,(int)y-3,6,6);
    }
}

