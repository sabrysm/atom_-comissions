package com.me.GuildBot.utils;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class ImageTextExtractor {
    public static String processImage(InputStream inputStream) throws IOException, NullPointerException {
        // Load the image
        BufferedImage img = ImageIO.read(inputStream);

        // Resize the image
        img = resizeImage(img, 492 * 2, 583 * 2);

        // Enhance the contrast and brightness
        img = enhanceImage(img);

        // Extract text using Tesseract
        String text, cleanedText;
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
        tesseract.setLanguage("eng");
        tesseract.setVariable("debug_file", "/dev/null");
        try {
            text = tesseract.doOCR(img);
        } catch (TesseractException e) {
            throw new IOException("Error extracting text from the image: " + e.getMessage());
        }

        // Filter and clean text
        cleanedText = cleanText(text);

        return cleanedText;

    }

    public static String processImage(String imgPath) {
        try {
            // Load the OpenCV library
            System.loadLibrary("opencv_java490");

            // Load the image
            BufferedImage img = getImageFromPath(imgPath);

            // Resize the image
            img = resizeImage(img, 492 * 2, 583 * 2);
//
            // Enhance the contrast and brightness
            img = enhanceImage(img);


            // Extract text using Tesseract
            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
            tesseract.setLanguage("eng");

            String text = tesseract.doOCR(img);

            // Filter and clean text

            return cleanText(text);

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    protected static BufferedImage getImageFromPath(String imgPath) throws IOException{
        try {
            Mat mat = Imgcodecs.imread(imgPath);

            // Change img to Gray scale
            Mat grayMat = new Mat();
            Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY);

            // resize image
            Mat resizedMat = new Mat();
            Size size = new Size(mat.width() * 1.9f, mat.height() * 1.9f);
            Imgproc.resize(grayMat, resizedMat, size);

            // Apply filter on image
            // Define the kernel as a 3x3 matrix
            Mat kernel = new Mat(3, 3, CvType.CV_32F);
            kernel.put(0, 0,  0, -1,  0);
            kernel.put(1, 0, -1,  5, -1);
            kernel.put(2, 0,  0, -1,  0);

            // Create an output Mat to store the result
            Mat filteredMat = new Mat();

            // Apply the filter using the kernel
            Imgproc.filter2D(resizedMat, filteredMat, -1, kernel);

            // Convert Mat to BufferedImage
            MatOfByte matOfByte = new MatOfByte();
            byte[] imgByte;
            Imgcodecs.imencode(".png", resizedMat, matOfByte);
            imgByte = matOfByte.toArray();
            return ImageIO.read(new ByteArrayInputStream(imgByte));
        } catch (Exception e) {
            throw new IOException("Error reading the image: " + e.getMessage());
        }
    }


    protected static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        resizedImage.getGraphics().drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
        return resizedImage;
    }

    protected static BufferedImage enhanceImage(BufferedImage image) {
        // Step 1: Convert the image to grayscale (if not already grayscale)
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = grayImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Step 2: Apply RescaleOp to adjust contrast and brightness
        float scaleFactor = 1.2f;  // Adjust contrast
        float offset = 20f;        // Adjust brightness
        RescaleOp rescaleOp = new RescaleOp(scaleFactor, offset, null);
        BufferedImage enhancedImage = rescaleOp.filter(grayImage, null);

        return enhancedImage;
    }

    private static String cleanText(String text) {
        String[] lines = text.split("\n");
        StringBuilder cleanedText = new StringBuilder();
        for (String line : lines) {
            line = line.replaceAll("^[^a-zA-Z]+", "").trim();
            if (!line.isEmpty()) {
                cleanedText.append(line).append("\n");
            }
        }
        return cleanedText.toString().trim();
    }

    public static List<String> getNames(InputStream imageStream) throws IOException {
        String text;
        List<String> lines;
        ArrayList<String> namesList = new ArrayList<>();
        try {
            text = processImage(imageStream);
        } catch (IOException e) {
            throw new IOException("Error processing the image: " + e.getMessage());
        } catch (NullPointerException e) {
            throw new NullPointerException("The image provided is null: " + e.getMessage());
        }
        lines = List.of(text.split("\n"));
        // Split by space
        for (String name : lines) {
            for (String n : name.split(" ")) {
                namesList.add(n);
            }
        }
        System.out.println(namesList);
        return namesList;
    }

    public static List<String> getNames(String imgPath) throws IOException {
        String text;
        List<String> lines;
        ArrayList<String> namesList = new ArrayList<>();
        text = processImage(imgPath);
        lines = List.of(text.split("\n"));
        // Split by space
        for (String name : lines) {
            for (String n : name.split(" ")) {
                namesList.add(n);
            }
        }
        return namesList;
    }

    public static void main(String[] args) throws IOException {
        String imgPath = "E:\\Programming\\MindCloud\\imgs\\image_for_test.png";
        List<String> names = getNames(imgPath);
        System.out.print("[");
        for (String name : names) {
            System.out.print(name);
            System.out.print(", ");
        }
        System.out.println("]");
    }
}
