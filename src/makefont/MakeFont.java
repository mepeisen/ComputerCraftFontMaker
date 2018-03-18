package makefont;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.imageio.ImageIO;

public class MakeFont{
	
	public static int getCharWidth(char c, Font font1)
	{
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		g2d.setFont(font1);
		FontMetrics fm = g2d.getFontMetrics();
		int width = fm.stringWidth(Character.toString(c));
		g2d.dispose();
		return width;
	}
	
	public static int getCharHeight(char c, Font font1)
	{
		BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = img.createGraphics();
		g2d.setFont(font1);
		FontMetrics fm = g2d.getFontMetrics();
		int height = fm.getHeight();
		g2d.dispose();
		return height;
	}
	
	public static final void saveStr(char[][] text, int fontType, String pngFile, String propertiesFile, int charSize, String fontName) throws IOException {
	    final Font fontNormal = new Font(fontName, fontType, charSize);
	    final Font fontHalf = new Font(fontName, fontType, charSize/2);

		final int width = getCharWidth('0', fontNormal);
		final int height = getCharHeight('0', fontNormal);
		int count_zero = 0;
		int count_normal = 0;
		int count_greater = 0;
		int count_invalid = 0;

		final BufferedImage img = new BufferedImage((width + 2) * 257, (height + 2) * 257, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g2d = img.createGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2d.setFont(fontNormal);
		final FontMetrics fmNormal = g2d.getFontMetrics(fontNormal);
		final FontMetrics fmHalf = g2d.getFontMetrics(fontHalf);
		g2d.setColor(Color.WHITE);
		for (int row = 0; row < text.length; row++)
		{
			System.out.println("Writing line " + row + " of " + pngFile);
			for (int col = 0; col < text[row].length; col++)
			{
				char c = text[row][col];
				if (Character.isWhitespace(c)) c = ' '; // fix for \t (tab) etc.
				int cwidth = fmNormal.stringWidth(""+c);
				if (cwidth == 0)
				{
					System.out.println("Invalid char (zero size) " + (int)c + "@" + cwidth + "px");
					c = '?';
					cwidth = fmNormal.stringWidth(""+c);
					count_zero++;

					// draw normal
					g2d.drawString(""+c, 1 + col*(width + 2) + ((width-cwidth)/2), fmNormal.getAscent() + (row*(height + 2)));
				}
				if (cwidth > width)
				{
					cwidth = fmHalf.stringWidth(""+c);
					if (cwidth > width)
					{
						System.out.println("Invalid char (too big) " + (int)c + "@half:" + cwidth + "px");
						c = '?';
						cwidth = fmNormal.stringWidth(""+c);
						count_invalid++;

						// draw normal
						g2d.drawString(""+c, 1 + col*(width + 2) + ((width-cwidth)/2), fmNormal.getAscent() + (row*(height + 2)));
					}
					else
					{
						int cheight = fmHalf.getHeight();
						g2d.setFont(fontHalf);
						count_greater++;
						
						// draw half
						g2d.drawString(""+c, 1 + col*(width + 2) + ((width-cwidth)/2), ((height-cheight)/2) + fmNormal.getAscent() + (row*(height + 2)));
						g2d.setFont(fontNormal);
					}
				}
				else
				{
					count_normal++;

					// draw normal
					g2d.drawString(""+c, 1 + col*(width + 2) + ((width-cwidth)/2), fmNormal.getAscent() + (row*(height + 2)));
				}
			}
		}
		System.out.println("characters: invalid=" + count_invalid + " / zero=" + count_zero + " / double=" + count_greater + " / normal=" + count_normal);
		System.out.println("max advance: " + fmNormal.getMaxAdvance());
		g2d.dispose();
		try {
			ImageIO.write(img, "png", new File(pngFile));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		
		final Properties props = new Properties();
		props.setProperty("fontHeight", String.valueOf(height));
		props.setProperty("fontWidth", String.valueOf(width));
		props.setProperty("maxChars", "65536");
		props.setProperty("charsPerLine", "256");
		props.setProperty("texHeight", String.valueOf(img.getHeight()));
		props.setProperty("texWidth", String.valueOf(img.getWidth()));
		try {
			final FileOutputStream fos = new FileOutputStream(new File(propertiesFile));
			props.store(fos, "");
			fos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws FontFormatException, IOException {
		{
			final Font font1 = Font.createFont(Font.TRUETYPE_FONT, new File(args[0]));
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		    ge.registerFont(font1);
		}
		final char[][] lines = new char[256][];
		for (int row = 0; row < 256; row++)
		{
			lines[row] = new char[256];
			for (int col = 0; col < 256; col++)
			{
				lines[row][col] = (char) (row*256 + col);
			}
		}
		
	    saveStr(
	    		lines,
	    		Integer.parseInt(args[2]),
	    		args[4] + "/" + args[5] + ".png",
	    		args[4] + "/" + args[5] + ".properties",
	    		Integer.parseInt(args[3]),
	    		args[1]);
	}

}
