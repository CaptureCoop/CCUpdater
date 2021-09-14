package io.wollinger.snipdl;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final String VERSION = "0.0.1";
    private static String url;
    private static String filename;
    private static String dir;

    public static void main(String[] args) throws IOException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(args.length <= 0) {
            System.out.println("SnipDL " + VERSION);
            System.out.println("Available Arguments:");
            System.out.println("-url https://server.com/file.zip");
            System.out.println("-filename newName.zip");
            System.out.println("-dir C:/place/to/save/");
        } else {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            int index = 0;
            for(String part : args) {
                switch(part) {
                    case "-url":
                        if(args.length > index + 1) url = args[index + 1];
                        else notEnoughArguments(part);
                        break;
                    case "-filename":
                        if(args.length > index + 1) filename = args[index + 1];
                        else notEnoughArguments(part);
                        break;
                    case "-dir":
                        if(args.length > index + 1) dir = args[index + 1];
                        else notEnoughArguments(part);
                        break;
                }
                index++;
            }
            run();
        }
    }

    public static void run() throws IOException {
        JFrame frame = new JFrame();
        frame.setSize(512, 128);
        JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        progressBar.setToolTipText("test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
        frame.setTitle("Starting...");
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        frame.setLocation(width / 2 - frame.getWidth() / 2, height / 2 - frame.getHeight() / 2);
        frame.add(progressBar);

        final int[] secondsRemaining = {6};

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        Runnable helloRunnable = () -> {
            secondsRemaining[0]--;
            String str = "Starting in " + secondsRemaining[0];
            frame.setTitle(str);
            System.out.println(str);
            if(secondsRemaining[0] == 0) {
                executor.shutdown();
                try {
                    executeDL(frame, progressBar);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        executor.scheduleAtFixedRate(helloRunnable, 0, 1, TimeUnit.SECONDS);
    }

    public static void executeDL(JFrame frame, JProgressBar progressBar) throws IOException {
        URL uri = new URL(url);

        if(filename == null)
            filename = new File(uri.getFile()).getName();

        System.out.println("\nStarting download from: " + uri);
        System.out.println("Saving to: " + new File(filename).getAbsolutePath());

        HttpURLConnection httpcon = (HttpURLConnection) uri.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

        try (BufferedInputStream in = new BufferedInputStream(httpcon.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
            frame.setTitle("Downloading...");
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            int total = httpcon.getContentLength();
            int downloaded = 0;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                downloaded += bytesRead;

                final int currentProgress = (int) ((((double)downloaded) / ((double)total)) * 100d);
                progressBar.setValue(currentProgress);
                System.out.println("Status: " + currentProgress + "%");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.dispose();
    }

    public static void notEnoughArguments(String arg) {
        System.out.println("Not enough arguments after: " + arg);
        System.exit(0);
    }
}
