package com.lathrum.VMF2OBJ.gui;

import java.io.File;
import java.util.logging.Logger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.lathrum.VMF2OBJ.Job;
import com.lathrum.VMF2OBJ.VMF2OBJ;
import com.lathrum.VMF2OBJ.SimpleFormatter;
import com.lathrum.VMF2OBJ.fileStructure.VMFFileEntry;

public class VMF2OBJFrame extends javax.swing.JFrame {

	private Job job = new Job();
	private Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private TextAreaHandler logHandler;
	private DefaultListModel<Path> listResourcesModel = new DefaultListModel<>();

	/**
	 * @param args the command line arguments
	 */
	public static void main(String args[]) {

		// Set the system look and feel
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception ex) {
		}

		// Create and display the form
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				new VMF2OBJFrame().setVisible(true);
			}
		});
	}

	public VMF2OBJFrame() {
		initComponents();
		initComponentsCustom();

		// VMF filedropper
		new FileDrop(fileInput, files -> {
			for (File file : files) {
				if (file.getName().toLowerCase().endsWith(".vmf")) {
					job.file = new VMFFileEntry(file);
					fileInput.setText(file.toString());
				}
			}
			validateConvertButtonEnabled();
		});

		// VPK and custom content file dropper
		new FileDrop(listResources, files -> {
			for (File file : files) {
				if (file.isDirectory()) {
					listResourcesModel.addElement(file.toPath());
				} else if (file.getName().toLowerCase().endsWith(".vpk")) {
					listResourcesModel.addElement(file.toPath());
				}
			}
			validateConvertButtonEnabled();
		});
	}

	public ListModel<Path> getFilesModel() {
		return listResourcesModel;
	}

	public void validateConvertButtonEnabled() {
		boolean value = job.file != null && !listResourcesModel.isEmpty();
		buttonConvert.setEnabled(value);
	}

	public void setButtonsEnabled(boolean value) {
		buttonConvert.setEnabled(value);
	}

	private File openFileDialog(File defaultFile, FileFilter filter) {
		JFileChooser fc = new JFileChooser() {

			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				if (file != null && !file.exists()) {
					showFileNotFoundDialog();
					return;
				}
				super.approveSelection();
			}

			private void showFileNotFoundDialog() {
				JOptionPane.showMessageDialog(this, "The selected file doesn't exist.");
			}
		};
		fc.setFileFilter(filter);

		if (defaultFile != null) {
			fc.setSelectedFile(defaultFile);
		} else {
			// use user.dir as default directory
			try {
				fc.setSelectedFile(new File(System.getProperty("user.dir")));
			} catch (Exception ex) {
			}
		}

		// show open file dialog
		int option = fc.showOpenDialog(this);

		if (option != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		return fc.getSelectedFile();
	}

	private File[] openFilesDialog(File defaultFile, FileFilter filter) {
		JFileChooser fc = new JFileChooser() {

			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				if (file != null && !file.exists()) {
					showFileNotFoundDialog();
					return;
				}
				super.approveSelection();
			}

			private void showFileNotFoundDialog() {
				JOptionPane.showMessageDialog(this, "The selected file doesn't exist.");
			}
		};
		fc.setMultiSelectionEnabled(true);
		fc.setFileFilter(filter);

		if (defaultFile != null) {
			fc.setSelectedFile(defaultFile);
		} else {
			// use user.dir as default directory
			try {
				fc.setSelectedFile(new File(System.getProperty("user.dir")));
			} catch (Exception ex) {
			}
		}

		// show open file dialog
		int option = fc.showOpenDialog(this);

		if (option != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		return fc.getSelectedFiles();
	}

	private File saveFileDialog(File defaultFile) {
		JFileChooser fc = new JFileChooser() {

			@Override
			public void approveSelection() {
				File file = getSelectedFile();
				File objFile = new File(file.getAbsolutePath() + ".obj");
				File mtlFile = new File(file.getAbsolutePath() + ".mtl");
				if ((objFile != null && objFile.exists() && !askOverwrite(objFile))
						|| (mtlFile != null && mtlFile.exists() && !askOverwrite(mtlFile))) {
					return;
				}
				super.approveSelection();
			}

			private boolean askOverwrite(File file) {
				String title = "Overwriting " + file.getPath();
				String message = "File " + file.getName() + " already exists.\n" + "Do you like to replace it?";

				int choice = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE);

				return choice == JOptionPane.OK_OPTION;
			}
		};
		fc.setMultiSelectionEnabled(false);
		fc.setSelectedFile(defaultFile);

		// show save file dialog
		int option = fc.showSaveDialog(this);

		if (option != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		return fc.getSelectedFile();
	}

	private File selectDirectoryDialog(File defaultFile) {
		JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(false);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		if (defaultFile != null) {
			fc.setSelectedFile(defaultFile);
		} else {
			// use user.dir as default directory
			try {
				fc.setSelectedFile(new File(System.getProperty("user.dir")));
			} catch (Exception ex) {
			}
		}

		// show dir selection dialog
		int option = fc.showOpenDialog(this);

		if (option != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		return fc.getSelectedFile();
	}

	private File openVMFFileDialog(File vmfFile) {
		return openFileDialog(vmfFile, new FileNameExtensionFilter("Source engine map file (.vmf)", "vmf"));
	}

	private File[] openVPKFileDialog(File vpkFile) {
		return openFilesDialog(vpkFile, new FileNameExtensionFilter("Valve Pak file (.vpk)", "vpk"));
	}

	private File saveVmfFileDialog(File vmfFile) {
		return saveFileDialog(vmfFile);
	}

	/**
	 * Start the conversion in a new thread.
	 */
	private void startConversion() {
		new Thread() {

			@Override
			public void run() {

				job.resourcePaths = new ArrayList<Path>();
				Enumeration<Path> entries = listResourcesModel.elements();
				while (entries.hasMoreElements()) {
					Path entry = entries.nextElement();
					job.resourcePaths.add(entry);
				}

				// deactivate buttons
				setButtonsEnabled(false);

				try {
					VMF2OBJ.main(job);
				} catch (Exception e) {
					System.out.println("Fatal error: " + e.toString());
				} finally {
					// activate buttons
					setButtonsEnabled(true);
				}
			}
		}.start();
	}

	private void initComponentsCustom() {
		// Load app version
		final Properties properties = new Properties();
		try {
			properties.load(VMF2OBJ.class.getClassLoader().getResourceAsStream("project.properties"));
		} catch (Exception ignored) {
		}
		setTitle("VMF2OBJ " + properties.getProperty("version"));

		// try {
		// 	URL iconUrl = getClass().getResource("resources/icon.png");
		// 	Image icon = Toolkit.getDefaultToolkit().createImage(iconUrl);
		// 	setIconImage(icon);
		// 	logFrame.setIconImage(icon);
		// } catch (Exception ex) {
		// }
	}

	private void initComponents() {

		tabbedPaneOptions = new javax.swing.JTabbedPane();
		panelFiles = new javax.swing.JPanel();
		labelVMFFile = new javax.swing.JLabel();
		fileInput = new javax.swing.JTextField();
		buttonSelect = new javax.swing.JButton();
		labelResources = new javax.swing.JLabel();
		scrollResources = new javax.swing.JScrollPane();
		listResources = new javax.swing.JList<Path>();
		buttonAdd = new javax.swing.JButton();
		buttonAddFolder = new javax.swing.JButton();
		buttonRemove = new javax.swing.JButton();
		buttonRemoveAll = new javax.swing.JButton();
		labelDnDTip = new javax.swing.JLabel();
		panelSettings = new javax.swing.JPanel();
		checkBoxQuietMode = new javax.swing.JCheckBox();
		checkBoxSkipToolBrushes = new javax.swing.JCheckBox();
		buttonConvert = new javax.swing.JButton();
		logScrollPane = new javax.swing.JScrollPane();
		logTextArea = new javax.swing.JTextArea();

		setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
		setLocationByPlatform(true);
		setSize(500, 500);

		listResources.setModel(getFilesModel());
		scrollResources.setViewportView(listResources);

		fileInput.setEditable(false);
		buttonSelect.setText("Select Map");
		buttonSelect.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonSelectActionPerformed(evt);
			}
		});

		buttonAdd.setText("Add");
		buttonAdd.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonAddActionPerformed(evt);
			}
		});

		buttonAddFolder.setText("Add Folder");
		buttonAddFolder.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonAddFolderActionPerformed(evt);
			}
		});

		buttonRemove.setText("Remove");
		buttonRemove.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonRemoveActionPerformed(evt);
			}
		});

		buttonRemoveAll.setText("Remove all");
		buttonRemoveAll.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonRemoveAllActionPerformed(evt);
			}
		});

		labelVMFFile.setText("VMF file to convert:");
		labelResources.setText("List of resources (VPK files and external resources):");
		labelDnDTip.setText("Tip: drag and drop files/folders on the boxes above");

		javax.swing.GroupLayout panelFilesLayout = new javax.swing.GroupLayout(panelFiles);
		panelFiles.setLayout(panelFilesLayout);
		panelFilesLayout
				.setHorizontalGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelFilesLayout.createSequentialGroup().addContainerGap().addGroup(panelFilesLayout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addComponent(labelVMFFile)
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFilesLayout.createSequentialGroup()
										.addComponent(fileInput, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(buttonSelect,
												javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
								.addComponent(labelResources)
								.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelFilesLayout.createSequentialGroup()
										.addComponent(scrollResources, javax.swing.GroupLayout.DEFAULT_SIZE, 199, Short.MAX_VALUE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
												.addComponent(buttonAdd, javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(buttonAddFolder, javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(buttonRemove, javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
												.addComponent(buttonRemoveAll, javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
								.addComponent(labelDnDTip)).addContainerGap()));
		panelFilesLayout.setVerticalGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(panelFilesLayout.createSequentialGroup().addContainerGap().addComponent(labelVMFFile)
						.addGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(fileInput, javax.swing.GroupLayout.DEFAULT_SIZE, 22, 22)
								.addGroup(panelFilesLayout.createSequentialGroup().addComponent(buttonSelect)))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED).addComponent(labelResources)
						.addGroup(panelFilesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(scrollResources, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
								.addGroup(panelFilesLayout.createSequentialGroup().addComponent(buttonAdd)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(buttonAddFolder)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(buttonRemove)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(buttonRemoveAll)))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(labelDnDTip)
						.addContainerGap()));

		tabbedPaneOptions.addTab("Files", panelFiles);

		checkBoxQuietMode.setText("Quiet Mode");
		checkBoxQuietMode.setToolTipText("Suppress Warnings");
		checkBoxQuietMode.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkBoxQuietModeActionPerformed(evt);
			}
		});

		checkBoxSkipToolBrushes.setText("Skip Tool Brushes");
		checkBoxSkipToolBrushes.setToolTipText("Any Tool brushes will not be included in the converted OBJ file");
		checkBoxSkipToolBrushes.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				checkBoxSkipToolBrushesActionPerformed(evt);
			}
		});

		javax.swing.GroupLayout panelTexturesLayout = new javax.swing.GroupLayout(panelSettings);
		panelSettings.setLayout(panelTexturesLayout);
		panelTexturesLayout
				.setHorizontalGroup(panelTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelTexturesLayout.createSequentialGroup().addContainerGap()
								.addGroup(panelTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(checkBoxSkipToolBrushes).addComponent(checkBoxQuietMode))));
		panelTexturesLayout
				.setVerticalGroup(panelTexturesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(panelTexturesLayout.createSequentialGroup().addContainerGap().addComponent(checkBoxQuietMode)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(checkBoxSkipToolBrushes)));

		tabbedPaneOptions.addTab("Settings", panelSettings);

		buttonConvert.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
		buttonConvert.setEnabled(false);
		buttonConvert.setText("Convert");
		buttonConvert.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				buttonConvertActionPerformed(evt);
			}
		});

		logScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Log"));

		logTextArea.setEditable(false);
		logTextArea.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
		logTextArea.setRows(1);
		logTextArea.setDisabledTextColor(new java.awt.Color(0, 0, 0));
		logScrollPane.setViewportView(logTextArea);
		logHandler = new TextAreaHandler(logTextArea);
		logHandler.setFormatter(new SimpleFormatter());
		logger.addHandler(logHandler);

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(
						layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(
										layout.createSequentialGroup().addContainerGap()
												.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
														.addComponent(tabbedPaneOptions, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
														.addGroup(layout.createSequentialGroup()
																.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 151,
																		Short.MAX_VALUE)
																.addComponent(buttonConvert))
														.addComponent(logScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 662, Short.MAX_VALUE))
												.addContainerGap()));
		layout
				.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup().addContainerGap()
								.addComponent(tabbedPaneOptions).addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(
										layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(buttonConvert))
								.addComponent(logScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 662, Short.MAX_VALUE)
								.addContainerGap()));
	}

	private void buttonSelectActionPerformed(java.awt.event.ActionEvent evt) {
		File vmfFile = new File(fileInput.getText());

		File newvmfFile = openVMFFileDialog(vmfFile);

		if (newvmfFile == null) {
			return;
		}

		fileInput.setText(newvmfFile.getAbsolutePath());
		job.file = new VMFFileEntry(newvmfFile);

		validateConvertButtonEnabled();
	}

	private void buttonAddActionPerformed(java.awt.event.ActionEvent evt) {
		File vpkFile = null;

		if (listResourcesModel.size() == 1) {
			vpkFile = listResourcesModel.firstElement().toFile();
		}

		File[] vpkFiles = openVPKFileDialog(vpkFile);

		if (vpkFiles == null) {
			return;
		}

		for (File file : vpkFiles) {
			listResourcesModel.addElement(file.toPath());
		}

		validateConvertButtonEnabled();
	}

	private void buttonAddFolderActionPerformed(java.awt.event.ActionEvent evt) {
		File vpkFile = null;

		if (listResourcesModel.size() == 1) {
			vpkFile = listResourcesModel.firstElement().toFile();
		}

		File dir = selectDirectoryDialog(vpkFile);

		if (dir == null) {
			return;
		}

		listResourcesModel.addElement(dir.toPath());

		validateConvertButtonEnabled();
	}

	private void buttonRemoveActionPerformed(java.awt.event.ActionEvent evt) {
		int[] selected = listResources.getSelectedIndices();
		listResources.clearSelection();

		for (int index : selected) {
			listResourcesModel.remove(index);
		}

		validateConvertButtonEnabled();
	}

	private void buttonRemoveAllActionPerformed(java.awt.event.ActionEvent evt) {
		listResourcesModel.clear();
		buttonConvert.setEnabled(false);
	}

	private void checkBoxQuietModeActionPerformed(java.awt.event.ActionEvent evt) {
		job.SuppressWarnings = checkBoxQuietMode.isSelected();
	}

	private void checkBoxSkipToolBrushesActionPerformed(java.awt.event.ActionEvent evt) {
		job.skipTools = checkBoxSkipToolBrushes.isSelected();
	}

	private void buttonConvertActionPerformed(java.awt.event.ActionEvent evt) {
		VMFFileEntry entry = job.file;
		File vmfFile = saveVmfFileDialog(new File(entry.outPath));

		if (vmfFile == null) {
			return;
		}

		entry.setOutpath(vmfFile.getAbsolutePath());

		startConversion();
	}

	private javax.swing.JButton buttonAdd;
	private javax.swing.JButton buttonAddFolder;
	private javax.swing.JButton buttonConvert;
	private javax.swing.JButton buttonRemove;
	private javax.swing.JButton buttonRemoveAll;
	private javax.swing.JCheckBox checkBoxQuietMode;
	private javax.swing.JCheckBox checkBoxSkipToolBrushes;
	private javax.swing.JLabel labelDnDTip;
	private javax.swing.JList<Path> listResources;
	private javax.swing.JPanel panelFiles;
	private javax.swing.JPanel panelSettings;
	private javax.swing.JLabel labelVMFFile;
	private javax.swing.JTextField fileInput;
	private javax.swing.JLabel labelResources;
	private javax.swing.JScrollPane scrollResources;
	private javax.swing.JButton buttonSelect;
	private javax.swing.JTabbedPane tabbedPaneOptions;
	private javax.swing.JScrollPane logScrollPane;
	private javax.swing.JTextArea logTextArea;
}