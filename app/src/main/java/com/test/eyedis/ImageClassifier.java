package com.test.eyedis;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class ImageClassifier {
    /** Name of the model file stored in Assets. */
    private static final String MODEL_PATH = "Model.tflite";

    /** Name of the label file stored in Assets. */
    private static final String LABEL_PATH = "labels.txt";

    /** Dimensions of inputs. */
    private static final int DIM_BATCH_SIZE = 1;

    private static final int DIM_PIXEL_SIZE = 3;

    static final int DIM_IMG_SIZE_X = 224;
    static final int DIM_IMG_SIZE_Y = 224;

    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;


    /* Preallocated buffers for storing image data in. */
    private int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

    /** An instance of the driver class to run model inference with Tensorflow Lite. */
    private Interpreter tflite;
    private Interpreter gpu_tflite;

    /** Labels corresponding to the output of the vision model. */
    private List<String> labelList;

    /** A ByteBuffer to hold image data, to be feed into Tensorflow Lite as inputs. */
    private ByteBuffer imgData;

    /** An array to hold inference results, to be feed into Tensorflow Lite as outputs. */
    private float[][] labelProbArray;

    /** Finding **/
    private float accuracy;
    private String label;

    ImageClassifier(Context myContext) {

        try {
            GpuDelegate delegate = new GpuDelegate();
            Interpreter.Options options = (new Interpreter.Options()).addDelegate(delegate);
            gpu_tflite = new Interpreter(loadModelFile(myContext),options);
            tflite = new Interpreter(loadModelFile(myContext));
            labelList = loadLabelList(myContext);
        } catch (IOException io) {
//            Log.d(TAG, "Failed to load model and labels. ");
        }

        imgData =
                ByteBuffer.allocateDirect(
                        4 * DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
        imgData.order(ByteOrder.nativeOrder());
        labelProbArray = new float[1][labelList.size()];
//        Log.d(TAG, "Created a Tensorflow Lite Image Classifier.");
    }

    /** Classifies a frame from the preview stream. */
    String classifyFrame(Bitmap bitmap, boolean gpu) {
        if (tflite == null || gpu_tflite == null) {
//            Log.e(TAG, "Image classifier has not been initialized; Skipped.");
            return "Uninitialized Classifier.";
        }
        convertBitmapToByteBuffer(bitmap);
        // Here's where the magic happens!!!

        if (gpu) {
            System.out.println("using GPU");
            gpu_tflite.run(imgData, labelProbArray);
        } else {
            System.out.println("using CPU");
            tflite.run(imgData, labelProbArray);
        }

        accuracy = 0.0f;
        label = "";
        for (int i = 0; i < 10; i++) {
            if (accuracy < labelProbArray[0][i]) {
                accuracy = labelProbArray[0][i];
                label = labelList.get(i);
            }
        }
        // Confidence Level
        if (accuracy < 0.75f) {
            label = "others";
        }

        // print the results
        return label;
    }

    /** Closes tflite to release resources. */
    public void close() {
        tflite.close();
        tflite = null;
    }

    private MappedByteBuffer loadModelFile(Context myContext) throws IOException {
        // Open the model using an input stream, and memory map it to load
        AssetFileDescriptor fileDescriptor = myContext.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Reads label list from Assets. */
    private List<String> loadLabelList(Context myContext) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(myContext.getAssets().open(LABEL_PATH)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }

    private Bitmap resizeBitmap(Bitmap image , int dimension ) {
        return Bitmap.createScaledBitmap( image , dimension , dimension , true ) ;
    }

    /** Writes Image data into a {@code ByteBuffer}. */
    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }

        bitmap = resizeBitmap(bitmap, 224);
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
            for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
    }

}
