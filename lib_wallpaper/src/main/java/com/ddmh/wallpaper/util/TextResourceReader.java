/***
 * Excerpted from "OpenGL ES for Android",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/kbogla for more book information.
***/
package com.ddmh.wallpaper.util;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextResourceReader {
    /**
     * Reads in text from a resource file and returns a String containing the
     * text.
     */
    public static String readTextFileFromResource(Context context,
        int resourceId) {
        StringBuilder body = new StringBuilder();

        try {
            InputStream inputStream = context.getResources()
                .openRawResource(resourceId);
            InputStreamReader inputStreamReader = new InputStreamReader(
                inputStream);
            BufferedReader bufferedReader = new BufferedReader(
                inputStreamReader);

            String nextLine;

            while ((nextLine = bufferedReader.readLine()) != null) {
                body.append(nextLine);
                body.append('\n');
            }
        } catch (IOException e) {
            throw new RuntimeException(
                "Could not open resource: " + resourceId, e);
        } catch (Resources.NotFoundException nfe) {
            throw new RuntimeException("Resource not found: " 
                + resourceId, nfe);
        }

        return body.toString();
    }
    public static String readShaderTextFromAsset(Context context,String fileName) {
        InputStream inputStream = null;
        try {
            // 获取解密后的输入流
             inputStream = context.getAssets().open("glsl/" + fileName);


            // 读取解密后的内容
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }

            return stringBuilder.toString();

        } catch (IOException e) {
            e.printStackTrace();
            return "";
        } finally {
            // 关闭资源
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


}
