package weatherclient;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FontFormatException;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import org.freixas.jcalendar.JCalendar;

/**
 * Alkalmazás alaposztálya.
 *
 * @author imruf84
 */
public final class WeatherClientFrame extends javax.swing.JFrame {

    /**
     * Soros port kimenete.
     */
    static private OutputStream out = null;
    /**
     * Adatbázis.
     */
    private static WeatherDataBase wdb = null;
    /**
     * Letöltés ablaka.
     */
    private static DownloadDataFrame ddf = null;
    /**
     * Adatok táblázata.
     */
    private static WeatherDataTable dataTable;
    /**
     * Napok listája.
     */
    private static DaysList daysList;
    /**
     * Igaz esetén ablakos módban dolgozunk.
     */
    private static boolean guiMode = false;
    /**
     * Időzítő a folyamatos adatletöltéshez.
     */
    private final Timer getCurrentDataThread;
    /**
     * Van-e jelenleg adatforgalom?
     */
    private static final AtomicReference<Boolean> IS_DATA_CHANEL_ACTIVE = new AtomicReference<>(false);
    /**
     * Iránytű.
     */
    private static Compass compass;

    /**
     * Creates new form MainFrame
     *
     * @throws java.lang.ClassNotFoundException kivétel
     * @throws java.sql.SQLException kivétel
     * @throws java.awt.FontFormatException kivétel
     * @throws java.io.IOException kivétel
     */
    public WeatherClientFrame() throws ClassNotFoundException, SQLException, FontFormatException, IOException {
        wdb = new WeatherDataBase();
        initComponents();

        // Aktuális adatok letöltésének folyamata.
        getCurrentDataThread = new Timer();
        getCurrentDataThread.schedule(new TimerTask() {
            @Override
            public void run() {

                if (currentDataMI.isSelected() && !IS_DATA_CHANEL_ACTIVE.get()) {
                    try {
                        sendMessage("gcd");
                    } catch (IOException ex) {
                        Dialogs.error(ex.getLocalizedMessage());
                    }
                }
            }
        }, 0, (1000 * 2));

        dataTable = new WeatherDataTable(wdb.getConnection());
        dataTable.getSelectionModel().addListSelectionListener((ListSelectionEvent lse) -> {
            if (lse.getValueIsAdjusting()) {
                return;
            }
            if (!(dataTable.getSelectedRowCount() > 0)) {
                return;
            }

            try {
                WeatherListData d = dataTable.getWeatherData(dataTable.getSelectedRow());
                if (null == d) {
                    return;
                }

                compass.setWeatherData(d);

            } catch (SQLException ex) {
                Dialogs.showErrorMessageDialog(this, ex.getLocalizedMessage());
            }
        });
        dataScrollPane.setViewportView(dataTable);

        compass = new Compass();
        renderPanel.add(compass, BorderLayout.CENTER);

        daysList = new DaysList(wdb.getConnection());
        daysListScrollPane.setViewportView(daysList);
        daysList.refresh();

        setLocationRelativeTo(null);
    }

    /**
     * Téma beállítása.
     *
     * @throws FontFormatException kivétel
     * @throws IOException kivétel
     */
    public static void setLookAndFeel() throws FontFormatException, IOException {
        // Téma beállítása.
        javax.swing.plaf.metal.MetalLookAndFeel.setCurrentTheme(new MyMetalTheme());
        // Az ablakkeret az operációs rendszeré szeretnénk, hogy legyen.
        JFrame.setDefaultLookAndFeelDecorated(false);
        // Egyes témák esetében az alapértelmezett Enter leütés nem csinál semmit, ezért engedélyezzük külön.
        UIManager.getLookAndFeelDefaults().put("Button.defaultButtonFollowsFocus", Boolean.TRUE);
        // Görgetősávok témájának megváltoztatása sajátra, mert a lila szerintem túl gagyi.
        UIManager.getLookAndFeelDefaults().put("ScrollBarUI", "weatherclient.SimpleScrollBarUI");
        // Folyamatjelző felirata legyen fekete.
        UIManager.put("ProgressBar.selectionForeground", Color.BLACK);
        UIManager.put("ProgressBar.selectionBackground", Color.BLACK);

    }

    /**
     * Soros portok lekérdezése.
     *
     * @return soros portok
     */
    public static HashSet<CommPortIdentifier> getAvailableSerialPorts() {
        HashSet<CommPortIdentifier> h = new HashSet<>();
        Enumeration thePorts = CommPortIdentifier.getPortIdentifiers();
        while (thePorts.hasMoreElements()) {
            CommPortIdentifier com = (CommPortIdentifier) thePorts.nextElement();
            switch (com.getPortType()) {
                case CommPortIdentifier.PORT_SERIAL:
                    try {
                        CommPort thePort = com.open("CommUtil", 50);
                        thePort.close();
                        h.add(com);
                    } catch (PortInUseException e) {
                        System.out.println("Port, " + com.getName() + ", is in use.");
                    } catch (Exception e) {
                        Dialogs.error("Failed to open port " + com.getName());
                        Dialogs.error(e.getLocalizedMessage());
                    }
            }
        }
        return h;
    }

    /**
     * Üzenet küldése soros porton.
     *
     * @param msg üzenet szövege
     * @return sikeres küldés esetén igaz, egyébként hamis
     * @throws IOException kivétel
     */
    boolean sendMessage(String msg) throws IOException {
        if (null == out) {
            return false;
        }

        out.write((msg + "\n").getBytes("UTF-8"));
        out.flush();

        return true;
    }

    /**
     * Adatok frissítése.
     */
    public static void refreshData() {
        if (!guiMode) {
            return;
        }

        try {
            dataTable.refresh();
            daysList.refresh();
        } catch (SQLException ex) {
            Dialogs.showErrorMessageDialog(null, ex.getLocalizedMessage());
        }
    }

    /**
     * Csatlakozás soros porton keresztül.
     *
     * @param portName port
     * @throws Exception kivétel
     */
    static void connect(String portName, boolean guiMode) throws Exception {

        if (portName.isEmpty()) {
            return;
        }

        CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
        if (portIdentifier.isCurrentlyOwned()) {
            String s = "A kiválasztott port jelenleg használatban van:" + portName;
            if (guiMode) {
                Dialogs.showErrorMessageDialog(null, s);
            } else {
                Dialogs.error(s);
            }
        } else {
            CommPort commPort = portIdentifier.open(WeatherClientFrame.class.getName(), 2000);

            if (commPort instanceof SerialPort) {
                SerialPort serialPort = (SerialPort) commPort;
                serialPort.setSerialPortParams(9600, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);

                InputStream in = serialPort.getInputStream();
                out = serialPort.getOutputStream();

                (new Thread(new SerialReader(in))).start();
                (new Thread(new SerialWriter(out))).start();

            } else {
                String s = "Nem soros port: " + commPort.getName();
                if (guiMode) {
                    Dialogs.showErrorMessageDialog(null, s);
                } else {
                    Dialogs.error(s);
                }
            }
        }
    }

    /**
     * Soros port olvasó.
     */
    public static class SerialReader implements Runnable {

        /**
         * Bemenet.
         */
        InputStream in;
        /**
         * Adatcsomag részeit tartalmazó karakterlánc.
         */
        String currentData = "";
        /**
         * Adatsorra várakozás indikátora.
         */
        boolean waitingForStoredData = false;

        /**
         * Konstruktor.
         *
         * @param in bemenet
         */
        public SerialReader(InputStream in) {
            this.in = in;
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];

            int len;
            String s = "";
            try {
                while ((len = this.in.read(buffer)) > -1) {

                    for (int i = 0; i < len; i++) {
                        char c = (char) buffer[i];

                        // Sorvégével nem foglalkozunk.
                        if (10 == c || 13 == c) {
                            continue;
                        }

                        s += c;

                        // Adat további része érkezett.
                        if (currentData.startsWith("#")) {
                            currentData += c;
                            // Adat vége érkezett.
                            if (currentData.endsWith("&")) {
                                // Ha tárolt adat érkezett, akkor kiírjuk a megfelelő ablakba.
                                WeatherData data = new WeatherData(currentData);
                                if (waitingForStoredData) {
                                    String s2 = currentData.replaceAll("\n", "") + "\n";
                                    if (guiMode) {
                                        ddf.write(s2);
                                    } else {
                                        System.out.print(s2);
                                    }
                                    wdb.storeData(data);
                                } else {
                                    // Egyébként megjelenítjük az információs sávban.
                                    if (guiMode) {
                                        currentDataLabel.setText(data.toString());
                                    } else {
                                        System.out.println(data.toString());
                                    }
                                    IS_DATA_CHANEL_ACTIVE.set(false);
                                    // Ha nincs a táblázatban kijelölt adat, akkor ezt jelenítjük meg.
                                    if (guiMode && !(dataTable.getSelectedRowCount() > 0)) {
                                        compass.setWeatherData(data.toWeatherListData());
                                    }
                                }
                                currentData = "";
                                s = "";
                            }
                            continue;
                        }
                        // Adat első része érkezett.
                        if ('#' == c) {
                            currentData = "" + c;
                            continue;
                        }

                        // Adatsor fog érkezni.
                        if (s.contains("[DATA_BEGIN]")) {

                            // Várunk míg nem lesz szabad az adatkapcsolat.
                            IS_DATA_CHANEL_ACTIVE.set(true);

                            waitingForStoredData = true;
                            if (guiMode) {
                                ddf = new DownloadDataFrame();
                                ddf.setVisible(true);
                            } else {
                                System.out.println("[DATA_BEGIN]");
                            }
                            s = "";
                            currentData = "";
                            continue;
                        }

                        // Nem érkezik több adat.
                        if (s.contains("[DATA_END]")) {
                            s = "";
                            currentData = "";
                            if (guiMode) {
                                ddf.setEnabled(true);
                            } else {
                                System.out.println("[DATA_END]");
                            }
                            waitingForStoredData = false;
                            IS_DATA_CHANEL_ACTIVE.set(false);
                        }
                    }

                }

            } catch (IOException | FontFormatException | ParseException | SQLException ex) {
                Dialogs.showErrorMessageDialog(null, ex.getLocalizedMessage());
            }
        }
    }

    /**
     * oros port író.
     */
    public static class SerialWriter implements Runnable {

        /**
         * Kimenet.
         */
        OutputStream out;

        /**
         * Konstruktor.
         *
         * @param out kimenet
         */
        public SerialWriter(OutputStream out) {
            this.out = out;
        }

        @Override
        public void run() {
            try {
                int c;
                while ((c = System.in.read()) > -1) {
                    this.out.write(c);
                }
            } catch (IOException e) {
                Dialogs.error(e.getLocalizedMessage());
            }
        }
    }

    /**
     * Alkalmazás belépési pontja.
     *
     * @param args argumentumok
     */
    public static void main(String[] args) {
        AtomicReference<String> portToUse = new AtomicReference<>("");

        System.out.println("Searching for devices...");

        guiMode = Arrays.asList(args).contains("-gui");
        for (com.fazecast.jSerialComm.SerialPort comPort : com.fazecast.jSerialComm.SerialPort.getCommPorts()) {

            comPort.openPort();

            System.out.print(comPort.getSystemPortName() + ": ");
            comPort.setComPortTimeouts(com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_BLOCKING, 500, 0);

            int n = 0;

            try {
                while (++n < 30) {
                    byte[] readBuffer = new byte[1024];
                    int numRead = comPort.readBytes(readBuffer, readBuffer.length);

                    if (numRead > 0 && new String(readBuffer).startsWith("wsc")) {
                        System.out.println("OK");
                        portToUse.set(comPort.getSystemPortName());
                        break;
                    }
                }
            } catch (Exception e) {
                Dialogs.error(e.getLocalizedMessage());
            }
            comPort.closePort();
            if (!(portToUse.get().length() > 0)) {
                System.out.println("timeout");
            } else {
                break;
            }
        }

        try {
            WeatherClientFrame.setLookAndFeel();

        } catch (FontFormatException | IOException e) {
            Dialogs.error(e.getLocalizedMessage());
        }

        // Ha nem találtunk csatlakoztatott klienst, akkor jelezzük.
        if (portToUse.get().isEmpty()) {
            String errorMsg = "Nem található csatlakoztatott eszköz.";
            if (guiMode) {
                Dialogs.showErrorMessageDialog(null, errorMsg);
            } else {
                Dialogs.error(errorMsg);
            }
        }

        // Grafikus felhasználói felület létrehozása ha szükséges.
        if (guiMode) {
            java.awt.EventQueue.invokeLater(() -> {

                // Egyébként mehet a munka.
                try {
                    WeatherClientFrame mf = new WeatherClientFrame();
                    mf.setVisible(true);
                    connect(portToUse.get(), true);
                } catch (Exception ex) {
                    Dialogs.showErrorMessageDialog(null, ex.getLocalizedMessage());
                }
            });
        } else {
            try {
                // Csak parancssori mód.
                wdb = new WeatherDataBase();
                connect(portToUse.get(), false);
            } catch (Exception ex) {
                Dialogs.error(ex.getLocalizedMessage());
            }
        }
    }

    /**
     * Legutolsó tárolt adat utáni adatok lekérdezése.
     *
     * @throws IOException kivétel
     * @throws SQLException kivétel
     */
    public void downloadLatestData() throws IOException, SQLException {
        String s = "d" + wdb.getLatestDateTime();
        sendMessage(s);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        dataScrollPane = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jSplitPane2 = new javax.swing.JSplitPane();
        filtersPanel = new javax.swing.JPanel();
        daysPanel = new javax.swing.JPanel();
        daysListScrollPane = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        filterButtonsPanel = new javax.swing.JPanel();
        applyFiltersButton = new javax.swing.JButton();
        clearSelectionButton = new javax.swing.JButton();
        renderPanel = new javax.swing.JPanel();
        infoPanel = new javax.swing.JPanel();
        currentDataLabel = new javax.swing.JLabel();
        mainMenuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        quitMenuItem = new javax.swing.JMenuItem();
        connectionMenu = new javax.swing.JMenu();
        downloadDataMenuItem = new javax.swing.JMenuItem();
        currentDataMI = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        setTimeMenuItem = new javax.swing.JMenuItem();
        calibrationMenu = new javax.swing.JMenu();
        setRefDirMenuItem = new javax.swing.JMenuItem();
        setRefSpeedMenuItem = new javax.swing.JMenuItem();
        dataMenu = new javax.swing.JMenu();
        removeDataMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Időjárás kliens v0.1");
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getClassLoader().getResource("weatherclient/icons/MainFrameIcon.png")));
        setMinimumSize(new java.awt.Dimension(500, 300));
        setPreferredSize(new java.awt.Dimension(900, 700));

        jSplitPane1.setDividerLocation(550);
        jSplitPane1.setDividerSize(3);
        jSplitPane1.setResizeWeight(0.8);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        dataScrollPane.setViewportView(jTable1);

        jSplitPane1.setLeftComponent(dataScrollPane);

        jSplitPane2.setDividerLocation(200);
        jSplitPane2.setDividerSize(3);
        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane2.setResizeWeight(0.8);

        filtersPanel.setLayout(new java.awt.BorderLayout());

        daysPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Napok"));
        daysPanel.setLayout(new java.awt.BorderLayout());

        daysListScrollPane.setViewportView(jList1);

        daysPanel.add(daysListScrollPane, java.awt.BorderLayout.CENTER);

        filtersPanel.add(daysPanel, java.awt.BorderLayout.CENTER);

        applyFiltersButton.setText("Alkalmaz");
        applyFiltersButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyFiltersButtonActionPerformed(evt);
            }
        });
        filterButtonsPanel.add(applyFiltersButton);

        clearSelectionButton.setText("Kijelölés törlése");
        clearSelectionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSelectionButtonActionPerformed(evt);
            }
        });
        filterButtonsPanel.add(clearSelectionButton);

        filtersPanel.add(filterButtonsPanel, java.awt.BorderLayout.PAGE_END);

        jSplitPane2.setLeftComponent(filtersPanel);

        renderPanel.setBackground(new java.awt.Color(255, 255, 255));
        renderPanel.setLayout(new java.awt.BorderLayout());
        jSplitPane2.setRightComponent(renderPanel);

        jSplitPane1.setRightComponent(jSplitPane2);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        infoPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED));
        infoPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        currentDataLabel.setText("-");
        infoPanel.add(currentDataLabel);

        getContentPane().add(infoPanel, java.awt.BorderLayout.SOUTH);

        fileMenu.setText("Fájl");

        quitMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.CTRL_MASK));
        quitMenuItem.setText("Kilépés");
        quitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(quitMenuItem);

        mainMenuBar.add(fileMenu);

        connectionMenu.setText("Kapcsolat");

        downloadDataMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        downloadDataMenuItem.setText("Adatok letöltése...");
        downloadDataMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                downloadDataMenuItemActionPerformed(evt);
            }
        });
        connectionMenu.add(downloadDataMenuItem);

        currentDataMI.setSelected(true);
        currentDataMI.setText("Aktuális adatok folyamatos lekérdezése");
        connectionMenu.add(currentDataMI);
        connectionMenu.add(jSeparator1);

        setTimeMenuItem.setText("Idő beállítása...");
        setTimeMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTimeMenuItemActionPerformed(evt);
            }
        });
        connectionMenu.add(setTimeMenuItem);

        calibrationMenu.setText("Kalibrálás");

        setRefDirMenuItem.setText("Irány kalibrálása...");
        setRefDirMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setRefDirMenuItemActionPerformed(evt);
            }
        });
        calibrationMenu.add(setRefDirMenuItem);

        setRefSpeedMenuItem.setText("Sebesség kalibrálása...");
        setRefSpeedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setRefSpeedMenuItemActionPerformed(evt);
            }
        });
        calibrationMenu.add(setRefSpeedMenuItem);

        connectionMenu.add(calibrationMenu);

        mainMenuBar.add(connectionMenu);

        dataMenu.setText("Adat");

        removeDataMenuItem.setText("Törlés...");
        removeDataMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeDataMenuItemActionPerformed(evt);
            }
        });
        dataMenu.add(removeDataMenuItem);

        mainMenuBar.add(dataMenu);

        setJMenuBar(mainMenuBar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void quitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quitMenuItemActionPerformed
        dispose();
        System.exit(0);
    }//GEN-LAST:event_quitMenuItemActionPerformed

    private void downloadDataMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_downloadDataMenuItemActionPerformed
        try {
            downloadLatestData();
        } catch (IOException | SQLException ex) {
            Dialogs.showErrorMessageDialog(this, ex.getLocalizedMessage());
        }
    }//GEN-LAST:event_downloadDataMenuItemActionPerformed

    private void applyFiltersButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyFiltersButtonActionPerformed
        dataTable.setDaysFilter(daysList.getSelectedDays());
        refreshData();
    }//GEN-LAST:event_applyFiltersButtonActionPerformed

    private void setTimeMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setTimeMenuItemActionPerformed
        JPanel p = new JPanel(new GridLayout(0, 1));

        JCalendar calendar = new JCalendar(JCalendar.DISPLAY_DATE | JCalendar.DISPLAY_TIME, false);
        p.add(calendar);

        Object[] o = {"Beállít", "Mégsem"};
        int n = JOptionPane.showOptionDialog(
                this,
                p,
                "Dátum és idő beállítása",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.DEFAULT_OPTION,
                null, o, calendar);

        if (JOptionPane.YES_OPTION == n) {
            try {
                sendMessage("t" + new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getDate()));
            } catch (IOException ex) {
                Dialogs.showErrorMessageDialog(this, ex.getLocalizedMessage());
            }
        }

    }//GEN-LAST:event_setTimeMenuItemActionPerformed

    private void setRefDirMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setRefDirMenuItemActionPerformed
        JPanel p = new JPanel(new GridLayout(0, 2));

        JLabel l = new JLabel("Referencia irány:");
        p.add(l);
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0, -360, 360, 1));
        p.add(sp);

        Object[] o = {"Beállít", "Mégsem"};
        int n = JOptionPane.showOptionDialog(
                this,
                p,
                "Irány kalibrálása",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.DEFAULT_OPTION,
                null, o, sp);

        if (JOptionPane.YES_OPTION == n) {
            try {
                sendMessage("dr" + sp.getValue());
            } catch (IOException ex) {
                Dialogs.showErrorMessageDialog(this, ex.getLocalizedMessage());
            }
        }
    }//GEN-LAST:event_setRefDirMenuItemActionPerformed

    private void setRefSpeedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_setRefSpeedMenuItemActionPerformed
        JPanel p = new JPanel(new GridLayout(0, 2));

        JLabel l = new JLabel("Referencia sebesség: ");
        p.add(l);
        SpinnerNumberModel model = new SpinnerNumberModel(0.0d, 0.0d, 100.0d, 1.0d);
        JSpinner sp = new JSpinner(model);
        JSpinner.NumberEditor editor = (JSpinner.NumberEditor) sp.getEditor();
        DecimalFormat format = editor.getFormat();
        format.setMinimumFractionDigits(3);
        p.add(sp);

        Object[] o = {"Beállít", "Mégsem"};
        int n = JOptionPane.showOptionDialog(
                this,
                p,
                "Sebesség kalibrálása",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.DEFAULT_OPTION,
                null, o, sp);

        if (JOptionPane.YES_OPTION == n) {
            try {
                sendMessage("sr" + sp.getValue());
            } catch (IOException ex) {
                Dialogs.showErrorMessageDialog(this, ex.getLocalizedMessage());
            }
        }
    }//GEN-LAST:event_setRefSpeedMenuItemActionPerformed

    private void removeDataMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeDataMenuItemActionPerformed
        JPanel p = new JPanel(new GridLayout(0, 2));

        p.add(new JLabel("Mettől:"));
        JSpinner spFrom = new JSpinner(new SpinnerDateModel());
        spFrom.setEditor(new JSpinner.DateEditor(spFrom, "yyyy.MM.dd"));
        p.add(spFrom);

        p.add(new JLabel("Meddig:"));
        JSpinner spTo = new JSpinner(new SpinnerDateModel());
        spTo.setEditor(new JSpinner.DateEditor(spTo, "yyyy.MM.dd"));
        p.add(spTo);

        p.add(new JLabel("Törlés az eszközről:"));
        JCheckBox deviceCB = new JCheckBox();
        p.add(deviceCB);

        p.add(new JLabel("Törlés az adatbázisból:"));
        JCheckBox dbCB = new JCheckBox();
        p.add(dbCB);

        Object[] o = {"Töröl", "Mégsem"};
        int n = JOptionPane.showOptionDialog(
                this,
                p,
                "Adatok törlése",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.DEFAULT_OPTION,
                null, o, spFrom);

        if (JOptionPane.YES_OPTION == n) {
            try {

                if (JOptionPane.showConfirmDialog(null, "Valóban törölni kívánja az adatokat?", "Törlés", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                    if (dbCB.isSelected()) {
                        daysList.clearSelection();
                        dataTable.setDaysFilter(daysList.getSelectedDays());
                        wdb.removeData(new SimpleDateFormat("yyyy-MM-dd").format(spFrom.getValue()), new SimpleDateFormat("yyyy-MM-dd").format(spTo.getValue()));
                        refreshData();
                    }
                    if (deviceCB.isSelected()) {
                        sendMessage("r" + new SimpleDateFormat("yyMMdd").format(spFrom.getValue()) + new SimpleDateFormat("yyMMdd").format(spTo.getValue()));
                    }
                }
            } catch (IOException | SQLException ex) {
                Dialogs.showErrorMessageDialog(this, ex.getLocalizedMessage());
            }
        }
    }//GEN-LAST:event_removeDataMenuItemActionPerformed

    private void clearSelectionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSelectionButtonActionPerformed
        dataTable.clearSelection();
    }//GEN-LAST:event_clearSelectionButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton applyFiltersButton;
    private javax.swing.JMenu calibrationMenu;
    private javax.swing.JButton clearSelectionButton;
    private javax.swing.JMenu connectionMenu;
    private static javax.swing.JLabel currentDataLabel;
    private javax.swing.JCheckBoxMenuItem currentDataMI;
    private javax.swing.JMenu dataMenu;
    private javax.swing.JScrollPane dataScrollPane;
    private javax.swing.JScrollPane daysListScrollPane;
    private javax.swing.JPanel daysPanel;
    private javax.swing.JMenuItem downloadDataMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JPanel filterButtonsPanel;
    private javax.swing.JPanel filtersPanel;
    private javax.swing.JPanel infoPanel;
    private javax.swing.JList<String> jList1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenuItem quitMenuItem;
    private javax.swing.JMenuItem removeDataMenuItem;
    private javax.swing.JPanel renderPanel;
    private javax.swing.JMenuItem setRefDirMenuItem;
    private javax.swing.JMenuItem setRefSpeedMenuItem;
    private javax.swing.JMenuItem setTimeMenuItem;
    // End of variables declaration//GEN-END:variables
}
