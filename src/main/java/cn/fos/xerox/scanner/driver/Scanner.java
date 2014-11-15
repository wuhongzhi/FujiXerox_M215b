package cn.fos.xerox.scanner.driver;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageIO;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

public abstract class Scanner {
	public enum Command {
		END(hex2bytes("55000000")), GO(hex2bytes("49000000")), READY(hex2bytes("4c010000"));
		private byte[] val;

		Command(byte[] val) {
			this.val = val;
		}

		/**
		 * get status
		 * 
		 * @return byte[]
		 */
		public byte[] get() {
			return this.val;
		}
	}
	public enum Format {
		JPEG, PDF, PNG, RGB, TIFF;
		public String toString() {
			return this.name().toLowerCase();
		}
	}
	public static class ImageFilterOutputStream extends FilterOutputStream {
		public static final int DEFAULT_HEIGHT = 3507;
		public static final int DEFAULT_WIDTH = 2496;
		public static final int DEFAULT_DPI = 300;
		int counter = 0, x = 0, y = 0, w = DEFAULT_WIDTH, h = DEFAULT_HEIGHT;
		int ignored_px_less = 10;
		BufferedImage image;
		int[] rgb = new int[3];
		Scanner scanner;
		public ImageFilterOutputStream(OutputStream out, Scanner scanner) {
			super(out);
			this.scanner = scanner;
			this.w = scanner.paper.get().width;
			this.h = scanner.paper.get().height;
			this.image = new BufferedImage(w, h, scanner.image_type);
		}
		public void close() throws IOException {
			int left = scanner.viewport.x,
					right = scanner.viewport.width + left,
					top = scanner.viewport.y,
					bottom = scanner.viewport.height + top;
			if (scanner.isExtremely()) {
				scanner.verbose("extremely clariting ...");
				for (int y = top; y <= bottom; y++) {
					for(int x = left; x <= right; x++){
						int c = image.getRGB(x, y);
						if (!filter(c)) image.setRGB(x, y, c & 0x00ffffff);
					}
				}
			} else if (scanner.isTransparent()) {
				scanner.verbose("well clariting ...");
				scanner.verbose("  scan x");
				//scan x axis
				for (int y = top; y <= bottom; y++) {
					int x1 = left;
					for (; x1 <= right; x1++) {
						int c = image.getRGB(x1, y);
						if (filter(c)) break;
						image.setRGB(x1, y, c & 0x00ffffff);
					}
					int x2 = right;
					for (;x2 >= x1; x2--) {
						int c = image.getRGB(x2, y);
						if (filter(c)) break;
						image.setRGB(x2, y, c & 0x00ffffff);
					}
					if (x2 - x1 < ignored_px_less)
						for (int x = x1; x <= x2; x++) image.setRGB(x, y, 0);	
				}
				scanner.verbose("  scan y");
				//scan y axis
				for (int x = left; x <= right; x++) {
					int y1 = top;
					for (; y1 <= bottom; y1++) {
						int c = image.getRGB(x, y1);
						if (filter(c)) break;
						image.setRGB(x, y1, c & 0x00ffffff);
					}
					int y2 = bottom;
					for (;y2 >= y1; y2--) {
						int c = image.getRGB(x, y2);
						if (filter(c)) break;
						image.setRGB(x, y2, c & 0x00ffffff);
					}
					if (y2 - y1 < ignored_px_less)
						for (int y = y1; y <= y2; y++) image.setRGB(x, y, 0);	
				}
				scanner.verbose("  scan x axis noise");
				//scan space horizon
				for (int y = top; y <= bottom; y++) {
					for(int x = left; x <= right; x++){
						if ((image.getRGB(x, y) & 0xff000000) != 0) {
							int x1 = x;
							for (; x <= right; x++) {
								int c = image.getRGB(x, y);
								if (filter(c)) x1 = x;
								if ((c & 0xff000000) == 0) break;
							}
							if (x <= right) {
								for (; x1 < x; x1++) {
									image.setRGB(x1, y, image.getRGB(x1, y) & 0x00ffffff);
								}
								for (; x <= right; x++) {
									int c = image.getRGB(x, y);
									if (filter(c)) break;
									image.setRGB(x, y, c & 0x00ffffff);
								}
							}
						}
					}
				}
				scanner.verbose("  scan y axis noise");
				//scan space vertical
				for (int x = left; x <= right; x++) {
					for(int y = top; y <= bottom; y++){
						if ((image.getRGB(x, y) & 0xff000000) != 0) {
							int y1 = y++;
							for (; y <= bottom; y++) {
								int c = image.getRGB(x, y);
								if (filter(c)) y1 = y;
								if ((c & 0xff000000) == 0) break;
							}
							if (y <= bottom) {
								for (; y1 < y; y1++) {
									image.setRGB(x, y1, image.getRGB(x, y1) & 0x00ffffff);
								}
								for (; y <= bottom; y++) {
									int c = image.getRGB(x, y);
									if (filter(c)) break;
									image.setRGB(x, y, c & 0x00ffffff);
								}
							}
						}
					}
				}				
			}
			//scale image to target dpi
			if (scanner.dpi != DEFAULT_DPI) {
				scanner.verbose("scale ...");
				int nw = scanner.dpi * w / DEFAULT_DPI;
				int nh = scanner.dpi * h / DEFAULT_DPI;
				BufferedImage nimage = new BufferedImage(nw, nh, scanner.image_type);
				Graphics2D g2d = (Graphics2D) nimage.getGraphics();
				g2d.drawImage(image, 0, 0, nw, nh, null);
				image = nimage;
			}
			scanner.verbose("saving ...");
			//save image to file
			ImageIO.write(image, scanner.format.toString(), this.out);
			super.close();
			scanner.verbose("finish.");
		}
		boolean filter(int c) {
			return (c & 0xff000000) != 0 
					&& (((c >> 0x16) & 0xff) < scanner.getBackground() 
					|| ((c >> 0x08) & 0xff) < scanner.getBackground()
					|| ((c >> 0x00) & 0xff) < scanner.getBackground());
		}
		public void write(int b) throws IOException {
			rgb[counter] = b & 0xff;
			if (++counter == 3) {
				int c = 0xffffffff;
				if (scanner.viewport.contains(x, y) && !scanner.middle.contains(x, y)) {
					c = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2] | 0xff000000;
				} else if (scanner.transparent || scanner.extremely) {
					c = 0x00ffffff;
				}
				if (x < w && y < h) image.setRGB(x, y, c);
				if (++x >= DEFAULT_WIDTH) {
					x = 0;
					y++;
				}
				counter = 0;
			}
		}
	}

	public enum PackageType {
		ACK(hex2bytes("5300000000000000")), 
		DATA(null), 
		INPROGRESS(hex2bytes("4400400000000000")), 
		LAST(hex2bytes("4400350000000000")), 
		NEXT(hex2bytes("4400140000000000"));
		/**
		 * get package type
		 * 
		 * @param arg
		 * @return
		 */
		public static PackageType get(byte[] arg) {
			for (PackageType p : PackageType.values()) {
				if (Arrays.equals(p.val, arg))
					return p;
			}
			DATA.val = arg;
			return DATA;
		}

		private byte[] val;

		PackageType(byte[] val) {
			this.val = val;
		}

		/**
		 * get package value
		 * 
		 * @return
		 */
		public byte[] get() {
			return this.val;
		}
	}

	public enum PaperType {
		A4(0), A5(1), B5(2), Executive(6), Letter(4);
		public static PaperType get(int i) {
			for (PaperType p : PaperType.values())
				if (p.v == i)
					return p;
			return null;
		}

		int v;
		PaperType(int v) {
			this.v = v;
		}

		public Dimension get() {
			switch (this.v) {
			case 0:
				return new Dimension(mm2px(210), mm2px(297));
			case 1:
				return new Dimension(mm2px(148), mm2px(210));
			case 2:
				return new Dimension(mm2px(176), mm2px(250));
			case 4:
				return new Dimension(mm2px(216), mm2px(279));
			default:
				return new Dimension(mm2px(216), mm2px(356));
			}
		}
	}

	public enum SettingUp {
		DATA;
		private Calendar calendar = Calendar.getInstance();
		private byte[] PREFIX = hex2bytes("000000000000000000000000de07");
		private byte[] SUFFIX = hex2bytes("e00134210000aa2d000081ffffff"
				+ "7f0000000100000081ffffff7f000000010000000000000007"
				+ "000000ec1300004ffcffffe803000004000000180000000000"
				+ "0000000000002c0100002c0100000000000000000000c00900"
				+ "00b40d00000000000000000000000000000000000000000000"
				+ "00000000000000000100000000000000010000000000000000" 
				+ "000000");
		private int year, month, date, hours, mintes, seconds;

		/**
		 * get setting
		 * 
		 * @return
		 * @throws IOException
		 */
		public byte[] get() throws IOException {
			calendar.setTime(new Date());
			ByteArrayOutputStream bos = new ByteArrayOutputStream(PREFIX.length + SUFFIX.length + 4);
			bos.write(PREFIX);
			year = calendar.get(Calendar.YEAR);
			seconds = calendar.get(Calendar.SECOND);
			bos.write(month = calendar.get(Calendar.MONTH) + 1);
			bos.write(date = calendar.get(Calendar.DATE));
			bos.write(hours = calendar.get(Calendar.HOUR_OF_DAY));
			bos.write(mintes = calendar.get(Calendar.MINUTE));
			bos.write(SUFFIX);
			return bos.toByteArray();
		}

		/**
		 * get output stream
		 * 
		 * @param format
		 *            PackageType
		 * @return OutputStream
		 * @throws IOException
		 */
		public OutputStream getOutputStream(Scanner scanner) throws IOException {
			OutputStream out = new FileOutputStream(String.format("%s.%s", scanner.file, scanner.format));
			if (scanner.isRaw()) return out;
			return new ImageFilterOutputStream(out, scanner);
		}
	}

	/**
	 * convert hex to bytes[]
	 * 
	 * @param hexstr
	 *            String
	 * @return byte[]
	 */
	public static byte[] hex2bytes(String hexstr) {
		try {
			return Hex.decodeHex(hexstr.toCharArray());
		} catch (DecoderException e) {
		}
		return null;
	}
	/**
	 * print message to stdout
	 * 
	 * @param msg
	 *            object
	 */
	static void message(Object msg) {
		System.out.println(msg);
	}
	/**
	 * mm to px
	 * @param v
	 * @return
	 */
	public static int mm2px(int v) {
		return (int) Math.round(v * ImageFilterOutputStream.DEFAULT_DPI / 25.4);
	}
	/**
	 * background
	 */
	private int background;
	/**
	 * progress bar
	 */
	private boolean bar;
	/**
	 * convert
	 */
	private String converter;
	
	int dpi = ImageFilterOutputStream.DEFAULT_DPI;

	/**
	 * extremely
	 */
	private boolean extremely;

	/**
	 * file
	 */
	private String file;

	Format format = Format.JPEG;

	int image_type = BufferedImage.TYPE_INT_RGB;

	PaperType paper = PaperType.A4;

	/**
	 * percent of scanning
	 */
	private int precent;

	/**
	 * raw image
	 */
	private boolean raw;

	/**
	 * term mode
	 */
	private boolean term = true;

	/**
	 * background transparent
	 */
	private boolean transparent;
	/**
	 * vendor/product id
	 */
	private short vendorId, productId;
	/**
	 * output log
	 */
	private boolean verbose;

	Rectangle viewport, middle;

	/**
	 * check status
	 * @param endpoint Object
	 * @param status PackageType
	 * @throws Exception
	 */
	protected abstract void check(Object endpoint, PackageType status) throws Exception;

	/**
	 * @return the background
	 */
	public int getBackground() {
		return background;
	}

	/**
	 * @return the convert
	 */
	public String getConverter() {
		return converter;
	}

	/**
	 * @return the file
	 */
	public String getFile() {
		return file;
	}

	/**
	 * @return the precent
	 */
	public int getPrecent() {
		return precent;
	}

	/**
	 * @return the productId
	 */
	public short getProductId() {
		return productId;
	}

	/**
	 * @return the vendorId
	 */
	public short getVendorId() {
		return vendorId;
	}

	/**
	 * wait for interrupt
	 * @param endpoint Object
	 * @return PackageType
	 * @throws Exception
	 */
	protected abstract PackageType interrupt(Object endpoint) throws Exception;

	/**
	 * @return the progressbar
	 */
	public boolean isBar() {
		return bar;
	}
	
	/**
	 * @return the extremely
	 */
	public boolean isExtremely() {
		return extremely;
	}

	/**
	 * @return the rawImage
	 */
	public boolean isRaw() {
		return raw;
	}
	
	/**
	 * @return the term
	 */
	public boolean isTerm() {
		return term;
	}

	/**
	 * @return the transparent
	 */
	public boolean isTransparent() {
		return transparent;
	}
	/**
	 * @return the verbose
	 */
	public boolean isVerbose() {
		return verbose;
	}
	/**
	 * log indicator print indicator
	 * 
	 * @param progress
	 *            int
	 * @param last
	 *            boolean
	 */
	protected void progress(int progress, boolean last) {
		if (!isBar()) return;
		int np = (progress * 100 / 222) | 0;
		if (np > precent || last) {
			precent = last ? 100 : np;
			String indicator = StringUtils.leftPad("", 73 * precent / 100, '#');
			indicator = StringUtils.rightPad(indicator, 73, '.');
			System.err.print((isTerm() ? "\033[80D\033[2K[" : "") + indicator + String.format("] %3d%%", precent));
			if (last || !isTerm())
				System.err.println("");
		}
	}
	
	/**
	 * receive data from usb 
	 * @param endpoint Object
	 * @return byte[]
	 * @throws Exception
	 */
	protected abstract byte[] receive(Object endpoint) throws Exception;
	
	/**
	 * start scan
	 * 
	 * @see cn.fos.xerox.scanner.driver.Scanner#scan()
	 */
	public final void scan() throws Exception {
		Object[] pipes = setup(); // 0:interrupt,1:input,2:output
		verbose("waiting scanning ...");
		byte[] setting = setup(interrupt(pipes[0]));
		verbose("setup");
		send(pipes[2], Command.READY.get());
		check(pipes[1], PackageType.ACK);
		send(pipes[2], Command.GO.get());
		send(pipes[2], setting);
		check(pipes[1], PackageType.ACK);
		verbose("reciving data ...");
		OutputStream output = SettingUp.DATA.getOutputStream(this);
		for (int count = 0;;) {
			PackageType pkg = PackageType.get(receive(pipes[1]));
			if (pkg == PackageType.DATA) {
				output.write(pkg.get());
			} else if (pkg != PackageType.INPROGRESS) {
				progress(count++, false);
				if (pkg == PackageType.ACK)
					break;
			}
		}
		send(pipes[2], Command.END.get());
		check(pipes[1], PackageType.ACK);
		progress(getPrecent(), true);
		verbose("process image ...");
		output.close();
	}
	
	/**
	 * send command to usb
	 * @param endpoint Object
	 * @param data byte[]
	 * @throws Exception
	 */
	protected abstract void send(Object endpoint, byte[] data) throws Exception;
	
	/**
	 * @param background the background to set
	 */
	public void setBackground(int background) {
		this.background = background;
	}
	
	/**
	 * @param progressbar
	 *            the progressbar to set
	 */
	public void setBar(boolean progressbar) {
		this.bar = progressbar;
	}

	/**
	 * @param convert
	 *            the convert to set
	 */
	public void setConvert(String converter) {
		this.converter = converter;
	}

	/**
	 * @param extremely the extremely to set
	 */
	public void setExtremely(boolean extremely) {
		this.extremely = extremely;
	}

	/**
	 * @param file
	 *            the file to set
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * @param precent
	 *            the precent to set
	 */
	public void setPrecent(int precent) {
		this.precent = precent;
	}

	/**
	 * @param productId
	 *            the productId to set
	 */
	public void setProductId(short productId) {
		this.productId = productId;
	}

	/**
	 * @param rawImage
	 *            the rawImage to set
	 */
	public void setRaw(boolean rawImage) {
		this.raw = rawImage;
	}

	/**
	 * @param term
	 *            the term to set
	 */
	public void setTerm(boolean term) {
		this.term = term;
	}

	/**
	 * @param transparent the transparent to set
	 */
	public void setTransparent(boolean transparent) {
		this.transparent = transparent;
	}

	/**
	 * setup scanner
	 * @return arrays of endpoints (interrupt,input,output)
	 * @throws Exception
	 */
	protected abstract Object[] setup() throws Exception;

	public byte[] setup(PackageType pkg) throws IOException {
		byte[] r = SettingUp.DATA.get();
		// 484403020003020200010001
		// ^^
		if (pkg.val[2] == 2)
			dpi = 200;
		else if (pkg.val[2] == 3)
			dpi = 300;
		else if (pkg.val[2] == 4)
			dpi = 400;
		else if (pkg.val[2] == 5)
			dpi = 600;
		// 484403020003020200010001
		// ^^
		if (pkg.val[3] == 0)
			image_type = BufferedImage.TYPE_BYTE_BINARY;
		else if (pkg.val[3] == 1)
			image_type = BufferedImage.TYPE_BYTE_GRAY;
		else if (pkg.val[3] == 2)
			image_type = BufferedImage.TYPE_INT_RGB;
		// 484403020002322b05010001
		//         ^^
		paper = Scanner.PaperType.get(pkg.val[4]);
		// 484403020002322b05010001
		// ^^
		if (pkg.val[5] == 1)
			format = Format.TIFF;
		else if (pkg.val[5] == 2)
			format = Format.JPEG;
		else if (pkg.val[5] == 3)
			format = Format.PDF;
		// 484403020003020200010001
		//             ^^^^^^
		int tb = mm2px(pkg.val[6]);//top/bottom
		int lr = mm2px(pkg.val[7]);//left/right
		int md = mm2px(pkg.val[8]);//middle
		viewport = new Rectangle(lr, tb, paper.get().width - 2 * lr, paper.get().height - 2 * tb);
		middle = new Rectangle((paper.get().width - md) / 2, (paper.get().height - md) / 2, md, md);
		file = StringUtils.defaultString(file,
				String.format("%04d%02d%02dT%02d%02d%02d", 
						SettingUp.DATA.year, 
						SettingUp.DATA.month, 
						SettingUp.DATA.date, 
						SettingUp.DATA.hours, 
						SettingUp.DATA.mintes, 
						SettingUp.DATA.seconds));
		if (isRaw()) {
			format = Format.RGB;
		} else if (isTransparent() || isExtremely()) {
			format = Format.PNG;
			image_type = BufferedImage.TYPE_INT_ARGB;
		} else if (Format.JPEG != format) {
			if (StringUtils.isNotBlank(converter)) {
				message(String.format("%s %s.%s.jpeg %s.%s", converter, file, format, file, format));
				file += "." + format;
			}
			format = Format.JPEG;
		}
		return r;
	}

	/**
	 * @param vendorId
	 *            the vendorId to set
	 */
	public void setVendorId(short vendorId) {
		this.vendorId = vendorId;
	}

	/**
	 * @param verbose the verbose to set
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	/**
	 * verbose message
	 * @param message
	 */
	protected void verbose(String message) {
		if (verbose) {
			System.err.println(message);
		}
	}
}
