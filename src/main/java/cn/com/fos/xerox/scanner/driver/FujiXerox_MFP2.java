package cn.com.fos.xerox.scanner.driver;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.usb4java.ConfigDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.EndpointDescriptor;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public class FujiXerox_MFP2 extends Scanner {
	/**
	 * claimed
	 */
	private boolean claimed;

	/**
	 * device handle
	 */
	private DeviceHandle handle;

	/**
	 * check libusb
	 * 
	 * @param errorcode
	 * @throws Exception
	 */
	private void check(int errorcode) throws Exception {
		if (errorcode < 0)
			throw new LibUsbException(errorcode);
	}

	/**
	 * check received status
	 * 
	 * @param ep
	 *            EndpointDescriptor
	 * @param p2
	 *            PackageType
	 * @throws Exception
	 */
	protected void check(Object ep, PackageType status) throws Exception {
		if (PackageType.get(receive(ep)) != status)
			throw new LibUsbException(LibUsb.ERROR_INVALID_PARAM);
		ByteBuffer buffer = ByteBuffer.allocateDirect(((EndpointDescriptor)ep).wMaxPacketSize());
		check(LibUsb.controlTransfer(handle, (byte) 0xa1, (byte) 0x00, (short) 0x00, (short) 0x0100, buffer, 0));
	}

	/**
	 * waiting for interrupt
	 * 
	 * @param ep
	 *            EndpointDescriptor
	 * @return PackageType
	 * @throws Exception
	 */
	protected PackageType interrupt(Object ep) throws Exception {
		ByteBuffer irp = ByteBuffer.allocateDirect(((EndpointDescriptor)ep).wMaxPacketSize());
		IntBuffer counter = IntBuffer.allocate(1);
		check(LibUsb.interruptTransfer(handle, ((EndpointDescriptor)ep).bEndpointAddress(), irp, counter, 0));
		byte[] in = new byte[counter.get()];
		irp.get(in);
		return PackageType.get(in);
	}

	/**
	 * read data from scanner
	 * 
	 * @param ep
	 *            EndpointDescriptor
	 * @param length
	 *            size
	 * @return
	 * @throws Exception
	 */
	protected byte[] receive(Object ep) throws Exception {
		ByteBuffer buffer = ByteBuffer.allocateDirect(((EndpointDescriptor)ep).wMaxPacketSize());
		IntBuffer counter = IntBuffer.allocate(1);
		check(LibUsb.bulkTransfer(handle, ((EndpointDescriptor)ep).bEndpointAddress(), buffer, counter, 0));
		int s = counter.get();
		if (s < 1)
			return null;
		byte[] r = new byte[s];
		buffer.get(r);
		return r;
	}

	/**
	 * send command to scanner
	 * 
	 * @param ep
	 *            EndpointDescriptor
	 * @param data
	 *            commands
	 * @throws Exception
	 */
	protected void send(Object ep, byte[] data) throws Exception {
		ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
		IntBuffer counter = IntBuffer.allocate(1);
		buffer.put(data);
		check(LibUsb.bulkTransfer(handle, ((EndpointDescriptor)ep).bEndpointAddress(), buffer, counter, 0));
	}

	/**
	 * setup scanner
	 * 
	 * @return EndpointDescriptor[]
	 * @throws Exception
	 */
	protected Object[] setup() throws Exception {
		check(LibUsb.init(null));
		handle = LibUsb.openDeviceWithVidPid(null, getVendorId(), getProductId());
		if (handle == null) throw new LibUsbException(LibUsb.ERROR_NO_DEVICE);
		check(LibUsb.claimInterface(handle, 0)); claimed = true;
		ConfigDescriptor config = new ConfigDescriptor();
		check(LibUsb.getConfigDescriptor(LibUsb.getDevice(handle), (byte) 0, config));
		EndpointDescriptor[] dps = config.iface()[0].altsetting()[0].endpoint();
		return new Object[] { dps[2], dps[1], dps[0] };
	}

	/**
	 * @see cn.com.fos.xerox.scanner.driver.Scanner#shutdown()
	 */
	@Override
	protected void shutdown() throws Exception {
		if (handle != null) {
			if (claimed) LibUsb.releaseInterface(handle, 0);
			LibUsb.close(handle);
		}
		LibUsb.exit(null);
	}
}
