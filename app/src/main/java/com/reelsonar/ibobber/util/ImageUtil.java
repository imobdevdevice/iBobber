package com.reelsonar.ibobber.util;

import android.media.ExifInterface;
import java.io.File;

/**
 * Created by markc on 6/23/16.
 */
public class ImageUtil {

    static public int getCameraPhotoOrientation( String imagePath ){
        int rotate = 0;
        try {

            File imageFile = new File(imagePath);

            ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

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


        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }
}
