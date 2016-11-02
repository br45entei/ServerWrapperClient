package com.gmail.br45entei.io;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/** Convert from SWT &lt;--&gt; AWT images
 * 
 * @author Brian_Entei */
public class ImageUtil {
	
	/** Converts an AWT image to SWT.
	 *
	 * @param image the image (<code>null</code> not permitted).
	 *
	 * @return Image data. */
	public static ImageData convertAWTImageToSWT(Image image) {
		if(image == null) {
			throw new IllegalArgumentException("Null 'image' argument.");
		}
		int w = image.getWidth(null);
		int h = image.getHeight(null);
		if(w == -1 || h == -1) {
			return null;
		}
		BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		Graphics g = bi.getGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return convertToSWT(bi);
	}
	
	/** @param data The data to convert
	 * @return The converted BufferedImage. */
	public static final BufferedImage convertToAWT(ImageData data) {
		ColorModel colorModel = null;
		PaletteData palette = data.palette;
		if(palette.isDirect) {
			BufferedImage bufferedImage = new BufferedImage(data.width, data.height, BufferedImage.TYPE_INT_ARGB);
			for(int y = 0; y < data.height; y++) {
				for(int x = 0; x < data.width; x++) {
					int pixel = data.getPixel(x, y);
					RGB rgb = palette.getRGB(pixel);
					bufferedImage.setRGB(x, y, data.getAlpha(x, y) << 24 | rgb.red << 16 | rgb.green << 8 | rgb.blue);
				}
			}
			return bufferedImage;
		}
		RGB[] rgbs = palette.getRGBs();
		byte[] red = new byte[rgbs.length];
		byte[] green = new byte[rgbs.length];
		byte[] blue = new byte[rgbs.length];
		for(int i = 0; i < rgbs.length; i++) {
			RGB rgb = rgbs[i];
			red[i] = (byte) rgb.red;
			green[i] = (byte) rgb.green;
			blue[i] = (byte) rgb.blue;
		}
		if(data.transparentPixel != -1) {
			colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue, data.transparentPixel);
		} else {
			colorModel = new IndexColorModel(data.depth, rgbs.length, red, green, blue);
		}
		BufferedImage bufferedImage = new BufferedImage(colorModel, colorModel.createCompatibleWritableRaster(data.width, data.height), false, null);
		WritableRaster raster = bufferedImage.getRaster();
		int[] pixelArray = new int[1];
		for(int y = 0; y < data.height; y++) {
			for(int x = 0; x < data.width; x++) {
				int pixel = data.getPixel(x, y);
				pixelArray[0] = pixel;
				raster.setPixel(x, y, pixelArray);
			}
		}
		return bufferedImage;
	}
	
	/** Convert between SWT Image and AWT BufferedImage.
	 * <p>
	 * For a list of all SWT example snippets see <a href=
	 * "http://www.eclipse.org/swt/snippets/">http://www.eclipse.org/swt/snippets/</a>.
	 * This was snippet 156.
	 * 
	 * @param bufferedImage the buffered image (<code>null</code> not
	 *            permitted).
	 * @return The image data. */
	public static ImageData convertToSWT(BufferedImage bufferedImage) {
		if(bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(), colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			for(int y = 0; y < data.height; y++) {
				for(int x = 0; x < data.width; x++) {
					int rgb = bufferedImage.getRGB(x, y);
					int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
					data.setPixel(x, y, pixel);
					if(colorModel.hasAlpha()) {
						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
					}
				}
			}
			return data;
		} else if(bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for(int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for(int y = 0; y < data.height; y++) {
				for(int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		} else if(bufferedImage.getColorModel() instanceof ComponentColorModel) {
			ComponentColorModel colorModel = (ComponentColorModel) bufferedImage.getColorModel();
			//ASSUMES: 3 BYTE BGR IMAGE TYPE
			PaletteData palette = new PaletteData(0x0000FF, 0x00FF00, 0xFF0000);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(), colorModel.getPixelSize(), palette);
			//This is valid because we are using a 3-byte Data model with no transparent pixels
			data.transparentPixel = -1;
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[3];
			for(int y = 0; y < data.height; y++) {
				for(int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					int pixel = palette.getPixel(new RGB(pixelArray[0], pixelArray[1], pixelArray[2]));
					data.setPixel(x, y, pixel);
				}
			}
			return data;
		}
		return null;
	}
	
}
