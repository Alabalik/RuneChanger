package com.stirante.runechanger.utils;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class SwingUtils {

    /**
     * Returns scaled instance of an image. Uses affine transform.
     * @param width target width
     * @param height target height
     * @param image source image
     * @return scaled image
     */
    public static BufferedImage getScaledImage(int width, int height, BufferedImage image) {
        if (image == null) {
            return null;
        }
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        if (imageWidth == 0 || imageHeight == 0) {
            return null;
        }

        double scaleX = (double) width / imageWidth;
        double scaleY = (double) height / imageHeight;

        if (scaleX == 0 || scaleY == 0) {
            return null;
        }

        AffineTransform scaleTransform = AffineTransform.getScaleInstance(scaleX, scaleY);
        AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform, AffineTransformOp.TYPE_BILINEAR);

        return bilinearScaleOp.filter(
                image,
                new BufferedImage(width, height, image.getType()));
    }

}
