package cn.fos.xerox.scanner.driver;

import java.util.List;

import javax.usb.UsbControlIrp;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbEndpoint;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbPipe;
import javax.usb.UsbPlatformException;

public class FujiXerox_MFP1 extends Scanner {

	/**
	 * check status is OK
	 * 
	 * @param pipe
	 *            UsbPipe
	 * @param status
	 *            PackageType
	 * @throws Exception
	 */
	protected void check(Object pipe, PackageType status) throws Exception {
		if (PackageType.get(receive(pipe)) != status)
			throw new UsbPlatformException("the status is wrong");
		UsbDevice device = ((UsbPipe)pipe).getUsbEndpoint().getUsbInterface().getUsbConfiguration().getUsbDevice();
		UsbControlIrp controlIrp = device.createUsbControlIrp((byte) 0xa1, (byte) 0x00, (short) 0x00, (short) 0x0100);
		controlIrp.setData(new byte[((UsbPipe)pipe).getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize()]);
		controlIrp.setAcceptShortPacket(true);
		device.syncSubmit(controlIrp);
	}

	/**
	 * find usb device
	 * 
	 * @param hub
	 *            UsbHub
	 * @return UsbDevice
	 */
	@SuppressWarnings("unchecked")
	private UsbDevice findDevice(UsbHub hub) {
		for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
			UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
			if (desc.idVendor() == getVendorId() && desc.idProduct() == getProductId())
				return device;
			if (device.isUsbHub()) {
				device = findDevice((UsbHub) device);
				if (device != null)
					return device;
			}
		}
		return null;
	}

	/**
	 * get interrupt
	 * 
	 * @param pipe
	 *            UsbPipe
	 * @return PackageType
	 * @throws Exception
	 */
	protected PackageType interrupt(Object pipe) throws Exception {
		return PackageType.get(receive(pipe));
	}

	/**
	 * read data from usb EndPoint
	 * 
	 * @param pipe
	 *            UsbPipe
	 * @return byte[]
	 * @throws Exception
	 */
	protected byte[] receive(Object pipe) throws Exception {
		byte[] r = new byte[((UsbPipe)pipe).getUsbEndpoint().getUsbEndpointDescriptor().wMaxPacketSize()];
		int s = ((UsbPipe)pipe).syncSubmit(r);
		if (s == r.length)
			return r;
		if (s < 1)
			return null;
		byte[] n = new byte[s];
		System.arraycopy(r, 0, n, 0, s);
		return n;
	}

	/**
	 * send data to scanner
	 * 
	 * @param pipe
	 *            UsbPipe
	 * @param data
	 *            byte[]
	 * @throws Exception
	 */
	protected void send(Object pipe, byte[] data) throws Exception {
		((UsbPipe)pipe).syncSubmit(data);
	}

	/**
	 * setup usb pipes
	 * 
	 * @return UsbPipe[]
	 * @throws Exception
	 */
	protected Object[] setup() throws Exception {
		UsbDevice device = findDevice(UsbHostManager.getUsbServices().getRootUsbHub());
		if (device == null)
			System.exit(255);
		final UsbInterface iface = device.getActiveUsbConfiguration().getUsbInterface((byte) 0);
		iface.claim(new UsbInterfacePolicy() {
			public boolean forceClaim(UsbInterface usbInterface) {
				return true;
			}
		});
		final UsbEndpoint interrpu_ep = iface.getUsbEndpoint((byte) 0x83);
		final UsbEndpoint input_ep = iface.getUsbEndpoint((byte) 0x82);
		final UsbEndpoint output_ep = iface.getUsbEndpoint((byte) 0x02);
		final UsbPipe interrupt = interrpu_ep.getUsbPipe();
		final UsbPipe input = input_ep.getUsbPipe();
		final UsbPipe output = output_ep.getUsbPipe();
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				if (iface.isClaimed()) {
					try {
						for (UsbPipe pipe : new UsbPipe[] { interrupt, input, output }) {
							if (pipe.isOpen()) {
								pipe.abortAllSubmissions();
								pipe.close();
							}
						}
					} catch (Throwable e) {
						e.printStackTrace();
					} finally {
						try {
							iface.release();
						} catch (Throwable e) {
							e.printStackTrace();
						}
					}
				}
			}
		}));
		interrupt.open();
		output.open();
		input.open();
		return new Object[] { interrupt, input, output };
	}

}
