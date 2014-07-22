package examples;

public abstract class DataLink {
	 public abstract byte[] receiveBytes();

	 public abstract void transmit(byte... data);
	 
	 public void transmit(int... data) {
		 byte[] out = new byte[data.length];
		 for (int i=0; i < data.length; i++) {
			 out[i] = (byte)(int)data[i];
		 }
		 transmit(out);
	 }

	public abstract void clearBuffer();
}
