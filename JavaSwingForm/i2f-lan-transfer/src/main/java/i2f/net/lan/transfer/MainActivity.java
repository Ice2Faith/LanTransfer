package i2f.net.lan.transfer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.*;

/**
 * @author ltb
 * @date 2022/2/28 9:03
 * @desc
 */
public class MainActivity extends JFrame {
    public static void main(String[] args){
        new MainActivity().setVisible(true);
    }

    public static final int SERVER_PORT=63321;

    private JTextField edtServerPort;
    private JCheckBox ckbRunServer;
    private JButton btnScanLan;

    private JComboBox spnLocalAddress;
    private JComboBox spnLanAddress;
    private DefaultComboBoxModel spnLocalAddressModel;
    private DefaultComboBoxModel spnLanAddressModel;

    private JCheckBox ckbConnect;

    private JTextField edtConnectIp;
    private JTextField edtConnectPort;

    private JList lstMsg;
    private DefaultListModel lstMsgModel;

    private JButton btnSendFile;
    private JButton btnCleanMsg;
    private JTextArea edtMessage;
    private JCheckBox ckbAutoClean;
    private JButton btnSendMessage;

    private NetworkBackgroundService svc;


    public MainActivity(){
        initComponents();
        initEvents();
        initActivity();
    }

    private void initActivity(){
        svc=new NetworkBackgroundService(this);
        NetworkBackgroundService.serverStatus(svc);
        NetworkBackgroundService.clientStatus(svc);
        NetworkBackgroundService.scanLanAddressCache(svc);
    }

    protected int perWid(Container parent,double per){
        double ret=parent.getSize().getWidth()*(per/100.0);
        return (int)ret;
    }

    protected int perHei(Container parent,double per){
        double ret=parent.getSize().getHeight()*(per/100.0);
        return (int)ret;
    }

    protected void perSize(Container parent,Container component,double perW,double perH){
        component.setSize(perWid(parent,perW),perHei(parent,perH));
        component.setPreferredSize(new Dimension(perWid(parent,perW),perHei(parent,perH)));
    }

    class MsgItem{
        public static final int TYPE_ERROR=0;
        public static final int TYPE_LOG=1;

        public static final int TYPE_SERVER_RECV=10;
        public static final int TYPE_CLIENT_RECV=11;
        public String msg;
        public int type;
        public MsgItem(){}
        public MsgItem(int type,String msg){
            this.type=type;
            this.msg=msg;
        }
    }
    private LinkedList<MsgItem> list=new LinkedList<>();

    private Map<String, Set<String>> ipAddress=new HashMap<>();
    private Set<String> lanIpAddress=new HashSet<>();

    public static final String KEY_RECEIVER_CMD = "cmd";

    public static final String KEY_STATUS="status";
    public static final String KEY_SCAN_LAN_RESULT="scan_lan_result";
    public static final String KEY_TEXT="text";
    public static final String KEY_IP="ip";
    public static final String KEY_PORT="port";

    public static final int CMD_NULL=0;

    public static final int CMD_SERVER_STATUS = 0x101;
    public static final int CMD_CLIENT_STATUS = 0x102;
    public static final int CMD_LAN_SCAN_RESULT = 0x103;

    public static final int CMD_SYSTEM_ERROR=0x200;
    public static final int CMD_SYSTEM_LOG=0x201;
    public static final int CMD_SERVER_RECV=0x202;
    public static final int CMD_CLIENT_RECV=0x203;

    private LanAddressAdapter lanAdapter=new LanAddressAdapter();
    private LocalAddressAdapter localAdapter=new LocalAddressAdapter();
    private MessageAdapter msgAdapter=new MessageAdapter();


    protected void initComponents(){
        setSize(1080,720);
        setTitle("LanTransfer");
        setDefaultLookAndFeelDecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new GridLayout(3,1));

        JPanel topArea=new JPanel();
        topArea.setLayout(new BoxLayout(topArea,BoxLayout.Y_AXIS));
        getContentPane().add(topArea);

        JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayout(0,3));
        topArea.add(panel1);

        edtServerPort=new JTextField();
        edtServerPort.setBackground(Color.decode("#00ff00"));
        panel1.add(edtServerPort);

        ckbRunServer =new JCheckBox("启用主机");
        ckbRunServer.setBackground(Color.decode("#00ff00"));
        panel1.add(ckbRunServer);

        btnScanLan=new JButton("扫描");
        btnScanLan.setBackground(Color.decode("#ff8800"));
        panel1.add(btnScanLan);

        JPanel panel2=new JPanel();
        panel2.setLayout(new GridLayout(0,2));
        topArea.add(panel2);

        spnLocalAddressModel=new DefaultComboBoxModel();
        spnLocalAddress=new JComboBox(spnLocalAddressModel);
        panel2.add(spnLocalAddress);

        spnLanAddressModel=new DefaultComboBoxModel();
        spnLanAddress=new JComboBox(spnLanAddressModel);
        panel2.add(spnLanAddress);

        JPanel panel3=new JPanel();
        panel3.setLayout(new GridLayout(0,3));
        topArea.add(panel3);

        ckbConnect=new JCheckBox("连接");
        ckbConnect.setBackground(Color.decode("#0088ff"));
        panel3.add(ckbConnect);

        edtConnectIp=new JTextField();
        edtConnectIp.setBackground(Color.decode("#0088ff"));
        panel3.add(edtConnectIp);

        edtConnectPort=new JTextField();
        edtConnectPort.setBackground(Color.decode("#0088ff"));
        panel3.add(edtConnectPort);

        JPanel centerArea=new JPanel();
        centerArea.setLayout(new BoxLayout(centerArea,BoxLayout.Y_AXIS));
        getContentPane().add(centerArea);

        JLabel lbRecv=new JLabel("接收");
        centerArea.add(lbRecv);

        JPanel panel5=new JPanel();
        panel5.setLayout(new GridLayout(0,1));
        centerArea.add(panel5);


        lstMsgModel=new DefaultListModel<>();
        lstMsg=new JList(lstMsgModel);
        lstMsg.setCellRenderer(new ListItemRender());
        panel5.add(lstMsg);


        JPanel buttomArea=new JPanel();
        buttomArea.setLayout(new BoxLayout(buttomArea,BoxLayout.Y_AXIS));
        getContentPane().add(buttomArea);

        JLabel lbSend=new JLabel("发送");
        buttomArea.add(lbSend);

        JPanel panel7=new JPanel();
        panel7.setLayout(new GridLayout(0,2));
        buttomArea.add(panel7);

        btnSendFile=new JButton("发送文件");
        btnSendFile.setBackground(Color.decode("#8888ff"));
        panel7.add(btnSendFile);

        btnCleanMsg=new JButton("清空");
        btnCleanMsg.setBackground(Color.decode("#8888ff"));
        panel7.add(btnCleanMsg);

        JPanel panel8=new JPanel();
        panel8.setLayout(new BoxLayout(panel8,BoxLayout.X_AXIS));
        buttomArea.add(panel8);

        edtMessage=new JTextArea();
        panel8.add(edtMessage);

        JPanel panel9=new JPanel();
        panel9.setLayout(new GridLayout(0,1));
        panel8.add(panel9);

        ckbAutoClean=new JCheckBox("清空");
        ckbAutoClean.setBackground(Color.decode("#00aa44"));
        panel9.add(ckbAutoClean);

        btnSendMessage=new JButton("发送");
        btnSendMessage.setBackground(Color.decode("#00aa44"));
        panel9.add(btnSendMessage);

    }

    protected void initEvents(){
        btnSendMessage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBtnSendMessageClick();
            }
        });
        btnSendFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBtnSendFileClicked();
            }
        });
        ckbRunServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBtnRunServerClicked();
            }
        });
        btnScanLan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBtnScanLanClicked();
            }
        });
        ckbConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBtnConnectClicked();
            }
        });
        btnCleanMsg.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onBtnCleanLogClicked();
            }
        });
        spnLocalAddress.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object item=spnLocalAddress.getSelectedItem();
                String ip=String.valueOf(item);
                lanIpAddress.clear();
                lanIpAddress=ipAddress.get(ip);
                if(lanIpAddress==null){
                    lanIpAddress=new HashSet<>();
                }
                lanAdapter.notifyDataSetChanged();
            }
        });
        spnLanAddress.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                Object item=spnLanAddress.getSelectedItem();
                String ip=String.valueOf(item);
                edtConnectIp.setText(ip);
            }
        });
    }

    class ListItemRender extends DefaultListCellRenderer{
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            MsgItem item=(MsgItem)value;
            JLabel ret=new JLabel(item.msg);
            switch (item.type){
                case MsgItem.TYPE_LOG:
                    ret.setForeground(new Color(180,180,180));
                    break;
                case MsgItem.TYPE_ERROR:
                    ret.setForeground(new Color(255,0,0));
                    break;
                case MsgItem.TYPE_SERVER_RECV:
                    ret.setForeground(new Color(0,150,100));
                    break;
                case MsgItem.TYPE_CLIENT_RECV:
                    ret.setForeground(new Color(0,100,150));
                    break;
            }
            return ret;
        }
    }

    interface IDataset{
        void notifyDataSetChanged();
    }

    class LocalAddressAdapter implements IDataset{

        @Override
        public void notifyDataSetChanged() {
            spnLocalAddressModel=new DefaultComboBoxModel();
            for(String item : ipAddress.keySet()){
                spnLocalAddressModel.addElement(item);
            }
            spnLocalAddress.setModel(spnLocalAddressModel);
            if(ipAddress.size()>0){
                spnLocalAddress.setSelectedIndex(0);
                Object item=spnLocalAddress.getSelectedItem();
                String ip=String.valueOf(item);
                lanIpAddress.clear();
                lanIpAddress=ipAddress.get(ip);
                if(lanIpAddress==null){
                    lanIpAddress=new HashSet<>();
                }
            }
        }
    }

    class LanAddressAdapter implements IDataset{

        @Override
        public void notifyDataSetChanged() {
            spnLanAddressModel=new DefaultComboBoxModel();
            for(String item : lanIpAddress){
                spnLanAddressModel.addElement(item);
            }
            spnLanAddress.setModel(spnLanAddressModel);
            if(lanIpAddress.size()>0){
                spnLanAddress.setSelectedIndex(0);
                Object item=spnLanAddress.getSelectedItem();
                String ip=String.valueOf(item);
                edtConnectIp.setText(ip);

            }
        }

    }


    class MessageAdapter implements IDataset{

        @Override
        public void notifyDataSetChanged() {
            lstMsgModel=new DefaultListModel<>();
            for(MsgItem item : list){
                lstMsgModel.addElement(item);
            }
            lstMsg.setModel(lstMsgModel);
        }

    }

    public static void callbackServerStatus(MainActivity act,boolean status,int serverPort){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SERVER_STATUS);
        intent.putExtra(KEY_STATUS,status);
        intent.putExtra(KEY_PORT,serverPort);
        act.sendBroadcast(intent);
    }

    public static void callbackClientStatus(MainActivity act,boolean status,String connectIp,int connecPort){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_CLIENT_STATUS);
        intent.putExtra(KEY_STATUS,status);
        intent.putExtra(KEY_IP,connectIp);
        intent.putExtra(KEY_PORT,connecPort);
        act.sendBroadcast(intent);
    }
    public static void callbackLanScanResult(MainActivity act,Map<String,Set<String>> result){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_LAN_SCAN_RESULT);
        Bundle bundle=new Bundle();
        String[] local=new String[result.size()];
        int i=0;
        for(String item : result.keySet()){
            local[i]=item;
            Set<String> set=result.get(item);
            String[] lan=new String[set.size()];
            int j=0;
            for(String it : set){
                lan[j]=it;
                j++;
            }
            bundle.putStringArray(item,lan);
            i++;
        }
        bundle.putStringArray("local",local);
        intent.putExtra(KEY_SCAN_LAN_RESULT,bundle);
        act.sendBroadcast(intent);
    }

    public static void callbackSystemError(MainActivity act,String text){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SYSTEM_ERROR);
        intent.putExtra(KEY_TEXT,text);
        act.sendBroadcast(intent);
    }

    public static void callbackSystemLog(MainActivity act,String text){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SYSTEM_LOG);
        intent.putExtra(KEY_TEXT,text);
        act.sendBroadcast(intent);
    }

    public static void callbackServerRecv(MainActivity act,String text){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SERVER_RECV);
        intent.putExtra(KEY_TEXT,text);
        act.sendBroadcast(intent);
    }

    public static void callbackClientRecv(MainActivity act,String text){
        Intent intent=new Intent();
        intent.putExtra(KEY_RECEIVER_CMD,CMD_CLIENT_RECV);
        intent.putExtra(KEY_TEXT,text);
        act.sendBroadcast(intent);
    }

    private void onReceiveNullCmd(Intent intent){
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,"empty cmd received.");
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveServerStatusCmd(Intent intent){
        boolean status=intent.getBooleanExtra(KEY_STATUS,false);
        ckbRunServer.setSelected(status);
        int port=intent.getIntExtra(KEY_PORT,SERVER_PORT);
        edtServerPort.setText(port+"");
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,"server status sync.");
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveClientStatusCmd(Intent intent){
        boolean status=intent.getBooleanExtra(KEY_STATUS,false);
        ckbConnect.setSelected(status);
        String ip=intent.getStringExtra(KEY_IP);
        if(ip==null){
            ip="";
        }
        edtConnectIp.setText(ip);
        int port=intent.getIntExtra(KEY_PORT,SERVER_PORT);
        edtConnectPort.setText(port+"");
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,"client status sync.");
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveLanScanResultCmd(Intent intent){
        Bundle result=intent.getBundleExtra(KEY_SCAN_LAN_RESULT);
        String[] local=result.getStringArray("local");
        Map<String,Set<String>> map=new HashMap<>();
        for(String item : local){
            String[] lans=result.getStringArray(item);
            Set<String> set=new HashSet<>();
            for(String it : lans){
                set.add(it);
            }
            map.put(item,set);
        }
        ipAddress=map;
        lanIpAddress.clear();
        localAdapter.notifyDataSetChanged();
        lanAdapter.notifyDataSetChanged();
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,"scan lan address done.");
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveSystemErrorCmd(Intent intent){
        String text=intent.getStringExtra(KEY_TEXT);
        MsgItem item=new MsgItem(MsgItem.TYPE_ERROR,text);
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveSystemLogCmd(Intent intent){
        String text=intent.getStringExtra(KEY_TEXT);
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,text);
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveServerRecvCmd(Intent intent){
        String text=intent.getStringExtra(KEY_TEXT);
        MsgItem item=new MsgItem(MsgItem.TYPE_SERVER_RECV,text);
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveClientRecvCmd(Intent intent){
        String text=intent.getStringExtra(KEY_TEXT);
        MsgItem item=new MsgItem(MsgItem.TYPE_CLIENT_RECV,text);
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void sendBroadcast(Intent intent){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                dispatchReceiverIntent(intent);
            }
        });
    }
    private int getIntentCmd(Intent intent){
        return intent.getIntExtra(KEY_RECEIVER_CMD,CMD_NULL);
    }
    private void dispatchReceiverIntent(Intent intent){
        int cmd=getIntentCmd(intent);
        switch (cmd){
            case CMD_NULL:
                onReceiveNullCmd(intent);
                break;
            case CMD_SERVER_STATUS:
                onReceiveServerStatusCmd(intent);
                break;
            case CMD_CLIENT_STATUS:
                onReceiveClientStatusCmd(intent);
                break;
            case CMD_LAN_SCAN_RESULT:
                onReceiveLanScanResultCmd(intent);
                break;
            case CMD_SYSTEM_ERROR:
                onReceiveSystemErrorCmd(intent);
                break;
            case CMD_SYSTEM_LOG:
                onReceiveSystemLogCmd(intent);
                break;
            case CMD_SERVER_RECV:
                onReceiveServerRecvCmd(intent);
                break;
            case CMD_CLIENT_RECV:
                onReceiveClientRecvCmd(intent);
                break;
        }
    }

    void sendText(String text){
        NetworkBackgroundService.sendString(svc,text);
    }

    void sendFile(File file){
        NetworkBackgroundService.sendFile(svc,file.getAbsolutePath());
    }

    public void onBtnRunServerClicked() {
        boolean ck=ckbRunServer.isSelected();
        if(ck){
            String sport=edtServerPort.getText().toString();
            Integer port=SERVER_PORT;
            try{
                port=Integer.parseInt(sport);
                if(port<1 || port>65535){
                    throw new NumberFormatException("port only allow in range[1-65535] rather is "+port);
                }
            }catch (Exception e){
                callbackSystemError(this,"run server on port error:"+e.getMessage()+" of "+e.getClass().getName());
                return;
            }
            NetworkBackgroundService.runServer(svc,port);
        }else{
            NetworkBackgroundService.stopServer(svc);
        }
    }

    public void onBtnScanLanClicked() {
        String sport=edtServerPort.getText().toString();
        Integer port=SERVER_PORT;
        try{
            port=Integer.parseInt(sport);
            if(port<1 || port>65535){
                throw new NumberFormatException("port only allow in range[1-65535] rather is "+port);
            }
        }catch (Exception e){
            callbackSystemError(this,"run server on port error:"+e.getMessage()+" of "+e.getClass().getName());
            return;
        }
        NetworkBackgroundService.scanLanAddress(svc,port);
    }

    public void onBtnConnectClicked() {
        boolean ck=ckbConnect.isSelected();
        if(ck){
            Object obj=spnLanAddress.getSelectedItem();
            String ip=this.edtConnectIp.getText().toString();
            String sport=this.edtConnectPort.getText().toString();
            Integer port=SERVER_PORT;
            try{
                port=Integer.parseInt(sport);
                if(port<1 || port>65535){
                    throw new NumberFormatException("port only allow in range[1-65535] rather is "+port);
                }
            }catch (Exception e){
                callbackSystemError(this,"connect server port error:"+e.getMessage()+" of "+e.getClass().getName());
                return;
            }
            NetworkBackgroundService.connectServer(svc,ip,port);
        }else{
            NetworkBackgroundService.disconnectServer(svc);
        }
    }

    public void onBtnSendFileClicked() {
        JFileChooser chooser = new JFileChooser();             //设置选择器
        chooser.setMultiSelectionEnabled(true);             //设为多选
        int returnVal = chooser.showOpenDialog(getContentPane());        //是否打开文件选择框
        if (returnVal == JFileChooser.APPROVE_OPTION) {          //如果符合文件类型
            File[] files = chooser.getSelectedFiles();      //获取绝对路径
            for(File item : files){
                sendFile(item);
            }
        }
    }

    public void onBtnSendMessageClick() {
        String text= edtMessage.getText();
        sendText(text);
        if(ckbAutoClean.isSelected()){
            edtMessage.setText("");
        }
    }

    public void onBtnCleanLogClicked() {
        this.list.clear();
        this.msgAdapter.notifyDataSetChanged();
    }
}
