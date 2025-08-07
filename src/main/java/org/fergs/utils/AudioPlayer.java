package org.fergs.utils;

import lombok.Getter;
import lombok.Setter;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

@Getter @Setter
public class AudioPlayer {
    public Clip clip;
    public Clip hoverClip;
    public FloatControl gainControl;
    /**
     * Initializes the AudioPlayer and preloads the hover sound effect.
     * This should be called once at application startup.
     */
    public AudioPlayer() {
        preloadHoverSound("/audio/button-hover.wav");
    }
    /**
     * Loads a WAV resource and loops it indefinitely.
     * @param resourcePath e.g. "/audio/ambience.wav"
     */
    public void playLoop(String resourcePath) {
        stop();
        try {
            final URL url = getClass().getResource(resourcePath);
            if (url == null) throw new IOException("Resource not found: " + resourcePath);

            final AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            final AudioFormat baseFormat = ais.getFormat();
            final AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            try (final AudioInputStream dais = AudioSystem.getAudioInputStream(decodedFormat, ais)) {
                clip = AudioSystem.getClip();
                clip.open(dais);
                gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Plays the hover sound effect for UI elements.
     */
    public void playHoverSound() {
        if (hoverClip == null) return;

        if (hoverClip.isRunning()) {
            hoverClip.stop();
        }

        hoverClip.setFramePosition(0);

        hoverClip.start();
    }
    /**
     * Preloads a hover sound effect for UI elements.
     * @param resourcePath e.g. "/audio/hover.wav"
     */
    public void preloadHoverSound(String resourcePath) {
        try {
            final URL url = getClass().getResource(resourcePath);
            if (url == null) throw new IOException("Resource not found: " + resourcePath);

            final AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            final AudioFormat baseFormat = ais.getFormat();
            final AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(),
                    16,
                    baseFormat.getChannels(),
                    baseFormat.getChannels() * 2,
                    baseFormat.getSampleRate(),
                    false
            );
            try (final AudioInputStream dais = AudioSystem.getAudioInputStream(decodedFormat, ais)) {
                hoverClip = AudioSystem.getClip();
                hoverClip.open(dais);
                gainControl = (FloatControl) hoverClip.getControl(FloatControl.Type.MASTER_GAIN);
            }
        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            e.printStackTrace();
        }
    }
    /** Stops playback and frees resources. */
    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
            gainControl = null;
        }
    }

    /**
     * @param volume linear [0.0f → 1.0f]
     */
    public void setVolume(float volume) {
        if (gainControl == null) return;
        final float min = gainControl.getMinimum();
        final float max = gainControl.getMaximum();
        final float dB  = min + (max - min) * volume;
        gainControl.setValue(dB);
    }
}