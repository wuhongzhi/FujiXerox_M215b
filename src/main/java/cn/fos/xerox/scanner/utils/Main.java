package cn.fos.xerox.scanner.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Main {
	public static void main(String[] args) throws Throwable {
		if (args.length == 0 || StringUtils.equals(args[0], "-h")) {
			message("Main [scan|convert] [options]");
		} else if (StringUtils.equals(args[0], "scan")) {
			Scan.main(ArrayUtils.subarray(args, 1, args.length));
		} else if (StringUtils.equals(args[0], "convert")) {
			Convert.main(ArrayUtils.subarray(args, 1, args.length));
		}
	}
	public static void message(String message) {
		System.err.println(message);
	}
}
