package com.i2f.lan.lantransfer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {

    public static final int SERVER_PORT=63321;

    public static final int REQ_CODE_PERMISSION_REQUIRE=0x101;
    public static final int REQ_CODE_CHOICE_FILE=0x102;

    private EditText edtSendMessage;
    private CheckBox ckbRunServer;
    private Spinner spnLocalAddress;
    private Spinner spnConnectAddress;
    private ListView lsvRecv;
    private Button btnScanLan;
    private CheckBox ckbConnect;
    private Button btnChoiceFile;
    private Button btnSendMessage;

    private EditText edtServerPort;
    private CheckBox ckbAutoCleanSend;
    private EditText edtConnectIp;
    private EditText edtConnectPort;




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

    private LanAddressAdapter lanAdapter=new LanAddressAdapter();
    private LocalAddressAdapter localAdapter=new LocalAddressAdapter();
    private MessageAdapter msgAdapter=new MessageAdapter();

    private ServiceCallbackReceiver receiver;

    public static final String ACTION_SERVICE_CALLBACK_RECIVER = "i2f.lan.transfer.server.callback";

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

    public static void callbackServerStatus(Context ctx,boolean status,int serverPort){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SERVER_STATUS);
        intent.putExtra(KEY_STATUS,status);
        intent.putExtra(KEY_PORT,serverPort);
        ctx.sendBroadcast(intent);
    }

    public static void callbackClientStatus(Context ctx,boolean status,String connectIp,int connecPort){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
        intent.putExtra(KEY_RECEIVER_CMD,CMD_CLIENT_STATUS);
        intent.putExtra(KEY_STATUS,status);
        intent.putExtra(KEY_IP,connectIp);
        intent.putExtra(KEY_PORT,connecPort);
        ctx.sendBroadcast(intent);
    }
    public static void callbackLanScanResult(Context ctx,Map<String,Set<String>> result){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
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
        ctx.sendBroadcast(intent);
    }

    public static void callbackSystemError(Context ctx,String text){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SYSTEM_ERROR);
        intent.putExtra(KEY_TEXT,text);
        ctx.sendBroadcast(intent);
    }

    public static void callbackSystemLog(Context ctx,String text){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SYSTEM_LOG);
        intent.putExtra(KEY_TEXT,text);
        ctx.sendBroadcast(intent);
    }

    public static void callbackServerRecv(Context ctx,String text){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
        intent.putExtra(KEY_RECEIVER_CMD,CMD_SERVER_RECV);
        intent.putExtra(KEY_TEXT,text);
        ctx.sendBroadcast(intent);
    }

    public static void callbackClientRecv(Context ctx,String text){
        Intent intent=new Intent();
        intent.setAction(ACTION_SERVICE_CALLBACK_RECIVER);
        intent.putExtra(KEY_RECEIVER_CMD,CMD_CLIENT_RECV);
        intent.putExtra(KEY_TEXT,text);
        ctx.sendBroadcast(intent);
    }

    private void onReceiveNullCmd(Intent intent){
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,"empty cmd received.");
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveServerStatusCmd(Intent intent){
        boolean status=intent.getBooleanExtra(KEY_STATUS,false);
        ckbRunServer.setChecked(status);
        int port=intent.getIntExtra(KEY_PORT,SERVER_PORT);
        edtServerPort.setText(port+"");
        MsgItem item=new MsgItem(MsgItem.TYPE_LOG,"server status sync.");
        list.addFirst(item);
        msgAdapter.notifyDataSetChanged();
    }
    private void onReceiveClientStatusCmd(Intent intent){
        boolean status=intent.getBooleanExtra(KEY_STATUS,false);
        ckbConnect.setChecked(status);
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

    class ServiceCallbackReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            dispatchReceiverIntent(intent);
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
    }

    // 文件选择
    // https://www.jianshu.com/p/a229722b55fe
    // 分享与接收分享
    // https://www.jianshu.com/p/30fe5307689c

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(receiver==null){
            receiver=new ServiceCallbackReceiver();
            IntentFilter filter=new IntentFilter();
            filter.addAction(ACTION_SERVICE_CALLBACK_RECIVER);
            registerReceiver(receiver,filter);
        }
        initActivity();
        requirePermission();
        resolveSharedContent();
    }

    @Override
    protected void onDestroy() {
        if(receiver!=null){
            unregisterReceiver(receiver);
        }
        receiver=null;
        super.onDestroy();
    }

    private void initActivity(){
        edtSendMessage =findViewById(R.id.edtSendMessage);
        ckbRunServer=findViewById(R.id.ckbRunServer);
        spnLocalAddress=findViewById(R.id.spnLocalAddress);
        spnConnectAddress=findViewById(R.id.spnLanAddress);
        lsvRecv=findViewById(R.id.lsvRecv);
        btnChoiceFile=findViewById(R.id.btnChoiceFile);
        ckbConnect =findViewById(R.id.ckbConnect);
        btnScanLan=findViewById(R.id.btnScanLan);
        btnSendMessage=findViewById(R.id.btnSendMessage);

        edtServerPort=findViewById(R.id.edtServerPort);
        ckbAutoCleanSend=findViewById(R.id.ckbAutoCleanSend);
        edtConnectIp=findViewById(R.id.edtConnectIp);
        edtConnectPort=findViewById(R.id.edtConnectPort);

        edtServerPort.setText(SERVER_PORT+"");
        edtConnectPort.setText(SERVER_PORT+"");

        spnLocalAddress.setAdapter(localAdapter);
        spnConnectAddress.setAdapter(lanAdapter);
        lsvRecv.setAdapter(msgAdapter);

        NetworkBackgroundService.serverStatus(this);
        NetworkBackgroundService.clientStatus(this);
        NetworkBackgroundService.scanLanAddressCache(this);

        spnLocalAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item=localAdapter.getItem(position);
                String ip=String.valueOf(item);
                lanIpAddress.clear();
                lanIpAddress=ipAddress.get(ip);
                if(lanIpAddress==null){
                    lanIpAddress=new HashSet<>();
                }
                lanAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spnConnectAddress.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item=lanAdapter.getItem(position);
                String ip=String.valueOf(item);
                edtConnectIp.setText(ip);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    class LocalAddressAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return ipAddress.size();
        }

        @Override
        public Object getItem(int position) {
            String sip="";
            int i=0;
            for(String ip: ipAddress.keySet()){
                sip=ip;
                if(i==position){
                    break;
                }
                i++;
            }
            return sip;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv=null;
            if(convertView!=null){
                tv=(TextView)convertView;
            }else{
                tv=new TextView(MainActivity.this);
            }
            Object item=getItem(position);
            tv.setText(item.toString());
            tv.setTextSize(18.0f);
            return tv;
        }
    }

    class LanAddressAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return lanIpAddress.size();
        }

        @Override
        public Object getItem(int position) {
            String sip="";
            int i=0;
            for(String ip: lanIpAddress){
                sip=ip;
                if(i==position){
                    break;
                }
                i++;
            }
            return sip;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv=null;
            if(convertView!=null){
                tv=(TextView)convertView;
            }else{
                tv=new TextView(MainActivity.this);
            }
            Object item=getItem(position);
            tv.setText(item.toString());
            tv.setTextSize(18.0f);
            return tv;
        }
    }

    class MessageAdapter extends BaseAdapter{
        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public Object getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv=null;
            if(convertView!=null){
                tv=(TextView)convertView;
            }else{
                tv=new TextView(MainActivity.this);
            }
            Object item=getItem(position);
            MsgItem msg=(MsgItem)item;
            tv.setText(msg.msg);
            tv.setTextSize(20.0f);
            switch (msg.type){
                case MsgItem.TYPE_ERROR:
                    tv.setTextColor(Color.rgb(255,0,0));
                    break;
                case MsgItem.TYPE_LOG:
                    tv.setTextColor(Color.rgb(180,180,180));
                    break;
                case MsgItem.TYPE_SERVER_RECV:
                    tv.setTextColor(Color.rgb(0,150,100));
                    break;
                case MsgItem.TYPE_CLIENT_RECV:
                    tv.setTextColor(Color.rgb(0,100,150));
                    break;
            }
            return tv;
        }
    }

    private void requirePermission(){
        Toast.makeText(this,"软件需要权限以保证正常运行：\n" +
                "\t1.网络权限：用于进行信息交流\n" +
                "\t2.存储读取权限：用于读取或存储文件",Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE
            },REQ_CODE_PERMISSION_REQUIRE);
        }
    }

    private void resolveSharedContent(){
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action)&&type!=null){
            if ("text/plain".equals(type)){
                dealTextMessage(intent);
            }else if(type.startsWith("image/")){
                dealPicStream(intent);
            }else if(type.startsWith("application/")){
                dealDefaultAsFile(intent);
            }
        }else if (Intent.ACTION_SEND_MULTIPLE.equals(action)&&type!=null){
            if (type.startsWith("image/")){
                dealMultiplePicStream(intent);
            }
        }
    }

    void sendText(String text){
       NetworkBackgroundService.sendString(this,text);
    }

    void sendFile(File file){
        NetworkBackgroundService.sendFile(this,file.getAbsolutePath());
    }

    void dealTextMessage(Intent intent){
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        sendText(text);
    }

    void dealPicStream(Intent intent){
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String path=getFilePathFormUri(uri);
        sendFile(new File(path));
    }

    void dealDefaultAsFile(Intent intent){
        Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String path=getFilePathFormUri(uri);
        sendFile(new File(path));
    }

    void dealMultiplePicStream(Intent intent){
        ArrayList<Uri> list = intent.getParcelableArrayListExtra(intent.EXTRA_STREAM);
        for(Uri uri : list){
            String path=getFilePathFormUri(uri);
            sendFile(new File(path));
        }
    }


    private void shareTextBySystem(String text,String shareDescription){
        Intent sendIntent =new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, shareDescription));
    }

    private void shareImageBySystem(Uri uri,String shareDescription){
        Intent shareIntent =new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM,uri);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, shareDescription));
    }

    private void shareImageListBySystem(ArrayList<Uri> list, String shareDescription){
        Intent shareIntent =new Intent();
        shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, list);
        shareIntent.setType("image/*");
        startActivity(Intent.createChooser(shareIntent,shareDescription));
    }

    private void shareFileBySystem(Uri uri,String shareDescription){
        Intent shareIntent =new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM,uri);
        shareIntent.setType("application/octet-stream");
        startActivity(Intent.createChooser(shareIntent, shareDescription));
    }

    public void onBtnRunServerClicked(View view) {
        boolean ck=ckbRunServer.isChecked();
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
            NetworkBackgroundService.runServer(this,port);
        }else{
            NetworkBackgroundService.stopServer(this);
        }
    }

    public void onBtnScanLanClicked(View view) {
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
        NetworkBackgroundService.scanLanAddress(this,port);
    }

    public void onBtnConnectClicked(View view) {
        boolean ck=ckbConnect.isChecked();
        if(ck){
            Object obj=spnConnectAddress.getSelectedItem();
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
            NetworkBackgroundService.connectServer(this,ip,port);
        }else{
            NetworkBackgroundService.disconnectServer(this);
        }
    }

    public void onBtnSendFileClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        this.startActivityForResult(intent, REQ_CODE_CHOICE_FILE);
    }

    public void onBtnSendMessageClick(View view) {
        String text= edtSendMessage.getText().toString();
        sendText(text);
        if(ckbAutoCleanSend.isChecked()){
            edtSendMessage.setText("");
        }
    }

    public void onBtnCleanLogClicked(View view) {
        this.list.clear();
        this.msgAdapter.notifyDataSetChanged();
    }

    private String getFilePathFormUri(Uri uri) {
        String path=null;
        if ("file".equalsIgnoreCase(uri.getScheme())) {                                     //使用第三方应用打开
            path=uri.getPath();
        }
        //4.4以后
        else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            path = getPath(this, uri);

        } else {//4.4以下系统调用方法
            path=getRealPathFromURI(uri);
        }
        return path;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==REQ_CODE_PERMISSION_REQUIRE){
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"权限：["+permissions[i]+"]没有授权，软件可能无法正常运行！",Toast.LENGTH_LONG).show();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode==REQ_CODE_CHOICE_FILE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path=getFilePathFormUri(uri);
            if(path!=null){
                File file=new File(path);
                sendFile(file);
                Toast.makeText(this,"choice file:"+file.getAbsolutePath(),Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,"no file choice.",Toast.LENGTH_LONG).show();
            }
        }
    }

    public String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if (null != cursor && cursor.moveToFirst()) {
            ;
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
            cursor.close();
        }
        return res;
    }

    /**
     * 4.4的从Uri获取文件绝对路径
     */
    @SuppressLint("NewApi")
    public String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection,
                                String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }
}
