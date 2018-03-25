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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.imageio.ImageIO;

public class MakeFont{
	
	public static final class FontPart
	{
		public final int fontType;
		public final String pngFile;
		public final int charSize;
		public final int beginCodepoint;
		public final int endCodepoint;
		public final int charsPerLine;
		public FontPart(int fontType, String pngFile, int charSize, int beginCodepoint, int endCodepoint,
				int charsPerLine) {
			this.fontType = fontType;
			this.pngFile = pngFile;
			this.charSize = charSize;
			this.beginCodepoint = beginCodepoint;
			this.endCodepoint = endCodepoint;
			this.charsPerLine = charsPerLine;
		}
	}
	
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
	
	public static final void saveStr(boolean blend, String propertiesFile, String fontName, FontPart part) throws IOException {
		saveStr(blend, propertiesFile, i -> fontName, null, part);
	}
	
	private static final class FontMap
	{
		private final Map<String, Font> fonts = new HashMap<>();
		
		private final Function<Integer, String> fontSelector;
		
		private final int charSize;
		
		private final int fontType;

		public FontMap(Function<Integer, String> fontSelector, int charSize, int fontType) {
			this.fontSelector = fontSelector;
			this.charSize = charSize;
			this.fontType = fontType;
		}
		
		public Font get(int codepoint)
		{
			return this.fonts.computeIfAbsent(fontSelector.apply(codepoint), font -> new Font(font, fontType, charSize));
		}
		
	}
	
	public static final void saveStr(boolean blend, String propertiesFile, Function<Integer, String> fontSelector, Consumer<Properties> pConsumer, FontPart... parts) throws IOException {
		final Properties props = new Properties();
		props.setProperty("version", "2");
		props.setProperty("blending", blend ? "true" : "false");
		props.setProperty("parts", String.valueOf(parts.length));
		
		int partNo = 0;
		int maxChars = 0;
		for (final FontPart part : parts)
		{
			System.out.println("Creating part #"+partNo);
			
			final int countChars = part.endCodepoint - part.beginCodepoint + 1;
			final int countCols = part.charsPerLine;
			final int countRows = (int) Math.ceil((double)countChars/(double)part.charsPerLine);
			final int[][] text = new int[countRows][];
			for (int row = 0; row < countRows; row++)
			{
				text[row] = new int[countCols];
				for (int col = 0; col < countCols; col++)
				{
					final int cp = row*countCols + col + part.beginCodepoint;
					text[row][col] = cp > part.endCodepoint ? ' ' : cp;
				}
			}
			
		    final FontMap fontMapNormal = new FontMap(fontSelector, part.charSize, part.fontType);
		    final FontMap fontMapHalf = new FontMap(fontSelector, part.charSize/2, part.fontType);

			final int width = getCharWidth('0', fontMapNormal.get('0'));
			final int height = getCharHeight('0', fontMapNormal.get('0'));
			int count_zero = 0;
			int count_normal = 0;
			int count_greater = 0;
			int count_invalid = 0;

			final BufferedImage img = new BufferedImage((width + 2) * (countCols + 1), (height + 2) * (countRows + 1), BufferedImage.TYPE_INT_ARGB);
			final Graphics2D g2d = img.createGraphics();
			if (blend)
			{
				g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			}
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
			g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2d.setColor(Color.WHITE);
			for (int row = 0; row < text.length; row++)
			{
				System.out.println("Writing line " + row + " of " + part.pngFile);
				for (int col = 0; col < text[row].length; col++)
				{
					int codepoint = text[row][col];
					if (maxChars < codepoint) maxChars = codepoint;
					final Font fontNormal = fontMapNormal.get(codepoint);
					final Font fontHalf = fontMapHalf.get(codepoint);
					final FontMetrics fmHalf = g2d.getFontMetrics(fontHalf);
					final FontMetrics fmNormal = g2d.getFontMetrics(fontNormal);
					 // whitespace fix for \t (tab) etc.
					String str = Character.isWhitespace(codepoint) ? " " : new StringBuilder().appendCodePoint(codepoint).toString();
					int cwidth = fmNormal.stringWidth(str);
					if (cwidth == 0)
					{
						System.out.println("Invalid char (zero size) " + codepoint + "@" + cwidth + "px");
						str = "?";
						cwidth = fmNormal.stringWidth(str);
						count_zero++;

						// draw normal
						g2d.setFont(fontNormal);
						g2d.drawString(str, 1 + col*(width + 2) + ((width-cwidth)/2), fmNormal.getAscent() + (row*(height + 2)));
					}
					if (cwidth > width)
					{
						cwidth = fmHalf.stringWidth(str);
						if (cwidth > width)
						{
							System.out.println("Invalid char (too big) " + codepoint + "@half:" + cwidth + "px");
							str = "?";
							cwidth = fmNormal.stringWidth(str);
							count_invalid++;

							// draw normal
							g2d.setFont(fontNormal);
							g2d.drawString(str, 1 + col*(width + 2) + ((width-cwidth)/2), fmNormal.getAscent() + (row*(height + 2)));
						}
						else
						{
							int cheight = fmHalf.getHeight();
							g2d.setFont(fontHalf);
							count_greater++;
							
							// draw half
							g2d.drawString(str, 1 + col*(width + 2) + ((width-cwidth)/2), ((height-cheight)/2) + fmHalf.getAscent() + (row*(height + 2)));
						}
					}
					else
					{
						count_normal++;

						// draw normal
						g2d.setFont(fontNormal);
						g2d.drawString(str, 1 + col*(width + 2) + ((width-cwidth)/2), fmNormal.getAscent() + (row*(height + 2)));
					}
				}
			}
			System.out.println("characters: invalid=" + count_invalid + " / zero=" + count_zero + " / double=" + count_greater + " / normal=" + count_normal);
			//System.out.println("max advance: " + fmNormal.getMaxAdvance());
			g2d.dispose();
			try {
				ImageIO.write(img, "png", new File(part.pngFile));
			} catch (IOException ex) {
				ex.printStackTrace();
			}

			props.setProperty("fontHeight_"+partNo, String.valueOf(height));
			props.setProperty("fontWidth_"+partNo, String.valueOf(width));
			props.setProperty("charsPerLine_"+partNo, String.valueOf(part.charsPerLine));
			props.setProperty("texHeight_"+partNo, String.valueOf(img.getHeight()));
			props.setProperty("texWidth_"+partNo, String.valueOf(img.getWidth()));
			props.setProperty("texture_"+partNo, part.pngFile);
			props.setProperty("start_"+partNo, String.valueOf(part.beginCodepoint));
			props.setProperty("end_"+partNo, String.valueOf(part.endCodepoint));
			
			partNo++;
		}
		
		props.setProperty("maxChars", String.valueOf(maxChars));
		
		if (pConsumer != null)
		{
			pConsumer.accept(props);
		}
		try {
			final FileOutputStream fos = new FileOutputStream(new File(propertiesFile));
			props.store(fos, "");
			fos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws FontFormatException, IOException {
		if (args[0].equals("$UNIFONT"))
		{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			
			final Font font1 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont-10.0.07.ttf"));
		    ge.registerFont(font1);
		    
		    final Font font2 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont_csur-10.0.07.ttf"));
		    ge.registerFont(font2);
		    
		    final Font font3 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont_upper-10.0.07.ttf"));
		    ge.registerFont(font3);
		    
		    final int[][] lines = new int[512][];
			for (int row = 0; row < 512; row++)
			{
				lines[row] = new int[256];
				for (int col = 0; col < 256; col++)
				{
					lines[row][col] = row*256 + col;
				}
			}
			
		    saveStr(
		    		false,
		    		"unifont.properties",
		    		i -> {
		    			if (i >= 0xE000 && i <= 0xF8FF) return "Unifont CSUR";
		    			if (i >= 0xF0000 && i <= 0x10FFFF) return "Unifont CSUR"; // not yet used, may need additional setup
		    			if (i >= 0x10000 && i <= 0x1FFFF) return "Unifont Upper";
		    			if (Character.isBmpCodePoint(i)) return "Unifont";
		    			// fallback to unifont
		    			return "Unifont";
		    		},
		    		props -> {},
		    		new FontPart(Font.PLAIN, "unifont.png", 20, 0, 0x1FFFF, 256));
		    return;
		}
		
		if (args[0].equals("$UNIFONT-BLEND"))
		{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			
			final Font font1 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont-10.0.07.ttf"));
		    ge.registerFont(font1);
		    
		    final Font font2 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont_csur-10.0.07.ttf"));
		    ge.registerFont(font2);
		    
		    final Font font3 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont_upper-10.0.07.ttf"));
		    ge.registerFont(font3);
		    
		    final int[][] lines = new int[512][];
			for (int row = 0; row < 512; row++)
			{
				lines[row] = new int[256];
				for (int col = 0; col < 256; col++)
				{
					lines[row][col] = row*256 + col;
				}
			}
			
		    saveStr(
		    		true,
		    		"unifont.properties",
		    		i -> {
		    			if (i >= 0xE000 && i <= 0xF8FF) return "Unifont CSUR";
		    			if (i >= 0xF0000 && i <= 0x10FFFF) return "Unifont CSUR"; // not yet used, may need additional setup
		    			if (i >= 0x10000 && i <= 0x1FFFF) return "Unifont Upper";
		    			if (Character.isBmpCodePoint(i)) return "Unifont";
		    			// fallback to unifont
		    			return "Unifont";
		    		},
		    		props -> {},
		    		new FontPart(Font.PLAIN, "unifont.png", 20, 0, 0x1FFFF, 256));
		    return;
		}
		
		if (args[0].equals("$UNIFONT-HD"))
		{
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			
			final Font font1 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont-10.0.07.ttf"));
		    ge.registerFont(font1);
		    
		    final Font font2 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont_csur-10.0.07.ttf"));
		    ge.registerFont(font2);
		    
		    final Font font3 = Font.createFont(Font.TRUETYPE_FONT, new File("unifont_upper-10.0.07.ttf"));
		    ge.registerFont(font3);
		    
		    final int[][] lines = new int[512][];
			for (int row = 0; row < 512; row++)
			{
				lines[row] = new int[256];
				for (int col = 0; col < 256; col++)
				{
					lines[row][col] = row*256 + col;
				}
			}
			
		    saveStr(
		    		true,
		    		"unifont.properties",
		    		i -> {
		    			if (i >= 0xE000 && i <= 0xF8FF) return "Unifont CSUR";
		    			if (i >= 0xF0000 && i <= 0x10FFFF) return "Unifont CSUR"; // not yet used, may need additional setup
		    			if (i >= 0x10000 && i <= 0x1FFFF) return "Unifont Upper";
		    			if (Character.isBmpCodePoint(i)) return "Unifont";
		    			// fallback to unifont
		    			return "Unifont";
		    		},
		    		props -> {},
		    		new FontPart(Font.PLAIN, "unifont-1.png", 80, 0x00000, 0x07FFF, 256),
		    		new FontPart(Font.PLAIN, "unifont-2.png", 80, 0x08000, 0x0FFFF, 256),
		    		new FontPart(Font.PLAIN, "unifont-3.png", 80, 0x10000, 0x17FFF, 256),
		    		new FontPart(Font.PLAIN, "unifont-4.png", 80, 0x18000, 0x1FFFF, 256)
		    		);
		    return;
		}
		
		{
			final Font font1 = Font.createFont(Font.TRUETYPE_FONT, new File(args[0]));
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		    ge.registerFont(font1);
		}
		final int[][] lines = new int[256][];
		for (int row = 0; row < 256; row++)
		{
			lines[row] = new int[256];
			for (int col = 0; col < 256; col++)
			{
				lines[row][col] = row*256 + col;
			}
		}
		
	    saveStr(
	    		false,
	    		args[4] + "/" + args[5] + ".properties",
	    		args[1],
	    		new FontPart(
	    				Integer.parseInt(args[2]),
	    				args[4] + "/" + args[5] + ".png",
	    				Integer.parseInt(args[3]),
	    				0, 65535,
	    				256
	    		));
	}

}
