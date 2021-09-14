package net.capturecoop.snipupdater;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {
    private static String url;
    private static String filename;
    private static String dir;
    private static boolean gui;
    private static String exec;
    private static boolean extract;
    private static boolean deleteFile;

    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        if(args.length <= 0) {
            System.out.println("SnipUpdater");
            System.out.println("Available Arguments:");
            System.out.println("-url https://server.com/file.zip");
            System.out.println("-filename newName.zip");
            System.out.println("-dir C:/place/to/save/");
            System.out.println("-gui");
            System.out.println("-exec");
            System.out.println("-extract");
            System.out.println("-deleteFile");
        } else {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            int index = 0;
            for(String part : args) {
                switch(part) {
                    case "-url": url = checkArg(index, args); break;
                    case "-filename": filename = checkArg(index, args); break;
                    case "-dir": dir = checkArg(index, args); break;
                    case "-gui": gui = true; break;
                    case "-exec": exec = checkArg(index, args); break;
                    case "-extract": extract = true; break;
                    case "-deleteFile": deleteFile = true; break;
                }
                index++;
            }
            run();
        }
    }

    public static String checkArg(int index, String[] args) {
        String arg = null;
        if(args.length > index + 1) arg = args[index + 1];
        else notEnoughArguments(args[index]);
        return arg;
    }

    public static void run() {
        System.out.println("SnipUpdater");
        JFrame frame = new JFrame();
        JProgressBar progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 100);
        if(gui) {
            frame.setSize(512, 128);
            progressBar.setToolTipText("test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);
            frame.setTitle("Starting...");
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            int width = gd.getDisplayMode().getWidth();
            int height = gd.getDisplayMode().getHeight();
            frame.setLocation(width / 2 - frame.getWidth() / 2, height / 2 - frame.getHeight() / 2);
            frame.add(progressBar);
        }

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
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        executor.scheduleAtFixedRate(helloRunnable, 0, 1, TimeUnit.SECONDS);
    }

    public static void executeDL(JFrame frame, JProgressBar progressBar) throws IOException, InterruptedException {
        URL uri = new URL(url);

        if(filename == null)
            filename = new File(uri.getFile()).getName();

        HttpURLConnection httpcon = (HttpURLConnection) uri.openConnection();
        httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

        String path = filename;
        if(dir != null && !dir.isEmpty()) {
            new File(dir).mkdirs();
            path = dir + "//" + filename;
        }


        path = new File(path).getAbsolutePath();

        System.out.println("\nStarting download from: " + uri);
        System.out.println("Saving to: " + path);

        try (BufferedInputStream in = new BufferedInputStream(httpcon.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(path)) {
            frame.setTitle("Downloading...");
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            int total = httpcon.getContentLength();
            int downloaded = 0;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
                downloaded += bytesRead;

                final int currentProgress = (int) ((((double)downloaded) / ((double)total)) * 100d);
                if(gui) progressBar.setValue(currentProgress);
                System.out.println("Status: " + currentProgress + "%");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(extract) {
            byte[] buffer = new byte[4096];
            FileInputStream fis = new FileInputStream(path);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ZipInputStream zis = new ZipInputStream(bis);
            ZipEntry ze;

            String extractTo = getCurrentFolder();
            if(dir != null && !dir.isEmpty())
                extractTo = extractTo + dir;
            while ((ze = zis.getNextEntry()) != null) {
                Path filePath = Paths.get(extractTo).resolve(ze.getName());
                if(!ze.isDirectory()) {
                    try (FileOutputStream fos = new FileOutputStream(filePath.toFile());
                         BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            bos.write(buffer, 0, len);
                        }
                    }
                } else {
                    filePath.toFile().mkdirs();
                }
            }
            fis.close();
            bis.close();
            zis.close();
        }

        if(exec != null && !exec.isEmpty()) {
            String toExecute = dir + "//" + exec;
            if(dir == null)
                toExecute = exec;
            if(exec.toLowerCase().endsWith(".jar")) {
                System.out.println("Launching jar: " + toExecute);
                new ProcessBuilder("java", "-jar", toExecute).start();

            } else {
                System.out.println("Launching: " + toExecute);
                Process p = new ProcessBuilder(toExecute).start();
                if(deleteFile) //We wait if deleteFile is true to avoid issues
                    p.waitFor();
            }
        }

        if(deleteFile) {
            System.out.println("Deleting file: " + path);
            new File(path).delete();
        }
        frame.dispose();
    }

    public static String getCurrentFolder() {
        String folderToUseString = null;
        String jarFolder;
        try {
            folderToUseString = URLDecoder.decode(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(), "UTF-8");
        } catch (UnsupportedEncodingException | URISyntaxException e) {
            e.printStackTrace();
        }

        if(folderToUseString != null) {
            File folderToUse = new File(folderToUseString);
            if (folderToUse.getName().endsWith(".jar"))
                jarFolder = folderToUseString.replace(folderToUse.getName(), "");
            else
                jarFolder = folderToUseString;
            return jarFolder;
        }
        return null;
    }

    public static void notEnoughArguments(String arg) {
        System.out.println("Not enough arguments after: " + arg);
        System.exit(0);
    }
}
