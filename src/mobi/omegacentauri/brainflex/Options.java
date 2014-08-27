package mobi.omegacentauri.brainflex;

import java.awt.Checkbox;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

public class Options extends JFrame {
	private static final long serialVersionUID = 4138326933073169336L;
	Preferences prefs;
	protected File saveFile;
	static final String BINEXT = ".thg";
	static public final Class[] LINKS = { SerialLink57600.class, 
		MindWaveMobile.class, 
		BrainLinkSerialLinkLL.class, 
		BrainLinkBridgeSerialLink.class, FileDataLink.class
	};
	
	public Options() {
		super();
		
		saveFile = null;
		
		prefs = Preferences.userNodeForPackage(BrainFlex.class);
		
		setLocationByPlatform(true);
		
		setTitle("BrainFlex Options");
		setSize(640,220);

		Container pane = getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		
		Box comPortBox = new Box(BoxLayout.X_AXIS);
//		comPortPanel.setLayout(new BoxLayout(comPortPanel, BoxLayout.X_AXIS));

		final JComboBox<String> inMode = new JComboBox<String>();
		inMode.addItem("Mindwave Mobile");
		inMode.addItem("57600 baud Bluetooth-serial bridge + MindFlex");
		inMode.addItem("BrainLink with standard firmware + MindFlex");
		inMode.addItem("BrainLink with custom firmware + MindFlex");
		inMode.addItem("Load from saved file");
		comPortBox.add(inMode);

		final JLabel comPortLabel = new JLabel("Serial port:");
		comPortBox.add(comPortLabel);
		
		final TextField comPortField = new TextField(prefs.get(BrainFlex.PREF_SERIAL_PORT, ""));
		comPortField.selectAll();
		Dimension m = comPortField.getMaximumSize();
		m.height = inMode.getMaximumSize().height;
		comPortField.setMaximumSize(m);
		comPortBox.add(comPortField);
		
		inMode.setSelectedIndex(prefs.getInt(BrainFlex.PREF_IN_MODE, 0));
		comPortField.addTextListener(new TextListener() {
			
			@Override
			public void textValueChanged(TextEvent arg0) {
				prefs.put(BrainFlex.PREF_SERIAL_PORT, comPortField.getText());
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
				}
			}
		});
		
		final JLabel notes = new JLabel(" ", SwingConstants.LEFT);

		final Checkbox heartCheck = new Checkbox("Heart mode", prefs.getBoolean(BrainFlex.PREF_HEART_MODE, false));
		
		final Checkbox rawCheck = new Checkbox("Raw data window", prefs.getBoolean(BrainFlex.PREF_RAW, true));
		
		final Checkbox powerCheck = new Checkbox("Processed data window", prefs.getBoolean(BrainFlex.PREF_POWER, true));
		
		rawCheck.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_RAW, rawCheck.getState());
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
				}
				updateNotes(notes,heartCheck,rawCheck,powerCheck);
			}
		});
		
		powerCheck.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_POWER, powerCheck.getState());
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
				}
				updateNotes(notes,heartCheck,rawCheck,powerCheck);
			}
		});

		heartCheck.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_HEART_MODE, heartCheck.getState());
				flushPrefs();
				updateNotes(notes,heartCheck,rawCheck,powerCheck);
			}
		});
		
		updateNotes(notes,heartCheck,rawCheck,powerCheck);

		final Checkbox logCheck = new Checkbox("Log window", prefs.getBoolean(BrainFlex.PREF_LOG_WINDOW, true));
		
		logCheck.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_LOG_WINDOW, logCheck.getState());
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
				}
			}
		});
		
		rawCheck.addItemListener(new ItemListener() {			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_POWER, powerCheck.getState());
				try {
					prefs.flush();
				} catch (BackingStoreException e) {
				}
			}
		});

		final Checkbox saveBinaryCheck = new Checkbox("Save binary data", prefs.getBoolean(BrainFlex.PREF_SAVE_BINARY, false));
		
		saveBinaryCheck.setState(prefs.getBoolean(BrainFlex.PREF_SAVE_BINARY, false));
		
		saveBinaryCheck.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_SAVE_BINARY, saveBinaryCheck.getState());
				flushPrefs();
			}
		});

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		final JButton go = new JButton("Go");

		go.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if ( powerCheck.getState() || rawCheck.getState()) {
					if (LINKS[inMode.getSelectedIndex()] == FileDataLink.class) {
						if (comPortField.getText() == null ||
								comPortField.getText().length() == 0)
							return;
						
						if (prefs.getBoolean(BrainFlex.PREF_SAVE_BINARY, false)) {
							final JFileChooser fc = new JFileChooser();
							fc.setFileFilter(new FileFilter() {

								@Override
								public boolean accept(File arg0) {
									if (arg0.isDirectory())
										return true;
									return arg0.getName().endsWith(BINEXT);
								}

								@Override
								public String getDescription() {
									return "*"+BINEXT;
								}});
							
							if (fc.showSaveDialog(go) == JFileChooser.APPROVE_OPTION) {
								String n = fc.getSelectedFile().getPath();
								if (! n.endsWith(BINEXT))
									n += BINEXT;
								Options.this.saveFile = new File(n);
							}
							else {
								return;
							}
						}
						
						Options.this.dispose();
						new BrainFlex(saveFile);
					}
					else {
						final JFileChooser fc = new JFileChooser();
						String name = prefs.get(BrainFlex.PREF_FILE_NAME, null);
						if (name != null)
							fc.setSelectedFile(new File(name));
						fc.setFileFilter(new FileFilter() {

							@Override
							public boolean accept(File arg0) {
								if (arg0.isDirectory())
									return true;
								return arg0.getName().endsWith(BINEXT);
							}

							@Override
							public String getDescription() {
								return "*"+BINEXT;
							}});
						
						if (fc.showOpenDialog(go) == JFileChooser.APPROVE_OPTION) {
							String n = fc.getSelectedFile().getPath();
							if (! n.endsWith(BINEXT))
								n += BINEXT;
							prefs.put(BrainFlex.PREF_FILE_NAME, n);
							flushPrefs();
							Options.this.dispose();
							new BrainFlex(null);
						}						
					}
				}
			}
		});
		
		JRootPane root = getRootPane();
		root.setDefaultButton(go);
		
		buttonPanel.add(go);
				
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Options.this.dispose();
			}
		});
		
		JButton license = new JButton("License");
		license.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				LicenseDialog ld = new LicenseDialog();
				ld.setVisible(true);
			}
		});
		
		buttonPanel.add(cancel);
		buttonPanel.add(notes);
		buttonPanel.add(license);
		
		pane.add(comPortBox);
		pane.add(heartCheck);
		pane.add(rawCheck);
		pane.add(powerCheck);
		pane.add(logCheck);
		pane.add(saveBinaryCheck);
		pane.add(buttonPanel);
		
		int curInMode = prefs.getInt(BrainFlex.PREF_IN_MODE, 0);
		inMode.setSelectedIndex(curInMode);
		if (LINKS[curInMode] == FileDataLink.class) {
			comPortLabel.setEnabled(false);
			comPortField.setEnabled(false);
			saveBinaryCheck.setEnabled(false);
		}
		else {
			comPortLabel.setEnabled(true);
			comPortField.setEnabled(true);
			saveBinaryCheck.setEnabled(true);
		}
			
		inMode.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				prefs.putInt(BrainFlex.PREF_IN_MODE, inMode.getSelectedIndex());
				flushPrefs();
				if (LINKS[inMode.getSelectedIndex()] == FileDataLink.class) {
					comPortLabel.setEnabled(false);
					comPortField.setEnabled(false);
					saveBinaryCheck.setEnabled(false);
				}
				else {
					comPortLabel.setEnabled(true);
					comPortField.setEnabled(true);
					saveBinaryCheck.setEnabled(true);
				}
			}
		});
		
		setVisible(true);
	}

	protected void updateNotes(JLabel notes, Checkbox heartCheck, Checkbox rawCheck,
			Checkbox powerCheck) {
		if (!heartCheck.getState() && !rawCheck.getState() && !powerCheck.getState()) {
			notes.setText("At least one data window needs to be active.");
		}
		else {
			notes.setText("");
		}
		rawCheck.setEnabled(!heartCheck.getState());
		powerCheck.setEnabled(!heartCheck.getState());
	}
	
	public void flushPrefs() {
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
		}
	}


}

