package br.com.focaand.lousa.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.View;

public class ImageFileUtil {

    public static final int MEDIA_TYPE_CAMERA       = 1;
    public static final int MEDIA_TYPE_IMAGE        = 2;
    public static final int MEDIA_TYPE_SEGMENTATION = 3;
    public static final int MEDIA_TYPE_FINAL        = 4;

    /** Create a file Uri for saving an image or video */
    public static Uri getOutputMediaFileUri(int type) {
	return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    public static File getOutputMediaFile(int type) {
	// To be safe, you should check that the SDCard is mounted
	// using Environment.getExternalStorageState() before doing this.

	File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "focaAndLousa");
	// This location works best if you want the created images to be shared
	// between applications and persist after your app has been uninstalled.

	// Create the storage directory if it does not exist
	if (!mediaStorageDir.exists()) {
	    if (!mediaStorageDir.mkdirs()) {
		Log.d("focaAndLousa", "failed to create directory");
		return null;
	    }
	}

	// Create a media file name
	String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	File mediaFile;
	if (type == MEDIA_TYPE_CAMERA) {
	    mediaFile = new File(mediaStorageDir.getPath() + File.separator + "CAM_" + timeStamp + ".png");
	} else if (type == MEDIA_TYPE_IMAGE) {
	    mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".png");
	} else if (type == MEDIA_TYPE_SEGMENTATION) {
	    mediaFile = new File(mediaStorageDir.getPath() + File.separator + "SEG_" + "FOCAAND_TEMP" + ".png");
	} else if (type == MEDIA_TYPE_FINAL) {
	    mediaFile = new File(mediaStorageDir.getPath() + File.separator + "FOCA_" + timeStamp + ".png");
	} else {
	    return null;
	}

	return mediaFile;
    }

    public static boolean saveBitmap(Bitmap bitmap, String fileName) {

	FileOutputStream out = null;
	try {
	    out = new FileOutputStream(fileName);
	    bitmap.compress(Bitmap.CompressFormat.PNG, 80, out);
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	    try {
		if (out != null)
		    out.close();
	    } catch (Throwable ignore) {
	    }
	}

	return true;
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int maxResolution) {
	int newSmalSize = -1;
	int bmpW = bitmap.getWidth();
	int bmpH = bitmap.getHeight();
	boolean landscape = (bmpW >= bmpH);
	if (landscape)
	    newSmalSize = bmpH * maxResolution / bmpW;
	else
	    newSmalSize = bmpW * maxResolution / bmpH;
	Bitmap scaledBitmap = null;
	if (landscape)
	    scaledBitmap = Bitmap.createScaledBitmap(bitmap, maxResolution, newSmalSize, true);
	else
	    scaledBitmap = Bitmap.createScaledBitmap(bitmap, newSmalSize, maxResolution, true);
	return scaledBitmap;
    }

    public static String prepareFile(String fileName) {
	Bitmap originalBitmap = getBitmap(fileName);
	if (originalBitmap.getWidth() <= Preferences.getInstance().getMaxResolution() && originalBitmap.getHeight() <= Preferences.getInstance().getMaxResolution()) {
	    originalBitmap.recycle();
	    originalBitmap = null;
	    System.gc();
	    System.out.println("*focaAndLousa* - No need to resize file: " + fileName);
	    return fileName;
	} else {
	    Bitmap scaledBitmap = scaleBitmap(originalBitmap, Preferences.getInstance().getMaxResolution());
	    String newFileName = ImageFileUtil.getOutputMediaFileUri(MEDIA_TYPE_IMAGE).getPath();
	    saveBitmap(scaledBitmap, newFileName);
	    System.out.println("*focaAndLousa* - File: " + fileName + " resized to [" + scaledBitmap.getWidth()
		    + "x" + scaledBitmap.getHeight() + "] and saved to: "    + newFileName);

	    originalBitmap.recycle();
	    originalBitmap = null;
	    scaledBitmap.recycle();
	    scaledBitmap = null;
	    System.gc(); 
	    return newFileName;
	}
    }

    public static Bitmap getBitmap(String fileName) {
	BitmapFactory.Options options = new BitmapFactory.Options();
	options.inPreferredConfig = Bitmap.Config.ARGB_8888;
	Bitmap bitmap = BitmapFactory.decodeFile(fileName, options);

	// Rotate image if needed
	Matrix matrix = new Matrix();
	int rotation = getImageOrientation(fileName);
	System.out.println(" *focaAndLousa* - Rotating image [" + fileName + "] by " + rotation + " degrees *");
	matrix.postRotate(rotation);
	bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	bitmap.compress(CompressFormat.PNG, 80, out);

	return bitmap;
    }

    /**
     * 
     * @param imagePath
     * @autor http://stackoverflow.com/questions/19511610/camera-intent-auto-rotate-to-90-degree
     * @return orientation angle
     */
    public static int getImageOrientation(String imagePath) {
	int rotate = 0;
	try {

	    File imageFile = new File(imagePath);
	    ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
	    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

	    System.out.println(" *focaAndLousa* Image orientation detected: " + orientation);
	    switch (orientation) {
		case ExifInterface.ORIENTATION_ROTATE_270:
		    rotate = 270;
		    break;
		case ExifInterface.ORIENTATION_ROTATE_180:
		    rotate = 180;
		    break;
		case ExifInterface.ORIENTATION_ROTATE_90:
		    rotate = 90;
		    break;
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return rotate;
    }

    public static Point getProportionalXY(int screenWidth, int screenHeight, int bitmapWidth, int bitmapHeight, int inputX, int inputY) {
	int x = (int)(inputX * bitmapWidth) / screenWidth;
	int y = (int)(inputY * bitmapHeight) / screenHeight;
	return new Point(x, y);
    }

    /**
     * @autor http://stackoverflow.com/questions/7607165/how-to-write-build-time-stamp-into-apk
     */
    public static String getApkBuildTimeStamp(Activity activity) {
	String timeStamp = "";
	try {
	    ApplicationInfo ai = activity.getPackageManager().getApplicationInfo(activity.getPackageName(), 0);
	    ZipFile zf = new ZipFile(ai.sourceDir);
	    ZipEntry ze = zf.getEntry("classes.dex");
	    long time = ze.getTime();
	    timeStamp = SimpleDateFormat.getInstance().format(new java.util.Date(time));

	} catch (Exception e) {
	}
	return timeStamp;
    }
}
