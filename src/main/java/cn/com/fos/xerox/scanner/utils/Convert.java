package cn.com.fos.xerox.scanner.utils;

import gnu.getopt.Getopt;

import static cn.com.fos.xerox.scanner.driver.Scanner.ImageFilterOutputStream.DEFAULT_HEIGHT;
import static cn.com.fos.xerox.scanner.driver.Scanner.ImageFilterOutputStream.DEFAULT_WIDTH;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import cn.com.fos.xerox.scanner.driver.FujiXerox_MFP2;
import cn.com.fos.xerox.scanner.driver.Scanner;
import cn.com.fos.xerox.scanner.driver.Scanner.PackageType;;

public class Convert {
	public static void main(String[] args) throws Throwable {
		Getopt options = new Getopt("Convert", args, "B:hi:o:tV");
		boolean transparent = false, verbose = false;
		short background = 220;
		String input  = null, output = null;
		while (true) {
			switch (options.getopt()) {
			case -1:
				Scanner scanner = new FujiXerox_MFP2();
				scanner.setFile(output);
				scanner.setTransparent(transparent);
				scanner.setVerbose(verbose);
				scanner.setBackground(background);
				scanner.setup(PackageType.get(Scanner.hex2bytes("484403020003020200010001")));
				BufferedImage in = ImageIO.read(new File(input));
				OutputStream out =  Scanner.SettingUp.DATA.getOutputStream(scanner);
				int w = in.getWidth(), h = in.getHeight();
				int shift[] = new int[]{16, 8, 0};
				for (int y = 0; y < DEFAULT_HEIGHT; y++)
					for (int x = 0; x < DEFAULT_WIDTH; x++) {
						int c = 0xffffffff;
						if (x < w && y < h) c = in.getRGB(x, y);
						for (int s : shift) out.write((c >> s) & 0xff); 
					}
				out.close();
				System.exit(0);
			case 'B':
				background = Short.parseShort(options.getOptarg());
				break;
			case 'i':
				input = options.getOptarg();
				break;
			case 'o':
				output = options.getOptarg();
				break;
			case 't':
				transparent = true;
				break;
			case 'V':
				verbose = true;
				break;
			default:
				message("Convert [options]");
				message("  -h           See this help");
				message("  -B           background(default 220)");
				message("  -i filename  The input filename");
				message("  -o filename  The output filename");
				message("  -t           transparency");
				message("  -V           verbose mode");
				System.exit(0);
			}
		}
	}

	public static void message(String message) {
		System.err.println(message);
	}
}
