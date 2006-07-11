package com.webreach.mirth.client.ui;

import com.webreach.mirth.client.core.ClientException;
import com.webreach.mirth.client.ui.editors.filter.FilterPane;
import com.webreach.mirth.client.ui.editors.transformer.TransformerPane;
import com.webreach.mirth.model.Channel;
import com.webreach.mirth.model.Connector;
import com.webreach.mirth.model.Filter;
import com.webreach.mirth.model.Transformer;
import com.webreach.mirth.model.Transport;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AlternateRowHighlighter;
import org.jdesktop.swingx.decorator.HighlighterPipeline;
import java.util.Map.Entry;

public class ChannelSetup extends javax.swing.JPanel
{
    public Channel currentChannel;
    public String lastIndex = "";
    
    private int index;
    private Frame parent;
    private boolean isDeleting = false;
    private boolean loadingChannel = false;
    private JXTable jTable1;
    private JScrollPane jScrollPane4;
    private Map<String,Transport> transports;
    private TransformerPane transformerPane;
    private FilterPane filterPane;

    public ChannelSetup()
    {
        this.parent = PlatformUI.MIRTH_FRAME;
        initComponents();
        
        channelView.addMouseListener(new java.awt.event.MouseAdapter() 
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                showChannelEditPopupMenu(evt, false);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                showChannelEditPopupMenu(evt, false);
            }
        });
        jScrollPane4 = new JScrollPane();
        ArrayList<String> sourceConnectors;
        ArrayList<String> destinationConnectors;
        transformerPane = new TransformerPane();
        filterPane = new FilterPane();
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try
        {
            transports = this.parent.mirthClient.getTransports();
            sourceConnectors = new ArrayList<String>();
            destinationConnectors = new ArrayList<String>();
            Iterator i=transports.entrySet().iterator();
            while(i.hasNext())
            {
               Entry entry = (Entry)i.next();
               
               if(transports.get(entry.getKey()).getType() == Transport.Type.LISTENER)
                   sourceConnectors.add(transports.get(entry.getKey()).getName());
               
               else if(transports.get(entry.getKey()).getType() == Transport.Type.SENDER)
                   destinationConnectors.add(transports.get(entry.getKey()).getName());
            }
            
            sourceSourceDropdown.setModel(new javax.swing.DefaultComboBoxModel(sourceConnectors.toArray()));
            destinationSourceDropdown.setModel(new javax.swing.DefaultComboBoxModel(destinationConnectors.toArray()));
        }
        catch(ClientException e)
        {
            parent.alertException(e.getStackTrace());
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        index = -1;
        channelView.setMaximumSize(new Dimension(450, 3000));
    }
    
    private void showChannelEditPopupMenu(java.awt.event.MouseEvent evt, boolean onTable)
    {
        if (evt.isPopupTrigger())
        {
            if (onTable)
            {
                int row = jTable1.rowAtPoint(new Point(evt.getX(), evt.getY()));
                jTable1.setRowSelectionInterval(row, row);
            }
            parent.channelEditPopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }
    
    public void editTransformer()
    {
        if (channelView.getSelectedIndex() == 1)
            transformerPane.load(currentChannel.getSourceConnector().getTransformer());
        
        else if (channelView.getSelectedIndex() == 2)
        {
            if (currentChannel.getMode() == Channel.Mode.APPLICATION)
                transformerPane.load(currentChannel.getDestinationConnectors().get(0).getTransformer());
            else
            {
                int destination = getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")));
                transformerPane.load(currentChannel.getDestinationConnectors().get(destination).getTransformer());
            }
        }
    }
    
    public void editFilter()
    {
        if (channelView.getSelectedIndex() == 1)
            filterPane.load(currentChannel.getSourceConnector().getFilter());
        
        else if (channelView.getSelectedIndex() == 2)
        {
            if (currentChannel.getMode() == Channel.Mode.APPLICATION)
                filterPane.load(currentChannel.getDestinationConnectors().get(0).getFilter());
            else
            {
                int destination = getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")));
                filterPane.load(currentChannel.getDestinationConnectors().get(destination).getFilter());
            }
        }
    }
    
    public void makeDestinationTable(boolean addNew)
    {
        List<Connector> dc;
        Object[][] tableData;
        int tableSize;

        dc = currentChannel.getDestinationConnectors();
        tableSize = dc.size();
        if(addNew)
            tableSize++;
        tableData = new Object[tableSize][2];
        for (int i=0; i < tableSize; i++)
        {
            if(tableSize-1 == i && addNew)
            {
                Connector c = makeNewConnector();
                c.setName(getNewDestinationName(tableSize));
                c.setTransportName((String)destinationSourceDropdown.getItemAt(0));

                tableData[i][0] = c.getName();
                tableData[i][1] = c.getTransportName();

                dc.add(c);
            }
            else
            {
                tableData[i][0] = dc.get(i).getName();
                tableData[i][1] = dc.get(i).getTransportName();
            }
        }

        jTable1 = new JXTable();

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            tableData,
            new String []
                {
                    "Destination", "Connector Type"
                }
            )
            {
                boolean[] canEdit = new boolean []
                {
                    true, false
                };

                public boolean isCellEditable(int rowIndex, int columnIndex)
                {
                    return canEdit [columnIndex];
                }
        });
        
        jTable1.getColumnModel().getColumn(jTable1.getColumnModel().getColumnIndex("Destination")).setCellEditor(new MyTableCellEditor());

        jTable1.setSelectionMode(0);
        jTable1.setRowSelectionAllowed(true);
        jTable1.setRowHeight(20);
        jTable1.setFocusable(false); // Need to figure a way to make the arrows work here because the pane that shows up steals the focus
        
        jTable1.setOpaque(true);
        
        if(Preferences.systemNodeForPackage(Mirth.class).getBoolean("highlightRows", true))
        {
            HighlighterPipeline highlighter = new HighlighterPipeline();
            highlighter.addHighlighter(new AlternateRowHighlighter(UIConstants.HIGHLIGHTER_COLOR, UIConstants.BACKGROUND_COLOR, UIConstants.TITLE_TEXT_COLOR));
            ((JXTable)jTable1).setHighlighters(highlighter);
        }

        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            public void valueChanged(ListSelectionEvent evt)
            {
                if (!evt.getValueIsAdjusting())
                {
                    int last = getLastIndex(lastIndex);
                    if (last != -1 && last != jTable1.getRowCount() && !isDeleting)
                    {
                        int connectorIndex = getDestinationConnector((String)jTable1.getValueAt(last,getColumnNumber("Destination")));
                        Connector destinationConnector = currentChannel.getDestinationConnectors().get(connectorIndex);
                        destinationConnector.setProperties(connectorClass2.getProperties());
                    }

                    if(!loadConnector())
                    {
                        if(getLastIndex(lastIndex) == jTable1.getRowCount())
                            jTable1.setRowSelectionInterval(last-1,last-1);
                        else
                            jTable1.setRowSelectionInterval(last,last);
                    }
                    else
                    {
                        lastIndex = ((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")));
                    }
                }
            }
        });
        
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() 
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                showChannelEditPopupMenu(evt, true);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                showChannelEditPopupMenu(evt, true);
            }
        });

        int last = getLastIndex(lastIndex);
        if (addNew)
            jTable1.setRowSelectionInterval(jTable1.getRowCount()-1, jTable1.getRowCount()-1);
        else if (last == -1)
            jTable1.setRowSelectionInterval(0,0);       // Makes sure the event is called when the table is created.
        else if(last == jTable1.getRowCount())
            jTable1.setRowSelectionInterval(last-1,last-1);
        else
            jTable1.setRowSelectionInterval(last,last);
        jScrollPane4.setViewportView(jTable1);
        
        jScrollPane4.addMouseListener(new java.awt.event.MouseAdapter() 
        {
            public void mousePressed(java.awt.event.MouseEvent evt)
            {
                showChannelEditPopupMenu(evt, false);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt)
            {
                showChannelEditPopupMenu(evt, false);
            }
        });
        
    }
    
    private int getLastIndex(String destinationName)
    {
        for (int i = 0; i < jTable1.getRowCount(); i++)
        {
            if(((String)jTable1.getValueAt(i,getColumnNumber("Destination"))).equalsIgnoreCase(destinationName))
                return i;
        }
        return -1;
    }

    private String getNewDestinationName(int size)
    {
        String temp = "Destination ";

        for(int i = 1; i<=size; i++)
        {
            boolean exists = false;
            for(int j = 0; j < size-1; j++)
            {
                if(((String)jTable1.getValueAt(j,getColumnNumber("Destination"))).equalsIgnoreCase(temp + i))
                {
                    exists = true;
                }
            }
            if(!exists)
                return temp + i;
        }
        return "";
    }

    private int getColumnNumber(String name)
    {
        for (int i = 0; i < jTable1.getColumnCount(); i++)
        {
            if (jTable1.getColumnName(i).equalsIgnoreCase(name))
                return i;
        }
        return -1;
    }

    public int getSelectedDestination()
    {
        if(jTable1.isEditing())
            return jTable1.getEditingRow();
        else
            return jTable1.getSelectedRow();
    }

    private int getDestinationConnector(String destinationName)
    {
        List<Connector> dc = currentChannel.getDestinationConnectors();
        for(int i = 0; i<dc.size(); i++)
        {
            if(dc.get(i).getName().equalsIgnoreCase(destinationName))
                return i;
        }
        return -1;
    }

    public boolean loadConnector()
    {
        List<Connector> dc;
        String destinationName;
        
        if(getSelectedDestination() != -1)
            destinationName = (String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination"));
        else
            return false;
        
        if(currentChannel != null && currentChannel.getDestinationConnectors() != null)
        {
            dc = currentChannel.getDestinationConnectors();
            for(int i = 0; i<dc.size(); i++)
            {
                if(dc.get(i).getName().equalsIgnoreCase(destinationName))
                {
                    boolean visible = parent.channelEditTasks.getContentPane().getComponent(0).isVisible();
                    destinationSourceDropdown.setSelectedItem(dc.get(i).getTransportName());
                    parent.channelEditTasks.getContentPane().getComponent(0).setVisible(visible);
                    return true;
                }
            }
        }
        return false;
    }

    public void editChannel(int index)
    {
        loadingChannel = true;
        this.index = index;
        lastIndex = "";
        currentChannel = parent.channels.get(index);
        
        channelView.setSelectedComponent(summary);
        
        loadChannelInfo();
        
        if(currentChannel.getMode() == Channel.Mode.ROUTER || currentChannel.getMode() == Channel.Mode.BROADCAST)
            makeDestinationTable(false);
        else
            generateSingleDestinationPage();
        
        setSourceVariableList();
        setDestinationVariableList();
        loadingChannel = false;
    }

    public void addChannel(Channel channel)
    {
        loadingChannel = true;
        index = -1;
        lastIndex = "";
        currentChannel = channel;
        
        channelView.setSelectedComponent(summary);
        
        Connector sourceConnector = makeNewConnector();
	sourceConnector.setName("sourceConnector");
        sourceConnector.setTransportName((String)sourceSourceDropdown.getItemAt(0));
        sourceConnector.setProperties(connectorClass1.getProperties());
        
        currentChannel.setSourceConnector(sourceConnector);
                
        if(currentChannel.getMode() == Channel.Mode.APPLICATION)
        {
            List<Connector> dc;
            dc = currentChannel.getDestinationConnectors();

            Connector c = makeNewConnector();
            c.setName("Destination");
            c.setTransportName((String)destinationSourceDropdown.getItemAt(0));
            dc.add(c);
        }
        
        loadChannelInfo();
        
        if(currentChannel.getMode() == Channel.Mode.ROUTER || currentChannel.getMode() == Channel.Mode.BROADCAST)
            makeDestinationTable(true);
        else
            generateSingleDestinationPage();
        
        setSourceVariableList();
        setDestinationVariableList();
        
        saveChanges();
        loadingChannel = false;
    }

    private void loadChannelInfo()
    {
        summaryNameField.setText(currentChannel.getName());
        summaryDescriptionText.setText(currentChannel.getDescription());
        if (currentChannel.getDirection().equals(Channel.Direction.INBOUND))
            summaryDirectionLabel2.setText("Inbound");
        else if (currentChannel.getDirection().equals(Channel.Direction.OUTBOUND))
        {
            summaryDirectionLabel2.setText("Outbound");
            currentChannel.setMode(Channel.Mode.ROUTER);
        }

        if (currentChannel.getMode().equals(Channel.Mode.APPLICATION))
            summaryPatternLabel2.setText("Application");
        else if (currentChannel.getMode().equals(Channel.Mode.BROADCAST))
            summaryPatternLabel2.setText("Broadcast");
        else if (currentChannel.getMode().equals(Channel.Mode.ROUTER))
            summaryPatternLabel2.setText("Router");

        if (currentChannel.isEnabled())
            summaryEnabledCheckbox.setSelected(true);
        else
            summaryEnabledCheckbox.setSelected(false);
        
        if(((String)currentChannel.getProperties().get("recv_xml_encoded")) != null && ((String)currentChannel.getProperties().get("recv_xml_encoded")).equalsIgnoreCase("true"))
            xmlPreEncoded.setSelected(true);
        else
            xmlPreEncoded.setSelected(false);

        boolean visible = parent.channelEditTasks.getContentPane().getComponent(0).isVisible();

        sourceSourceDropdown.setSelectedItem(currentChannel.getSourceConnector().getTransportName());
        
        if (currentChannel.getMode().equals(Channel.Mode.APPLICATION))
        {
            destinationSourceDropdown.setSelectedItem(currentChannel.getDestinationConnectors().get(0).getTransportName());
        }
        
        parent.channelEditTasks.getContentPane().getComponent(0).setVisible(visible);

    }

    public boolean saveChanges()
    {
        if (summaryNameField.getText().equals(""))
        {
            JOptionPane.showMessageDialog(parent, "Channel name cannot be empty.");
                return false;
        }
        if (!currentChannel.getName().equals(summaryNameField.getText()))
        {
            if(!parent.checkChannelName(summaryNameField.getText()))
                return false;
        }
        
        currentChannel.getSourceConnector().setProperties(connectorClass1.getProperties());
        
        Connector temp;
        if(currentChannel.getMode() == Channel.Mode.APPLICATION)
            temp = currentChannel.getDestinationConnectors().get(0);
        else
            temp = currentChannel.getDestinationConnectors().get(getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination"))));
        temp.setProperties(connectorClass2.getProperties());

        currentChannel.setName(summaryNameField.getText());
        currentChannel.setDescription(summaryDescriptionText.getText());
        currentChannel.setEnabled(summaryEnabledCheckbox.isSelected());
        
        if(xmlPreEncoded.isSelected())
            currentChannel.getProperties().put("recv_xml_encoded", "true");
        else
            currentChannel.getProperties().put("recv_xml_encoded", "false");
        
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try
        {
            if(index == -1)
            {
                index = parent.channels.size();
                currentChannel.setId(parent.mirthClient.getNextId());
            }

            parent.updateChannel(currentChannel);
            currentChannel = parent.channels.get(index);
            parent.channelListPage.makeChannelTable();
        }
        catch (ClientException e)
        {
            parent.alertException(e.getStackTrace());
        }
        this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        
        return true;
    }

    public void addNewDestination()
    {
         makeDestinationTable(true);
         parent.channelEditTasks.getContentPane().getComponent(0).setVisible(true);
    }

    public void deleteDestination()
    {
        isDeleting = true;
        List<Connector> dc = currentChannel.getDestinationConnectors();
        if(dc.size() == 1)
        {
            JOptionPane.showMessageDialog(parent, "You must have at least one destination.");
            return;
        }
        
        dc.remove(getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination"))));
        makeDestinationTable(false);
        parent.channelEditTasks.getContentPane().getComponent(0).setVisible(true);
        isDeleting = false;
    }

    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents()
    {
        filterButtonGroup = new javax.swing.ButtonGroup();
        validationButtonGroup = new javax.swing.ButtonGroup();
        channelView = new javax.swing.JTabbedPane();
        summary = new javax.swing.JPanel();
        summaryNameLabel = new javax.swing.JLabel();
        summaryDescriptionLabel = new javax.swing.JLabel();
        summaryNameField = new com.webreach.mirth.client.ui.MirthTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        summaryDescriptionText = new com.webreach.mirth.client.ui.MirthTextArea();
        summaryDirectionLabel1 = new javax.swing.JLabel();
        summaryDirectionLabel2 = new javax.swing.JLabel();
        summaryPatternLabel1 = new javax.swing.JLabel();
        summaryPatternLabel2 = new javax.swing.JLabel();
        summaryEnabledCheckbox = new com.webreach.mirth.client.ui.MirthCheckBox();
        xmlPreEncoded = new com.webreach.mirth.client.ui.MirthCheckBox();
        source = new javax.swing.JPanel();
        sourceSourceDropdown = new com.webreach.mirth.client.ui.MirthComboBox();
        sourceSourceLabel = new javax.swing.JLabel();
        connectorClass1 = new com.webreach.mirth.client.ui.ConnectorClass();
        variableList1 = new com.webreach.mirth.client.ui.VariableList();
        destination = new javax.swing.JPanel();
        destinationSourceDropdown = new com.webreach.mirth.client.ui.MirthComboBox();
        destinationSourceLabel = new javax.swing.JLabel();
        connectorClass2 = new com.webreach.mirth.client.ui.ConnectorClass();
        variableList2 = new com.webreach.mirth.client.ui.VariableList();

        channelView.setFocusable(false);
        summary.setBackground(new java.awt.Color(255, 255, 255));
        summary.setFocusable(false);
        summary.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentShown(java.awt.event.ComponentEvent evt)
            {
                summaryComponentShown(evt);
            }
        });

        summaryNameLabel.setText("Channel Name:");

        summaryDescriptionLabel.setText("Channel Description:");

        summaryDescriptionText.setColumns(20);
        summaryDescriptionText.setRows(5);
        summaryDescriptionText.setText("Channel Description");
        jScrollPane2.setViewportView(summaryDescriptionText);

        summaryDirectionLabel1.setText("Direction:");

        summaryDirectionLabel2.setText("Outbound");

        summaryPatternLabel1.setText("Pattern:");

        summaryPatternLabel2.setText("Application Integration");

        summaryEnabledCheckbox.setBackground(new java.awt.Color(255, 255, 255));
        summaryEnabledCheckbox.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        summaryEnabledCheckbox.setText("Enabled");
        summaryEnabledCheckbox.setMargin(new java.awt.Insets(0, 0, 0, 0));

        xmlPreEncoded.setBackground(new java.awt.Color(255, 255, 255));
        xmlPreEncoded.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        xmlPreEncoded.setText("Channel will receive XML pre-encoded HL7 messages.");
        xmlPreEncoded.setMargin(new java.awt.Insets(0, 0, 0, 0));

        org.jdesktop.layout.GroupLayout summaryLayout = new org.jdesktop.layout.GroupLayout(summary);
        summary.setLayout(summaryLayout);
        summaryLayout.setHorizontalGroup(
            summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(summaryLayout.createSequentialGroup()
                .addContainerGap()
                .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(summaryLayout.createSequentialGroup()
                        .add(26, 26, 26)
                        .add(summaryNameLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(summaryNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 150, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(20, 20, 20)
                        .add(summaryEnabledCheckbox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(summaryLayout.createSequentialGroup()
                        .add(51, 51, 51)
                        .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                            .add(summaryPatternLabel1)
                            .add(summaryDirectionLabel1))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(summaryDirectionLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 78, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(summaryPatternLabel2)))
                    .add(summaryLayout.createSequentialGroup()
                        .add(summaryDescriptionLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 285, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(xmlPreEncoded, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(170, Short.MAX_VALUE))
        );
        summaryLayout.setVerticalGroup(
            summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(summaryLayout.createSequentialGroup()
                .addContainerGap()
                .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(summaryNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(summaryNameLabel)
                    .add(summaryEnabledCheckbox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(summaryDirectionLabel1)
                    .add(summaryDirectionLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(summaryPatternLabel1)
                    .add(summaryPatternLabel2))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(summaryLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(summaryDescriptionLabel)
                    .add(jScrollPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 166, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(xmlPreEncoded, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(284, Short.MAX_VALUE))
        );
        channelView.addTab("Summary", summary);

        source.setBackground(new java.awt.Color(255, 255, 255));
        source.setFocusable(false);
        source.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentShown(java.awt.event.ComponentEvent evt)
            {
                sourceComponentShown(evt);
            }
        });

        sourceSourceDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TCP/IP", "Database", "Email" }));
        sourceSourceDropdown.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                sourceSourceDropdownActionPerformed(evt);
            }
        });

        sourceSourceLabel.setText("Connector Type:");

        org.jdesktop.layout.GroupLayout connectorClass1Layout = new org.jdesktop.layout.GroupLayout(connectorClass1);
        connectorClass1.setLayout(connectorClass1Layout);
        connectorClass1Layout.setHorizontalGroup(
            connectorClass1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 356, Short.MAX_VALUE)
        );
        connectorClass1Layout.setVerticalGroup(
            connectorClass1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 497, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout sourceLayout = new org.jdesktop.layout.GroupLayout(source);
        source.setLayout(sourceLayout);
        sourceLayout.setHorizontalGroup(
            sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(sourceLayout.createSequentialGroup()
                .addContainerGap()
                .add(sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, sourceLayout.createSequentialGroup()
                        .add(connectorClass1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(variableList1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(sourceLayout.createSequentialGroup()
                        .add(sourceSourceLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sourceSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        sourceLayout.setVerticalGroup(
            sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(sourceLayout.createSequentialGroup()
                .addContainerGap()
                .add(sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(sourceSourceLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(sourceSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(connectorClass1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(variableList1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE))
                .addContainerGap())
        );
        channelView.addTab("Source", source);

        destination.setBackground(new java.awt.Color(255, 255, 255));
        destination.setFocusable(false);
        destination.addComponentListener(new java.awt.event.ComponentAdapter()
        {
            public void componentShown(java.awt.event.ComponentEvent evt)
            {
                destinationComponentShown(evt);
            }
        });

        destinationSourceDropdown.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "TCP/IP", "Database", "Email" }));
        destinationSourceDropdown.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                destinationSourceDropdownActionPerformed(evt);
            }
        });

        destinationSourceLabel.setText("Connector Type:");

        org.jdesktop.layout.GroupLayout connectorClass2Layout = new org.jdesktop.layout.GroupLayout(connectorClass2);
        connectorClass2.setLayout(connectorClass2Layout);
        connectorClass2Layout.setHorizontalGroup(
            connectorClass2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 356, Short.MAX_VALUE)
        );
        connectorClass2Layout.setVerticalGroup(
            connectorClass2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 497, Short.MAX_VALUE)
        );

        org.jdesktop.layout.GroupLayout destinationLayout = new org.jdesktop.layout.GroupLayout(destination);
        destination.setLayout(destinationLayout);
        destinationLayout.setHorizontalGroup(
            destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(destinationLayout.createSequentialGroup()
                .addContainerGap()
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, destinationLayout.createSequentialGroup()
                        .add(connectorClass2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(variableList2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(destinationLayout.createSequentialGroup()
                        .add(destinationSourceLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(destinationSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        destinationLayout.setVerticalGroup(
            destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, destinationLayout.createSequentialGroup()
                .addContainerGap()
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(destinationSourceLabel)
                    .add(destinationSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(connectorClass2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(variableList2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE))
                .addContainerGap())
        );
        channelView.addTab("Destinations", destination);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(channelView, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 573, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(channelView)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void summaryComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_summaryComponentShown
    {//GEN-HEADEREND:event_summaryComponentShown
        parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 1, 4, false);
    }//GEN-LAST:event_summaryComponentShown

    private void sourceComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_sourceComponentShown
    {//GEN-HEADEREND:event_sourceComponentShown
        if(currentChannel.getMode() == Channel.Mode.ROUTER)
        {
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 1, 4, false);
        }
        else
        {
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 1, 2, false);
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 3, 4, true);
        }
    }//GEN-LAST:event_sourceComponentShown

    private void destinationComponentShown(java.awt.event.ComponentEvent evt)//GEN-FIRST:event_destinationComponentShown
    {//GEN-HEADEREND:event_destinationComponentShown
        if(currentChannel.getMode() == Channel.Mode.APPLICATION)
        {
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 1, 2, false);
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 3, 4, true);
        }
        else if(currentChannel.getMode() == Channel.Mode.BROADCAST)
        {
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 1, 2, true);
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 3, 4, false);
        }
        else
            parent.setVisibleTasks(parent.channelEditTasks, parent.channelEditPopupMenu, 1, 4, true);
    }//GEN-LAST:event_destinationComponentShown

    private void sourceSourceDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sourceSourceDropdownActionPerformed
        
        if (!loadingChannel)
        {
            if (connectorClass1.getName() != null && connectorClass1.getName().equals((String)sourceSourceDropdown.getSelectedItem()))
                return;

            if (!compareProps(connectorClass1.getProperties(), connectorClass1.getDefaults()) || 
                    currentChannel.getSourceConnector().getFilter().getRules().size() > 0 ||
                    currentChannel.getSourceConnector().getTransformer().getSteps().size() > 0)
            {
                boolean changeType = parent.alertOption("Are you sure you would like to change this connector type and lose all of the current connector data?");
                if (!changeType)
                {
                    sourceSourceDropdown.setSelectedItem(connectorClass1.getProperties().get("DataType"));
                    return;
                }
            }
        }
        
        for(int i=0; i<parent.sourceConnectors.size(); i++)
        {
            if(parent.sourceConnectors.get(i).getName().equalsIgnoreCase((String)sourceSourceDropdown.getSelectedItem()))
            {
                connectorClass1 = parent.sourceConnectors.get(i);
            }
        }

        Connector sourceConnector = currentChannel.getSourceConnector();
        if(sourceConnector != null)
        {
            String dataType = sourceConnector.getProperties().getProperty("DataType");
            if (dataType == null)
                dataType = "";

            if (sourceConnector.getProperties().size() == 0 || !dataType.equals((String)sourceSourceDropdown.getSelectedItem()))
            {
                String name = sourceConnector.getName();
                sourceConnector = makeNewConnector();
                sourceConnector.setName(name);
                connectorClass1.setProperties(connectorClass1.getDefaults());
                sourceConnector.setProperties(connectorClass1.getProperties());
            }

            sourceConnector.setTransportName((String)sourceSourceDropdown.getSelectedItem());
            currentChannel.setSourceConnector(sourceConnector);
            connectorClass1.setProperties(sourceConnector.getProperties());
        }
        
        variableList1.setVariableList(sourceConnector.getTransformer().getSteps());
        
        source.removeAll();
        
        org.jdesktop.layout.GroupLayout sourceLayout = (org.jdesktop.layout.GroupLayout)source.getLayout();
        source.setLayout(sourceLayout);
        sourceLayout.setHorizontalGroup(
            sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(sourceLayout.createSequentialGroup()
                .addContainerGap()
                .add(sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, sourceLayout.createSequentialGroup()
                        .add(connectorClass1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(variableList1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(sourceLayout.createSequentialGroup()
                        .add(sourceSourceLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sourceSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        sourceLayout.setVerticalGroup(
            sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(sourceLayout.createSequentialGroup()
                .addContainerGap()
                .add(sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(sourceSourceLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 15, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(sourceSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(sourceLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(connectorClass1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(variableList1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE))
                .addContainerGap())
        );
        
        source.updateUI();
    }//GEN-LAST:event_sourceSourceDropdownActionPerformed

    private void destinationSourceDropdownActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_destinationSourceDropdownActionPerformed
    {//GEN-HEADEREND:event_destinationSourceDropdownActionPerformed
        if(currentChannel.getMode() == Channel.Mode.ROUTER || currentChannel.getMode() == Channel.Mode.BROADCAST)
        {
            if (!loadingChannel)
            {
                if (connectorClass2.getName() != null && connectorClass2.getName().equals((String)destinationSourceDropdown.getSelectedItem()) && lastIndex.equals((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination"))))
                    return;

                // if the selected destination is still the same AND the default properties/transformer/filter have 
                // not been changed from defaults then ask if the user would like to really change connector type.
                if (lastIndex.equals((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination"))) && (!compareProps(connectorClass2.getProperties(), connectorClass2.getDefaults()) || 
                    currentChannel.getDestinationConnectors().get(getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")))).getFilter().getRules().size() > 0 ||
                    currentChannel.getDestinationConnectors().get(getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")))).getTransformer().getSteps().size() > 0))
                {
                    boolean changeType = parent.alertOption("Are you sure you would like to change this connector type and lose all of the current connector data?");
                    if (!changeType)
                    {
                        destinationSourceDropdown.setSelectedItem(connectorClass2.getProperties().get("DataType"));
                        return;
                    }
                }
            }
            generateMultipleDestinationPage();
        }
        else
        {
            if (!loadingChannel)
            {
                if (connectorClass2.getName() != null && connectorClass2.getName().equals((String)destinationSourceDropdown.getSelectedItem()))
                    return;
                if (!compareProps(connectorClass2.getProperties(), connectorClass2.getDefaults()) || 
                    currentChannel.getDestinationConnectors().get(0).getFilter().getRules().size() > 0 ||
                    currentChannel.getDestinationConnectors().get(0).getTransformer().getSteps().size() > 0)
                {
                    boolean changeType = parent.alertOption("Are you sure you would like to change this connector type and lose all of the current connector data?");
                    if (!changeType)
                    {
                        destinationSourceDropdown.setSelectedItem(connectorClass2.getProperties().get("DataType"));
                        return;
                    }
                }
            }
            generateSingleDestinationPage();
        }
    }//GEN-LAST:event_destinationSourceDropdownActionPerformed

    public void generateMultipleDestinationPage()
    {
        for(int i=0; i<parent.destinationConnectors.size(); i++)
        {
            if(parent.destinationConnectors.get(i).getName().equalsIgnoreCase((String)destinationSourceDropdown.getSelectedItem()))
                connectorClass2 = parent.destinationConnectors.get(i);
        }

        List<Connector> dc = currentChannel.getDestinationConnectors();
        int connectorIndex = getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")));
        Connector destinationConnector = dc.get(connectorIndex);
                
        String dataType = destinationConnector.getProperties().getProperty("DataType");
        if (dataType == null)
            dataType = "";

        //System.out.println(destinationConnector.getTransportName() + " " + (String)destinationSourceDropdown.getSelectedItem());
        
        // set to defaults on first load of connector or if it has changed types.
        if (destinationConnector.getProperties().size() == 0 || !dataType.equals((String)destinationSourceDropdown.getSelectedItem()))
        {
            String name = destinationConnector.getName();
            destinationConnector = makeNewConnector();
            destinationConnector.setName(name);
            connectorClass2.setProperties(connectorClass2.getDefaults());
            destinationConnector.setProperties(connectorClass2.getProperties());    
        }
        
        destinationConnector.setTransportName((String)destinationSourceDropdown.getSelectedItem());
        dc.set(connectorIndex, destinationConnector);
        
        if (!((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Connector Type"))).equals(destinationConnector.getTransportName()) && getSelectedDestination() != -1)
            jTable1.setValueAt((String)destinationSourceDropdown.getSelectedItem(),getSelectedDestination(),getColumnNumber("Connector Type"));
        
//        System.out.println(destinationConnector.getProperties().toString());
        connectorClass2.setProperties(destinationConnector.getProperties());
        
        variableList2.setVariableList(destinationConnector.getTransformer().getSteps());
        
        destination.removeAll();
        
        org.jdesktop.layout.GroupLayout destinationLayout = (org.jdesktop.layout.GroupLayout)destination.getLayout();
        destination.setLayout(destinationLayout);
        destinationLayout.setHorizontalGroup(
            destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, destinationLayout.createSequentialGroup()
                .addContainerGap()
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jScrollPane4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 610, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, destinationLayout.createSequentialGroup()
                        .add(destinationSourceLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(destinationSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, destinationLayout.createSequentialGroup()
                        .add(connectorClass2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(variableList2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap())
        );
        destinationLayout.setVerticalGroup(
            destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(destinationLayout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 143, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(14, 14, 14)
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(destinationSourceLabel)
                    .add(destinationSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(connectorClass2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(variableList2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE))
                .addContainerGap())
        );

        destination.updateUI();
    }

    public void generateSingleDestinationPage()
    {
        for(int i=0; i<parent.destinationConnectors.size(); i++)
        {
            if(parent.destinationConnectors.get(i).getName().equalsIgnoreCase((String)destinationSourceDropdown.getSelectedItem()))
                connectorClass2 = parent.destinationConnectors.get(i);
        }
        /*
        if (currentChannel.getDestinationConnectors().size() == 1 && currentChannel.getDestinationConnectors().get(0).getTransportName().equals(connectorClass2.getName()))
            connectorClass2.setProperties(currentChannel.getDestinationConnectors().get(0).getProperties());
        */
        int connectorIndex = 0;
        Connector destinationConnector = currentChannel.getDestinationConnectors().get(connectorIndex);
        if(destinationConnector != null)
        {
            String dataType = destinationConnector.getProperties().getProperty("DataType");
            if (dataType == null)
                dataType = "";

            if (destinationConnector.getProperties().size() == 0 || !dataType.equals((String)destinationSourceDropdown.getSelectedItem()))
            {
                String name = destinationConnector.getName();
                destinationConnector = makeNewConnector();
                destinationConnector.setName(name);
                connectorClass2.setProperties(connectorClass2.getDefaults());
                destinationConnector.setProperties(connectorClass2.getProperties());
            }

            destinationConnector.setTransportName((String)destinationSourceDropdown.getSelectedItem());
            currentChannel.getDestinationConnectors().set(connectorIndex, destinationConnector);
            connectorClass2.setProperties(destinationConnector.getProperties());
        }
                
        destination.removeAll();

        org.jdesktop.layout.GroupLayout destinationLayout = (org.jdesktop.layout.GroupLayout)destination.getLayout();
        destination.setLayout(destinationLayout);
        destinationLayout.setHorizontalGroup(
            destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(destinationLayout.createSequentialGroup()
                .addContainerGap()
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, destinationLayout.createSequentialGroup()
                        .add(connectorClass2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(variableList2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(destinationLayout.createSequentialGroup()
                        .add(destinationSourceLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(destinationSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 110, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        destinationLayout.setVerticalGroup(
            destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, destinationLayout.createSequentialGroup()
                .addContainerGap()
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(destinationSourceLabel)
                    .add(destinationSourceDropdown, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(destinationLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(connectorClass2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(variableList2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 497, Short.MAX_VALUE))
                .addContainerGap())
        );

        destination.updateUI();
    }
    
    public void setSourceVariableList()
    {
        variableList1.setVariableList(currentChannel.getSourceConnector().getTransformer().getSteps());
        variableList1.repaint();
    }    
    
    public void setDestinationVariableList()
    {
        if (currentChannel.getMode() == Channel.Mode.APPLICATION)
            variableList2.setVariableList(currentChannel.getDestinationConnectors().get(0).getTransformer().getSteps());
        else
        {
            int destination = getDestinationConnector((String)jTable1.getValueAt(getSelectedDestination(),getColumnNumber("Destination")));
            variableList2.setVariableList(currentChannel.getDestinationConnectors().get(destination).getTransformer().getSteps());
        }
        variableList2.repaint();
    }

    public Connector makeNewConnector()
    {
        Connector c = new Connector();
        Transformer dt = new Transformer();
        Filter df = new Filter();
        
        c.setTransformer(dt);
        c.setFilter(df);
        return c;
    }
    public ConnectorClass getSourceConnector(){
    	return connectorClass1;
    }

    public void showSaveButton()
    {
        parent.channelEditTasks.getContentPane().getComponent(0).setVisible(true);
    }
    
    private boolean compareProps(Properties p1, Properties p2)
    {
        Enumeration<?> propertyKeys = p1.propertyNames();
        while (propertyKeys.hasMoreElements())
        {
            String key = (String)propertyKeys.nextElement();
            if (!p1.getProperty(key).equals(p2.getProperty(key)))
                return false;
        }
        return true;
    }
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTabbedPane channelView;
    private com.webreach.mirth.client.ui.ConnectorClass connectorClass1;
    private com.webreach.mirth.client.ui.ConnectorClass connectorClass2;
    private javax.swing.JPanel destination;
    private com.webreach.mirth.client.ui.MirthComboBox destinationSourceDropdown;
    private javax.swing.JLabel destinationSourceLabel;
    private javax.swing.ButtonGroup filterButtonGroup;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel source;
    private com.webreach.mirth.client.ui.MirthComboBox sourceSourceDropdown;
    private javax.swing.JLabel sourceSourceLabel;
    private javax.swing.JPanel summary;
    private javax.swing.JLabel summaryDescriptionLabel;
    private com.webreach.mirth.client.ui.MirthTextArea summaryDescriptionText;
    private javax.swing.JLabel summaryDirectionLabel1;
    private javax.swing.JLabel summaryDirectionLabel2;
    private com.webreach.mirth.client.ui.MirthCheckBox summaryEnabledCheckbox;
    private com.webreach.mirth.client.ui.MirthTextField summaryNameField;
    private javax.swing.JLabel summaryNameLabel;
    private javax.swing.JLabel summaryPatternLabel1;
    private javax.swing.JLabel summaryPatternLabel2;
    private javax.swing.ButtonGroup validationButtonGroup;
    private com.webreach.mirth.client.ui.VariableList variableList1;
    private com.webreach.mirth.client.ui.VariableList variableList2;
    private com.webreach.mirth.client.ui.MirthCheckBox xmlPreEncoded;
    // End of variables declaration//GEN-END:variables

}
