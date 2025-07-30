package org.fergs.objects;

import java.util.Random;

public final class Flake {
    final Random rand = new Random();
    public int x;
    public int y;
    public int size;
    public int speed;
    public double angle;
    public double rotationSpeed;
    public double phase;
    public double phaseSpeed;
    public double drift;

    public Flake(int x, int y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = 1 + size / 4;
        this.rotationSpeed = (rand.nextDouble() - 0.5) * 0.1;
        this.phaseSpeed = 0.02 + rand.nextDouble() * 0.02;
        this.phase = rand.nextDouble() * Math.PI * 2;
        this.drift = 1 + rand.nextDouble() * 2;
    }
}
