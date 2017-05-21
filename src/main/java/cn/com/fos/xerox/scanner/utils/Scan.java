package cn.com.fos.xerox.scanner.utils;

import cn.com.fos.xerox.scanner.driver.FujiXerox_MFP1;
import cn.com.fos.xerox.scanner.driver.FujiXerox_MFP2;
import cn.com.fos.xerox.scanner.driver.Scanner;
import gnu.getopt.Getopt;

public class Scan {
	public static void main(String[] args) throws Throwable {
		Getopt options = new Getopt("Scan", args, "bB:c:fho:p:rtxv:V");
		boolean fast = false, bar = false, raw = false, term = false, transparent = false, verbose = false;
		short vendor = 0x0550, product = 0x0165, background = 220;
		String convert = null, file = null;
		while (true) {
			switch (options.getopt()) {
			case -1:
				Scanner scanner = fast ? new FujiXerox_MFP2() : new FujiXerox_MFP1();
				scanner.setVendorId(vendor);
				scanner.setProductId(product);
				scanner.setBar(bar);
				scanner.setRaw(raw);
				scanner.setTerm(term);
				scanner.setConvert(convert);
				scanner.setFile(file);
				scanner.setTransparent(transparent);
				scanner.setVerbose(verbose);
				scanner.setBackground(background);
				scanner.scan();
				return;
			case 'b':
				bar = true;
				break;
			case 'B':
				background = Short.parseShort(options.getOptarg());
				break;
			case 'c':
				convert = options.getOptarg();
				break;
			case 'f':
				fast = true;
				break;
			case 'o':
				file = options.getOptarg();
				break;
			case 'p':
				product = Short.parseShort(options.getOptarg(), 16);
				break;
			case 'r':
				raw = true;
				break;
			case 't':
				transparent = true;
				break;
			case 'x':
				term = true;
				break;
			case 'v':
				vendor = Short.parseShort(options.getOptarg(), 16);
				break;
			case 'V':
				verbose = true;
				break;
			default:
				message("Scan [options]");
				message("  -h           See this help");
				message("  -b           Show progress bar");
				message("  -B           background(default 220)");
				message("  -c command   Image convert path");
				message("  -f           fast scanning");
				message("  -r           rgb file with(2496x3507+8)");
				message("  -o filename  The output filename");
				message("  -p hexid     Product ID in hex (default 0x0165)");
				message("  -t           transparency");
				message("  -x           term progress");
				message("  -v           Vendor ID in hex (default 0x0550)");
				message("  -V           verbose mode");
				System.exit(0);
			}
		}
	}

	public static void message(String message) {
		System.err.println(message);
	}
}
