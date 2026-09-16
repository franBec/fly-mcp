package dev.pollito.flymcp.brain;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class FrameRendererTest {

	private final FrameRenderer renderer = new FrameRenderer();

	@Test
	void rendersPngAtAdapterDimensions() throws Exception {
		byte[] png = this.renderer.render("Should I refactor the build?");

		assertThat(png[0]).isEqualTo((byte) 0x89);
		assertThat(png[1]).isEqualTo((byte) 'P');
		BufferedImage image = read(png);
		assertThat(image.getWidth()).isEqualTo(FrameRenderer.WIDTH);
		assertThat(image.getHeight()).isEqualTo(FrameRenderer.HEIGHT);
	}

	@Test
	void keepsTheBackgroundLight() throws Exception {
		BufferedImage image = read(this.renderer.render("light"));

		int rgb = image.getRGB(2, 2);
		assertThat((rgb >> 16) & 0xFF).isGreaterThan(200);
		assertThat((rgb >> 8) & 0xFF).isGreaterThan(200);
		assertThat(rgb & 0xFF).isGreaterThan(200);
	}

	@Test
	void isDeterministic() {
		assertThat(this.renderer.render("same question")).isEqualTo(this.renderer.render("same question"));
	}

	@Test
	void differentTextProducesDifferentFrames() {
		assertThat(this.renderer.render("one")).isNotEqualTo(this.renderer.render("two"));
	}

	@Test
	void longTextIsTruncatedInsteadOfOverflowing() throws Exception {
		byte[] png = this.renderer.render("word ".repeat(500));

		assertThat(read(png).getHeight()).isEqualTo(FrameRenderer.HEIGHT);
	}

	@Test
	void blankTextStillRenders() {
		assertThat(this.renderer.render("   ")).isNotEmpty();
		assertThat(this.renderer.render(null)).isNotEmpty();
	}

	private BufferedImage read(byte[] png) throws Exception {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
		assertThat(image).isNotNull();
		return image;
	}

}
