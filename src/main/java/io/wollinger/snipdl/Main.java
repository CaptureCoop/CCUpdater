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

    public static void main(String[] args) throws MalformedURLException {
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

            try (BufferedInputStream in = new BufferedInputStream(openStream(uri));
                 FileOutputStream fileOutputStream = new FileOutputStream(filename)) {
                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                    fileOutputStream.write(dataBuffer, 0, bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static InputStream openStream(URL url) {
        try {
            HttpURLConnection httpcon = (HttpURLConnection) url.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

            return httpcon.getInputStream();
        } catch (IOException e) {
            String error = e.toString();
            throw new RuntimeException(e);
        }
    }

    public static void notEnoughArguments(String arg) {
        System.out.println("Not enough arguments after: " + arg);
        System.exit(0);
    }
}
