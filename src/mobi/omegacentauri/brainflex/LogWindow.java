package mobi.omegacentauri.brainflex;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

public class LogWindow extends JFrame {
	private static final long serialVersionUID = 1700923833341394306L;
	private JTextArea textArea;

	public LogWindow() {
		super();
		
		setLocationByPlatform(true);
		
		setTitle("BrainFlex");
		setSize(640,480);

		textArea = new JTextArea();
		textArea.setEditable(false);
		((DefaultCaret)textArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		
		JScrollPane scroll = new JScrollPane( textArea );
		
		add(scroll);
		
		setVisible(true);
	}
	
	public void log(String s) {
		textArea.append(s+"\n");
	}
}
