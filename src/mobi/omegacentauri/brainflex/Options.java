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

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

public class Options extends JFrame {
	private static final long serialVersionUID = 4138326933073169336L;
	Preferences prefs;
	protected File saveFile;
	static final String BINEXT = ".thg";
	
	public Options() {
		super();
		
		saveFile = null;
		
		prefs = Preferences.userNodeForPackage(BrainFlex.class);
		
		setLocationByPlatform(true);
		
		setTitle("BrainFlex Options");
		setSize(640,200);

		Container pane = getContentPane();
		pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
		
		JPanel comPortPanel = new JPanel();
		comPortPanel.setLayout(new BoxLayout(comPortPanel, BoxLayout.X_AXIS));
		
		JLabel label = new JLabel("Serial port: ");
		comPortPanel.add(label);
		
		final TextField comPortField = new TextField(prefs.get(BrainFlex.PREF_SERIAL_PORT, ""));
		comPortField.selectAll();
		Dimension m = comPortField.getMaximumSize();
		m.height = 3*label.getMaximumSize().height/2;
		comPortField.setMaximumSize(m);
		comPortPanel.add(comPortField);
		
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
				updateNotes(notes,rawCheck,powerCheck);
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
				updateNotes(notes,rawCheck,powerCheck);
			}
		});

		updateNotes(notes,rawCheck,powerCheck);

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
		
		final Checkbox customFWCheck = new Checkbox("Custom BrainLink firmware (github.com/arpruss/custom-brainlink-firmware)", 
				prefs.getBoolean(BrainFlex.PREF_CUSTOM_FW, false));
		
		customFWCheck.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				prefs.putBoolean(BrainFlex.PREF_CUSTOM_FW, customFWCheck.getState());
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

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
		
		JButton go = new JButton("Go");
		go.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (powerCheck.getState() || rawCheck.getState()) {
					Options.this.dispose();
					new BrainFlex(comPortField.getText(), saveFile);
				}
			}
		});
		
		JRootPane root = getRootPane();
		root.setDefaultButton(go);
		
		buttonPanel.add(go);
		
		final JButton saveData = new JButton("Sava data: (none)");
		saveData.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				final JFileChooser fc = new JFileChooser();
				fc.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File arg0) {
						if (arg0.isDirectory())
							return false;
						return arg0.getName().endsWith(BINEXT);
					}

					@Override
					public String getDescription() {
						return "*"+BINEXT;
					}});
				
				if (fc.showSaveDialog(saveData) == JFileChooser.APPROVE_OPTION) {
					String n = fc.getSelectedFile().getPath();
					if (! n.endsWith(BINEXT))
						n += BINEXT;
					Options.this.saveFile = new File(n);
					saveData.setText("Save data: "+Options.this.saveFile.getName());
				}
				else 
					saveData.setText("Save data: (none)");
			}
		});
		
		buttonPanel.add(saveData);
		
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Options.this.dispose();
			}
		});
		
		buttonPanel.add(cancel);
		buttonPanel.add(notes);
		
		pane.add(comPortPanel);
		pane.add(rawCheck);
		pane.add(powerCheck);
		pane.add(logCheck);
		pane.add(customFWCheck);
		pane.add(buttonPanel);
		
		setVisible(true);
	}

	protected void updateNotes(JLabel notes, Checkbox rawCheck,
			Checkbox powerCheck) {
		if (!rawCheck.getState() && !powerCheck.getState()) {
			notes.setText("At least one data window needs to be active.");
		}
		else {
			notes.setText("");
		}
	}


	public class ZOptions extends JFrame {
		private static final long serialVersionUID = 4138326933073169336L;
		Preferences prefs;
		
		public ZOptions() {
			super();
			
			prefs = Preferences.userNodeForPackage(BrainFlex.class);
			
			setLocationByPlatform(true);
			
			setTitle("BrainFlex");
			setSize(640,200);

			Container pane = getContentPane();
			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
			
			JPanel comPortPanel = new JPanel();
			comPortPanel.setLayout(new BoxLayout(comPortPanel, BoxLayout.X_AXIS));
			
			JLabel label = new JLabel("Serial port: ");
			comPortPanel.add(label);
			
			final TextField comPortField = new TextField(prefs.get(BrainFlex.PREF_SERIAL_PORT, ""));
			comPortField.selectAll();
			Dimension m = comPortField.getMaximumSize();
			m.height = 3*label.getMaximumSize().height/2;
			comPortField.setMaximumSize(m);
			comPortPanel.add(comPortField);
			
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
					updateNotes(notes,rawCheck,powerCheck);
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
					updateNotes(notes,rawCheck,powerCheck);
				}
			});

			updateNotes(notes,rawCheck,powerCheck);

			final Checkbox customFWCheck = new Checkbox("Custom BrainLink firmware (github.com/arpruss/custom-brainlink-firmware)", prefs.getBoolean(BrainFlex.PREF_CUSTOM_FW, true));
			
			customFWCheck.addItemListener(new ItemListener() {
				
				@Override
				public void itemStateChanged(ItemEvent arg0) {
					prefs.putBoolean(BrainFlex.PREF_CUSTOM_FW, customFWCheck.getState());
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

			JPanel buttonPanel = new JPanel();
			buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
			
			JButton go = new JButton("Go");
			go.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					if (powerCheck.getState() || rawCheck.getState()) {
						Options.this.dispose();
						new BrainFlex(comPortField.getText(), saveFile);
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
			
			buttonPanel.add(cancel);
			buttonPanel.add(notes);
			
			pane.add(comPortPanel);
			pane.add(rawCheck);
			pane.add(powerCheck);
			pane.add(customFWCheck);
			pane.add(buttonPanel);
			
			setVisible(true);
		}

		protected void updateNotes(JLabel notes, Checkbox rawCheck,
				Checkbox powerCheck) {
			if (!rawCheck.getState() && !powerCheck.getState()) {
				notes.setText("At least one data window needs to be active.");
			}
			else {
				notes.setText("");
			}
		}
	}

}

