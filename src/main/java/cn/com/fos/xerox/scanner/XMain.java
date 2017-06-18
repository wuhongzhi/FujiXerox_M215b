package cn.com.fos.xerox.scanner;

import java.awt.event.InputEvent;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import cn.com.fos.twt.TApplication;
import cn.com.fos.twt.TButton;
import cn.com.fos.twt.TCheckBox;
import cn.com.fos.twt.TDialog;
import cn.com.fos.twt.TLabel;
import cn.com.fos.twt.TProgress;
import cn.com.fos.twt.TText;
import cn.com.fos.twt.TText.AcceptFilter;
import cn.com.fos.twt.TText.INPUT;
import cn.com.fos.twt.TWidget;
import cn.com.fos.twt.TWidget.FocusListener;
import cn.com.fos.twt.TWindow;
import cn.com.fos.twt.device.TColorPair;
import cn.com.fos.twt.device.TGraphicContext;
import cn.com.fos.twt.device.TColors.Color;
import cn.com.fos.twt.device.ncurses.NTerminator;
import cn.com.fos.twt.util.TScreenSaver;
import cn.com.fos.twt.util.TWarning;
import cn.com.fos.xerox.scanner.utils.Scan;

public class XMain extends TApplication implements FocusListener {
	public static TProgress progress;
	private static final long serialVersionUID = 1124602839222280046L;
	public static void main(String[] args) throws Throwable {
		new XMain().run();
	}
	private int background = 200;
	private boolean fast_scanning = false;
	private String output = "";
	private boolean progress_bar = true;
	private boolean raw_rgb = false;
	private String usb_product = "0165";
	private String usb_vendor = "0550";
	private boolean verbose_mode = false;
	private boolean transparency = false;
	public XMain() {
		super("SCANNER");
	}
	public void lost(TWidget w) {
		getGraphic().startDraw();
		w.setShadow(false);
		w.getParent().updateUI();
		getGraphic().endDraw();
	}
	public void focus(TWidget w) {
		getGraphic().startDraw();
		w.setShadow(true);
		w.getParent().updateUI();
		getGraphic().endDraw();
	}
	protected void createWidgets() {
		final TWindow win = new TWindow(getResource("message", "TITLE"), 33, 8);
		final TButton scan = new TButton(getResource("message", "SCAN"), 2, 1);
		scan.setBorder(true); scan.addListener(this);
		final TButton setup = new TButton(getResource("message", "SETUP"), 12, 1);
		setup.setBorder(true); setup.addListener(this);
		final TButton quit = new TButton(getResource("message", "QUIT"), 22, 1);
		quit.setBorder(true); quit.addListener(this);
		win.add(setup).add(scan).add(quit);
		scan.addListener(new ClickListener() {
			public void clicked(InputEvent e) {
				final TDialog t_progress = new TDialog(43, 4);
				t_progress.setExitCode(-1);
				t_progress.add(progress = new TProgress(40, 1));
				final List<String> args = new ArrayList<String>();
				if (progress_bar) args.add("-b");
				if (fast_scanning) args.add("-f");
				if (raw_rgb) args.add("-r");
				if (transparency) {
					args.add("-t");
					args.add("-B" + background);
				}
				if (verbose_mode) args.add("-V");
				args.add("-v" + usb_vendor);
				args.add("-p" + usb_product);
				if (output.length() != 0) args.add("-o " + output);
				t_progress.setVisible(true);
				new Thread(new Runnable() {
					public void run() {
						try {
							Scan.main(args.toArray(new String[0]));
						} catch (Throwable ex) {
							new TWarning(ex.getMessage(), new ClickListener() {
								public void clicked(InputEvent e) {
									getDesktop().topWindow().dispose();
								}
							}).setVisible(true);
						} finally {
							t_progress.dispose();
						}
					}
				}).start(); 
			}
		});
		setup.addListener(new ClickListener() {
			public void clicked(InputEvent e) {
				final TDialog t_dialog = new TDialog(getResource("message", "OPTIONS"), 50, 11);
				t_dialog.setModelWindow(true);
				final TCheckBox t_fast_scanning = new TCheckBox(getResource("message", "FASTSCAN"), 1, 1);
				t_fast_scanning.setChecked(fast_scanning);
				t_dialog.add(t_fast_scanning);
				final TCheckBox t_raw_rgb = new TCheckBox(getResource("message", "RAWOUTPUT"), 25, 1);
				t_raw_rgb.setChecked(raw_rgb);
				t_dialog.add(t_raw_rgb);
				final TCheckBox t_transparency = new TCheckBox(getResource("message", "TRANSPARENCY"), 1, 2);
				t_transparency.setChecked(transparency);
				t_dialog.add(t_transparency);
				final TText t_background = new TText(String.valueOf(background), 18, 2, 3);
				t_dialog.add(t_background);
				final TCheckBox t_verbose_mode = new TCheckBox(getResource("message", "VERBOSE"), 25, 2);
				t_verbose_mode.setChecked(verbose_mode);
				t_verbose_mode.setEnable(!NTerminator.class.isAssignableFrom(TGraphicContext.getEnvironment().getTerminatorType()));
				t_dialog.add(t_verbose_mode);
				t_dialog.add(new TLabel(getResource("message", "VENDOR"), 1, 3));
				final TText t_usb_vendor = new TText(usb_vendor, 10, 3, 4);
				t_dialog.add(t_usb_vendor);
				t_dialog.add(new TLabel(getResource("message", "PRODUCT"), 25, 3));
				final TText t_usb_product = new TText(usb_product, 35, 3, 4);
				t_dialog.add(t_usb_product);
				t_dialog.add(new TLabel(getResource("message", "OUTPUT"), 1, 4));
				final TText t_output = new TText(output, 10, 4, 36);
				t_dialog.add(t_output);
				int cw = t_dialog.getClientWidth();
				String txt = getResource("CANCEL");
				final TButton t_cancel = new TButton(txt, cw - TText.getTextWidth(txt) - 2, 7);
				t_dialog.add(t_cancel);t_cancel.setBrace(' ', ' ');
				txt = getResource("YES");
				final TButton t_ok = new TButton(txt, t_cancel.getX() - TText.getTextWidth(txt) - 2, 7);
				t_dialog.add(t_ok);t_ok.setBrace(' ', ' ');
				t_usb_vendor.setMaxLenth(4);
				t_usb_vendor.setInputType(INPUT.INTEGER);
				t_usb_product.setMaxLenth(4);
				t_usb_product.setInputType(INPUT.INTEGER);
				//setup listeners
				t_background.setEnable(transparency);
				t_background.setInputType(INPUT.INTEGER);
				t_background.setText(String.valueOf(background));
				t_transparency.addListener(new ChangedListener() {
					public void changed(TWidget widget) {
						t_background.setText(String.valueOf(background));
						t_background.setEnable(t_transparency.isChecked());
					}
				});
				t_background.setFilter(new AcceptFilter<BigInteger>() {
					public boolean accept(BigInteger obj) {
						int val = obj.intValue(); 
						return val > 0 && val < 256;
					}
				});
				t_ok.addListener(new ClickListener() {
					public void clicked(InputEvent e) {
						usb_vendor = t_usb_vendor.getText().trim();
						usb_product = t_usb_product.getText().trim();
						transparency = t_transparency.isChecked();
						raw_rgb = t_raw_rgb.isChecked();
						fast_scanning = t_fast_scanning.isChecked();
						if (transparency)
							background = Integer.parseInt(t_background.getText().trim());
						verbose_mode = t_verbose_mode.isChecked();
						output = t_output.getText().trim();
						t_cancel.click();
					}
				});
				t_cancel.addListener(new ClickListener() {
					public void clicked(InputEvent e) {
						t_dialog.dispose();
					}
				});
				t_dialog.setVisible(true);
			}
		});
		quit.addListener(new ClickListener() {
			public void clicked(InputEvent e) {
				getDesktop().dispose();
			}
		});
		setScreenSaver(new MyScreenSaver("SCREEN SAVER"));
		win.setVisible(true);
	}

	class MyScreenSaver extends TScreenSaver {
		private static final long serialVersionUID = -5737646514952379908L;
		private Random random = new Random();
		private TLabel hint = new TLabel("PLEASE PRESS ESCAPE KEY TO CLOSE THIS WINDOW");
		private int sw, sh;
		public MyScreenSaver(String title) {
			super(title);
			setColorPair(new TColorPair(Color.MAGENTA, Color.BLACK));
			sw = getClientWidth() - hint.getWidth();
			sh = getClientHeight() - hint.getHeight();
			hint.setColorPair(getColorPair());
			add(hint, sw / 2, sh / 2);
		}
		public  void timeout(int ticks) {
			if (ticks == 0) {
				hint.move(sw / 2, sh / 2);
			} else {
				hint.move(random.nextInt(sw), random.nextInt(sh));
			}
			updateUI();
		}
	}
	
	@Override
	protected int getTimeout() {
		return (int)TimeUnit.SECONDS.toMillis(30);
	}
}
