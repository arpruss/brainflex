package mobi.omegacentauri.brainflex;

public class MindWaveMobile extends SerialLink57600 {

	public MindWaveMobile(String port) throws Exception {
		super(port);
	}
	
	@Override
	public void preStart(int baud, byte[] data) {
		// don't bother changing baud rate
		transmit(data);
	}
	
}
