package mobi.omegacentauri.brainflex;

public class Mark {
	int t;
	int rawCount;
	char descriptor;
	
	public Mark(int t, int rawCount) {
		this(t,rawCount,(char)0);
	}
	
	public Mark(int t, int rawCount, char c) {
		this.t = t;
		this.rawCount = rawCount;
		this.descriptor = c;
	}		
}
