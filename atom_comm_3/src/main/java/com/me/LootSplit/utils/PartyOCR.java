package com.me.LootSplit.utils;

import net.sourceforge.tess4j.*;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.Mat;
import org.opencv.core.CvType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

import static java.lang.System.out;

public class PartyOCR {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static Mat loadAndPreprocessImage(String imagePath, Size targetSize) {
        Mat image = Imgcodecs.imread(imagePath);
        Imgproc.resize(image, image, targetSize);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        Mat sharpenedImage = new Mat(image.size(), CvType.CV_8UC1);
        Core.addWeighted(image, 1.5, Mat.zeros(image.size(), image.type()), 0, 0, sharpenedImage);
        return sharpenedImage;
    }

    public static List<Rect> performOCRAndGetBoundingBoxes(Mat image, ITesseract tesseract) throws TesseractException, IOException {
        File tempFile = File.createTempFile("tempImage", ".png");
        Imgcodecs.imwrite(tempFile.getAbsolutePath(), image);
        List<Rect> boundingBoxes = new ArrayList<>();
        List<Word> result = tesseract.getWords((List<BufferedImage>) tempFile, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        for (Word word : result) {
            Rect rect = new Rect(word.getBoundingBox().x, word.getBoundingBox().y,
                    word.getBoundingBox().width, word.getBoundingBox().height);
            boundingBoxes.add(rect);
        }
        tempFile.delete();
        return boundingBoxes;
    }

    public static int findKeywordIndex(List<Word> words, String[] keywords) {
        for (int i = 0; i < words.size(); i++) {
            for (String keyword : keywords) {
                if (words.get(i).getText().equalsIgnoreCase(keyword)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static void drawBoundingBox(Mat image, Rect rect, Scalar color) {
        Imgproc.rectangle(image, rect, color, 1);
    }

    public static Rect[] calculateAndDrawROIs(Mat image, Rect rect, boolean isCluster) {
        Rect leftROI, rightROI;
        if (isCluster) {
            leftROI = new Rect(rect.x - (int) (0.97 * rect.width), rect.y + 10 * rect.height,
                    rect.x + (int) (0.36 * rect.width),  rect.y + (int) (30.2 * rect.height));
            rightROI = new Rect(rect.x + (int) (1.98 * rect.width), rect.y + 10 * rect.height,
                    rect.x + (int) (2.65 * rect.width), rect.y + (int) (30.2 * rect.height));
        } else {
            leftROI = new Rect(rect.x - (int) (0.80 * rect.width), rect.y + (int) (10.3 * rect.height),
                    (int) (0.29 * rect.width), (int) (31 * rect.height));
            rightROI = new Rect(rect.x + (int) (1.61 * rect.width), rect.y + (int) (10.3 * rect.height),
                    (int) (2.65 * rect.width), (int) (31 * rect.height));
        }
        drawBoundingBox(image, leftROI, new Scalar(255, 0, 0));
        drawBoundingBox(image, rightROI, new Scalar(255, 0, 0));
        return new Rect[]{leftROI, rightROI};
    }

    public static List<String> detectNamesInColumns(Mat image, Rect leftROI, Rect rightROI, Rect keywordROI, ITesseract tesseract) throws TesseractException, IOException {
        List<String> detectedPlayers = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            // Adjust the ROI for each column
            Rect leftRect = new Rect(leftROI.x, leftROI.y + i * (int) (2.05 * keywordROI.height), (int) (1.33 * keywordROI.width), (int) (2.05 * keywordROI.height));
            Rect rightRect = new Rect(rightROI.x, rightROI.y + i * (int) (2.05* keywordROI.height), (int) (1.33 * keywordROI.width), (int) (2.05 * keywordROI.height));
            Mat leftCrop = new Mat(image, leftRect);
            Mat rightCrop = new Mat(image, rightRect);

            // Convert Mat to BufferedImage
            MatOfByte matOfByte = new MatOfByte();
            byte[] imgByte;
            Imgcodecs.imencode(".png", leftCrop, matOfByte);
            imgByte = matOfByte.toArray();
            BufferedImage leftImage = ImageIO.read(new ByteArrayInputStream(imgByte));
            Imgcodecs.imencode(".png", rightCrop, matOfByte);
            imgByte = matOfByte.toArray();
            BufferedImage rightImage = ImageIO.read(new ByteArrayInputStream(imgByte));

            String leftText = tesseract.doOCR(leftImage).strip();
            String rightText = tesseract.doOCR(rightImage).strip();

            System.out.printf("%s\n%s\n", leftText, rightText);

            detectedPlayers.add(leftText.isEmpty() ? "Unknown" : leftText);
            detectedPlayers.add(rightText.isEmpty() ? "Unknown" : rightText);

            drawBoundingBox(image, leftRect, new Scalar(0, 0, 255));
            drawBoundingBox(image, rightRect, new Scalar(0, 0, 255));
            // Show the 2 columns
            Imgcodecs.imwrite("DetectedNames.png", image);
            Imgcodecs.imwrite("LeftColumn.png", leftCrop);
            Imgcodecs.imwrite("RightColumn.png", rightCrop);
        }
        return detectedPlayers;
    }

    public Mat loadAndPreprocessImage(InputStream imgStream, Size targetSize) throws IOException {
        out.println("Loading and preprocessing image");
        byte[] imgBytes = imgStream.readAllBytes();
        Mat image = Imgcodecs.imdecode(new MatOfByte(imgBytes), Imgcodecs.IMREAD_COLOR);
        Imgproc.resize(image, image, targetSize);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2GRAY);
        Mat sharpenedImage = new Mat(image.size(), CvType.CV_8UC1);
        Core.addWeighted(image, 1.5, Mat.zeros(image.size(), image.type()), 0, 0, sharpenedImage);
        return sharpenedImage;
    }

    public List<String> extractNamesFromPartyImage(InputStream partyScreenshot) throws IOException {
        try {
            Mat screenshot = loadAndPreprocessImage(partyScreenshot, new Size(720, 840));

            // Convert Mat to BufferedImage
            MatOfByte matOfByte = new MatOfByte();
            byte[] imgByte;
            Imgcodecs.imencode(".png", screenshot, matOfByte);
            imgByte = matOfByte.toArray();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgByte));

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
            tesseract.setLanguage("eng");

            List<Word> ocrResult = tesseract.getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            String[] keywords = {"cluster", "prioridad"};
            int keywordIndex = findKeywordIndex(ocrResult, keywords);

            if (keywordIndex == -1) {
                out.println("Could not find the word 'Cluster' or 'Prioridad'");
                return new ArrayList<>();
            }

            out.printf("Keyword found at index: %d with x: %d, y: %d, w: %d, h: %d\n",
                    keywordIndex, ocrResult.get(keywordIndex).getBoundingBox().x,
                    ocrResult.get(keywordIndex).getBoundingBox().y,
                    ocrResult.get(keywordIndex).getBoundingBox().width,
                    ocrResult.get(keywordIndex).getBoundingBox().height);

            Word keywordWord = ocrResult.get(keywordIndex);
            Rect keywordRect = new Rect(keywordWord.getBoundingBox().x, keywordWord.getBoundingBox().y,
                    keywordWord.getBoundingBox().width, keywordWord.getBoundingBox().height);
            drawBoundingBox(screenshot, keywordRect, new Scalar(0, 255, 0));

            boolean isCluster = keywordWord.getText().equalsIgnoreCase("cluster");
            Rect[] ROIs = calculateAndDrawROIs(screenshot, keywordRect, isCluster);
            out.printf("Left ROI: %s, Right ROI: %s\n", ROIs[0], ROIs[1]);
            List<String> detectedPlayers = detectNamesInColumns(screenshot, ROIs[0], ROIs[1], keywordRect, tesseract);
            return detectedPlayers;

        } catch (IOException e) {
            throw new RuntimeException("Error extracting names from PartyImage due to IOException: " + e.getMessage());
        }
        catch (Exception e) {
            throw new RuntimeException("Error extracting names from PartyImage: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            String imagePath = "E:\\Courses\\OpenCV\\imgs\\image_for_test.png";
            Mat screenshot = loadAndPreprocessImage(imagePath, new Size(720, 840));

            // Convert Mat to BufferedImage
            MatOfByte matOfByte = new MatOfByte();
            byte[] imgByte;
            Imgcodecs.imencode(".png", screenshot, matOfByte);
            imgByte = matOfByte.toArray();
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgByte));

            ITesseract tesseract = new Tesseract();
            tesseract.setDatapath("C:/Program Files/Tesseract-OCR/tessdata");
            tesseract.setLanguage("eng");

            List<Word> ocrResult = tesseract.getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD);
            String[] keywords = {"cluster", "prioridad"};
            int keywordIndex = findKeywordIndex(ocrResult, keywords);
            out.printf("Keyword found at index: %d with x: %d, y: %d, w: %d, h: %d\n",
                    keywordIndex, ocrResult.get(keywordIndex).getBoundingBox().x,
                    ocrResult.get(keywordIndex).getBoundingBox().y,
                    ocrResult.get(keywordIndex).getBoundingBox().width,
                    ocrResult.get(keywordIndex).getBoundingBox().height);

            if (keywordIndex == -1) {
                out.println("Could not find the word 'Cluster' or 'Prioridad'");
                return;
            }

            Word keywordWord = ocrResult.get(keywordIndex);
            Rect keywordRect = new Rect(keywordWord.getBoundingBox().x, keywordWord.getBoundingBox().y,
                    keywordWord.getBoundingBox().width, keywordWord.getBoundingBox().height);
            drawBoundingBox(screenshot, keywordRect, new Scalar(0, 255, 0));

            boolean isCluster = keywordWord.getText().equalsIgnoreCase("cluster");
            Rect[] ROIs = calculateAndDrawROIs(screenshot, keywordRect, isCluster);
            out.printf("Left ROI: %s, Right ROI: %s\n", ROIs[0], ROIs[1]);
            List<String> detectedPlayers = detectNamesInColumns(screenshot, ROIs[0], ROIs[1], keywordRect, tesseract);
            int filledCount = (int) detectedPlayers.stream().filter(name -> !name.equals("Unknown")).count();
            double accuracy = (double) filledCount / detectedPlayers.size() * 100;

            out.println("Detected " + detectedPlayers.size() + " players with accuracy of " + accuracy + "%");
            out.println("Detected players: " + detectedPlayers);

            Imgcodecs.imwrite("DetectedNames.png", screenshot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
