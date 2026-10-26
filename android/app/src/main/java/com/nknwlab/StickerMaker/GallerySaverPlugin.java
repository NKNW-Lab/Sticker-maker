package com.nknwlab.StickerMaker;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

@CapacitorPlugin(name = "GallerySaver")
public class GallerySaverPlugin extends Plugin {

    @PluginMethod
    public void saveToGallery(PluginCall call) {
        String base64Data = call.getString("base64");
        String fileName = call.getString("fileName", "Sticker_A4_" + System.currentTimeMillis() + ".png");

        if (base64Data == null || base64Data.isEmpty()) {
            call.reject("Base64 string is required");
            return;
        }

        if (base64Data.contains(",")) {
            base64Data = base64Data.split(",")[1];
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
        } catch (Exception e) {
            call.reject("Failed to decode base64 image", e);
            return;
        }

        Context context = getContext();
        try {
            String savedPath = saveImageToPictures(context, imageBytes, fileName);
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("path", savedPath);
            call.resolve(result);
        } catch (Exception e) {
            call.reject("Error saving image to gallery: " + e.getMessage(), e);
        }
    }

    private String saveImageToPictures(Context context, byte[] imageBytes, String fileName) throws IOException {
        if (!fileName.endsWith(".png")) {
            fileName += ".png";
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Stickers");
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (imageUri == null) {
                throw new IOException("Failed to create MediaStore entry.");
            }

            try (OutputStream fos = resolver.openOutputStream(imageUri)) {
                if (fos == null) {
                    throw new IOException("Failed to open output stream.");
                }
                fos.write(imageBytes);
            }

            contentValues.clear();
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
            resolver.update(imageUri, contentValues, null, null);

            return imageUri.toString();
        } else {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File stickersDir = new File(picturesDir, "Stickers");
            if (!stickersDir.exists()) {
                stickersDir.mkdirs();
            }
            File imageFile = new File(stickersDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageBytes);
            }

            MediaScannerConnection.scanFile(
                    context,
                    new String[]{imageFile.getAbsolutePath()},
                    new String[]{"image/png"},
                    null
            );

            return imageFile.getAbsolutePath();
        }
    }
}
