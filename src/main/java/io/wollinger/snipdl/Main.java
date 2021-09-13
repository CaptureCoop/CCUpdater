package io.wollinger.snipdl;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Main {
    private static final String VERSION = "0.0.1";
    private static String url;
    private static String filename;
    private static String dir;

    public static void main(String[] args) throws IOException {
        if(args.length <= 0) {
            System.out.println("SnipDL " + VERSION);
            System.out.println("Available Arguments:");
            System.out.println("-url https://server.com/file.zip");
            System.out.println("-filename newName.zip");
            System.out.println("-dir C:/place/to/save/");
        } else {
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

            URL uri = new URL(url);

            if(filename == null)
                filename = new File(uri.getFile()).getName();

            System.out.println("\nStarting download from: " + uri);
            System.out.println("Saving to: " + new File(filename).getAbsolutePath());

            HttpURLConnection httpcon = (HttpURLConnection) uri.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

            try (BufferedInputStream in = new BufferedInputStream(httpcon.getInputStream());
                FileOutputStream fileOutputStream = new FileOutputStream(filename)) {

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                int total = httpcon.getContentLength();
                int downloaded = 0;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                    downloaded += bytesRead;
                    System.out.println(downloaded + "/" + total);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Done.");
        }
    }

    public static void notEnoughArguments(String arg) {
        System.out.println("Not enough arguments after: " + arg);
        System.exit(0);
    }
}
