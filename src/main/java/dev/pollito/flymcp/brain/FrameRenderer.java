package dev.pollito.flymcp.brain;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * Renders a question into the frame format the MaleCNS visual adapter expects:
 * a light-background 320x180 RGB PNG. Dark backgrounds barely activate the
 * connectome's Kenyon cells, so the palette stays light on purpose.
 *
 * <p>The renderer is deterministic: the same text produces the same bytes, so
 * consults are reproducible in tests and in mock mode.
 */
public class FrameRenderer {

	public static final int WIDTH = 320;

	public static final int HEIGHT = 180;

	static final Color BACKGROUND = new Color(235, 240, 249);

	static final Color INK = new Color(19, 36, 71);

	static final Color FOOTER_TEXT = new Color(219, 229, 249);

	private static final Font HEADER_FONT = new Font(Font.MONOSPACED, Font.BOLD, 13);

	private static final Font BODY_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 17);

	private static final Font FOOTER_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 10);

	private static final int MARGIN = 12;

	private static final int FOOTER_HEIGHT = 20;

	private static final int LINE_HEIGHT = 21;

	private static final int BODY_TOP = 52;

	public byte[] render(String text) {
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		try {
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setColor(BACKGROUND);
			g.fillRect(0, 0, WIDTH, HEIGHT);
			g.setColor(INK);

			g.setFont(HEADER_FONT);
			g.drawString("FLY EYE // QUESTION", MARGIN, 26);
			g.drawLine(MARGIN, 34, WIDTH - MARGIN, 34);

			g.setFont(BODY_FONT);
			FontMetrics metrics = g.getFontMetrics();
			int maxLines = (HEIGHT - FOOTER_HEIGHT - BODY_TOP - 8) / LINE_HEIGHT;
			List<String> lines = wrap(metrics, text, WIDTH - 2 * MARGIN, maxLines);
			int y = BODY_TOP + metrics.getAscent() - 4;
			for (String line : lines) {
				g.drawString(line, MARGIN, y);
				y += LINE_HEIGHT;
			}

			g.setColor(INK);
			g.fillRect(0, HEIGHT - FOOTER_HEIGHT, WIDTH, FOOTER_HEIGHT);
			g.setColor(FOOTER_TEXT);
			g.setFont(FOOTER_FONT);
			g.drawString("MALECNS v1.0  ENGINEERED READOUT  COMEDY ORACLE", MARGIN, HEIGHT - 7);
		}
		finally {
			g.dispose();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", out);
		}
		catch (IOException ex) {
			throw new UncheckedIOException("PNG encoding failed", ex);
		}
		return out.toByteArray();
	}

	private List<String> wrap(FontMetrics metrics, String text, int maxWidth, int maxLines) {
		List<String> lines = new ArrayList<>();
		if (text == null || text.isBlank() || maxLines <= 0) {
			return lines;
		}
		StringBuilder current = new StringBuilder();
		for (String word : text.strip().split("\\s+")) {
			for (String chunk : hardSplit(metrics, word, maxWidth)) {
				String candidate = current.isEmpty() ? chunk : current + " " + chunk;
				if (metrics.stringWidth(candidate) <= maxWidth) {
					current.setLength(0);
					current.append(candidate);
				}
				else {
					lines.add(current.toString());
					current.setLength(0);
					current.append(chunk);
					if (lines.size() == maxLines) {
						return ellipsize(metrics, lines, maxWidth);
					}
				}
			}
		}
		if (!current.isEmpty() && lines.size() < maxLines) {
			lines.add(current.toString());
		}
		return lines;
	}

	private List<String> hardSplit(FontMetrics metrics, String word, int maxWidth) {
		List<String> chunks = new ArrayList<>();
		String rest = word;
		while (metrics.stringWidth(rest) > maxWidth && rest.length() > 1) {
			int cut = rest.length() - 1;
			while (cut > 1 && metrics.stringWidth(rest.substring(0, cut)) > maxWidth) {
				cut--;
			}
			chunks.add(rest.substring(0, cut));
			rest = rest.substring(cut);
		}
		chunks.add(rest);
		return chunks;
	}

	private List<String> ellipsize(FontMetrics metrics, List<String> lines, int maxWidth) {
		int last = lines.size() - 1;
		String line = lines.get(last);
		while (!line.isEmpty() && metrics.stringWidth(line + " ...") > maxWidth) {
			line = line.substring(0, line.length() - 1);
		}
		lines.set(last, line + " ...");
		return lines;
	}

}
