package com.i2f.lan.lantransfer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import i2f.net.core.NetTransfer;
import i2f.net.core.NetTransferResponse;
import i2f.net.core.NetUtil;
import i2f.net.tcp.TcpClient;
import i2f.net.tcp.TcpServer;
import i2f.net.tcp.impl.ClientAccepter;

public class MainActivityBak extends Activity {



    public static final int SERVER_PORT=63321;

    public static final int REQ_CODE_PERMISSION_REQUIRE=0x101;
    public static final int REQ_CODE_CHOICE_FILE=0x102;

    public static final int WHAT_PRINT=0x101;


    public static final int WHAT_SEND_TEXT=0x301;
    public static final int WHAT_SEND_FILE=0x302;
    public static final int WHAT_RUN_SERVER=0x303;
    public static final int WHAT_SCAN_LAN=0x304;
    public static final int WHAT_CONNECT_SERVER=0x305;
    public static final int WHAT_SCAN_LAN_OK=0x306;

    private EditText edtSendMessage;
    private CheckBox ckbRunServer;
    private Spinner spnLocalAddress;
    private Spinner spnConnectAddress;
    private ListView lsvRecv;
    private Button btnScanLan;
    private Button btnConnect;
    private Button btnChoiceFile;
    private Button btnSendMessage;

    private TcpServer tcpServer;
    private TcpClient client;

    private List<String> list=new ArrayList<>();

    private Map<String, Set<String>> ipAddress=new HashMap<>();
    private Set<String> lanIpAddress=new HashSet<>();

    private LanAddressAdapter lanAdapter=new LanAddressAdapter();
    private LocalAddressAdapter localAdapter=new LocalAddressAdapter();
    private MessageAdapter msgAdapter=new MessageAdapter();

    class NetworkRunner implements Runnable{
        private String ip;
        public NetworkRunner(String ip){
            this.ip=ip;
        }
        @Override
        public void run() {
            try{
                client=new TcpClient(ip,SERVER_PORT);
                Socket sock=client.getSocket();
                TransferClientProcessor target=new TransferClientProcessor(sock);
                Thread thread=new Thread(target);
                thread.start();
            }catch (Exception e){
                print("connect server error:"+e.getMessage());
            }
        }
    }

    private Handler handler=new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {
            int what=msg.what;
            switch (what){
                case WHAT_SCAN_LAN_OK:
                    localAdapter.notifyDataSetChanged();
                    lanIpAddress.clear();
                    lanAdapter.notifyDataSetChanged();
                    break;
                case WHAT_PRINT:
                    String str=String.valueOf(msg.obj);
                    list.add(str);
                    msgAdapter.notifyDataSetChanged();
                    break;
                case WHAT_CONNECT_SERVER:
                    String ip=String.valueOf(msg.obj);
                    if(client!=null){
                        try{
                            client.close();
                            client=null;
                        }catch (Exception e){
                            print("close client error:"+e.getMessage());
                        }
                    }
                    new Thread(new NetworkRunner(ip)).start();

                    break;
                case WHAT_SCAN_LAN:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                print("scans lan server ...");
                                Map<InetAddress, Set<InetAddress>> lan= NetUtil.getAllLanInfo();
                                print("scans lan online server ...");
                                Set<String> finds=new HashSet<>();
                                ipAddress.clear();
                                for(Map.Entry<InetAddress,Set<InetAddress>> item : lan.entrySet()){
                                    InetAddress myaddr=item.getKey();
                                    if(myaddr instanceof Inet6Address){
                                        continue;
                                    }
                                    Set<InetAddress> lanaddrs=item.getValue();
                                    if(lanaddrs==null || lanaddrs.size()==0){
                                        continue;
                                    }
                                    Set<String> ips=new HashSet<>();
                                    for(InetAddress addr : lanaddrs){
                                        if(addr instanceof Inet6Address){
                                            continue;
                                        }
                                        try{
                                            TcpClient pclient=new TcpClient(addr.getHostAddress(),SERVER_PORT);
                                            pclient.close();
                                            finds.add(addr.getHostAddress());
                                            ips.add(addr.getHostAddress());
                                        }catch(Exception e){
                                            print("connect "+addr.getHostAddress()+" error of "+e.getMessage());
                                        }
                                    }
                                    if(ips.size()>0){
                                        ipAddress.put(myaddr.getHostAddress(),ips);
                                    }
                                }
                                try{
                                    TcpClient pclient=new TcpClient("127.0.0.1",SERVER_PORT);
                                    pclient.close();
                                    finds.add("127.0.0.1");
                                    Set<String> ips=new HashSet<>();
                                    ips.add("127.0.0.1");
                                    ipAddress.put("127.0.0.1",ips);
                                }catch(Exception e){
                                    print("connect 127.0.0.1 error of "+e.getMessage());
                                }
                            }catch (Exception e){
                                print("scan error:"+e.getMessage());
                            }
                            handler.sendEmptyMessage(WHAT_SCAN_LAN_OK);
                        }
                    }).start();
                    break;
                case WHAT_SEND_TEXT:
                    final String str1=String.valueOf(msg.obj);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if(client==null){
                                    print("未连接目标主机！");
                                }else{
                                    OutputStream os=client.getOutputStream();
                                    NetTransfer.sendString(str1,os);
                                }
                            }catch (Exception e){
                                print("send msg error:"+e.getMessage());
                            }
                        }
                    });
                    break;
                case WHAT_SEND_FILE:
                    final File file=(File)msg.obj;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                if(client==null){
                                    print("未连接目标主机！");
                                }else{
                                    OutputStream os=client.getOutputStream();
                                    NetTransfer.sendFile(file.getAbsolutePath(),os);
                                }
                            }catch (Exception e){
                                print("send file error:"+e.getMessage());
                            }
                        }
                    }).start();
                    break;
                case WHAT_RUN_SERVER:
                    Boolean open=(Boolean) msg.obj;
                    if(open){
                        if(tcpServer==null){
                            try{
                                print("server listening...");
                                tcpServer=new TcpServer(SERVER_PORT,new TransferClientAccepter());
                                print("server listen.");
                            }catch (Exception e){
                                print("start server error:"+e.getMessage());
                            }
                        }
                    }else{
                        if(tcpServer!=null){
                            try{
                                print("server closing...");
                                tcpServer.close();
                                print("server close.");
                            }catch (Exception e){
                                print("close server error:"+e.getMessage());
                            }
                        }
                        tcpServer=null;
                    }
                    break;
            }
        }
    };

    public class TransferClientProcessor implements Runnable{
        public Socket sock;
        public TransferClientProcessor(Socket sock){
            this.sock=sock;
        }
        @Override
        public void run() {
            try{
                InputStream is=sock.getInputStream();
                while(true){
                    NetTransferResponse resp=NetTransfer.recv(is);
                    if(resp.isTextPlain()){
                        String str=resp.getAsString();
                        print("server "+sock.getInetAddress().getHostAddress()+":"+str);
                        if("exit".equals(str)){
                            sock.close();
                            print("client close");
                            break;
                        }
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }

    }

    public class TransferClientAccepter extends ClientAccepter {
        @Override
        protected void sockProcess(int index, Socket sock) {
            print("client accept:"+sock.getInetAddress().getHostAddress());
            try{
                InputStream is= sock.getInputStream();
                OutputStream os=sock.getOutputStream();
                while(true) {
                    NetTransferResponse resp = NetTransfer.recv(is);
                    if(resp.isTextPlain()){
                        String str=resp.getAsString();
                        print("client "+sock.getInetAddress().getHostAddress()+":"+str);
                        if("exit".equals(str)){
                            print("client exit:"+sock.getInetAddress().getHostAddress());
                            NetTransfer.sendString("exit",os);
                            sock.close();
                            break;
                        }
                        if("hello".equals(str)){
                            print("client hello:"+sock.getInetAddress().getHostAddress());
                            InetAddress addr=sock.getInetAddress();
                            String clientIp=addr.getHostAddress();
                            String hostName=addr.getHostName();
                            NetTransfer.sendString("ip="+clientIp+","+"host="+hostName,os);
                        }
                    }else if(resp.isFile()){
                        String fileName=resp.getName();
                        File dir=new File(Environment.getExternalStorageDirectory(),"Download");
                        File file=new File(dir,fileName);
                        print("save client file:"+fileName+"\n\t-> "+file.getAbsolutePath());
                        if(!file.getParentFile().exists()){
                            file.getParentFile().mkdirs();
                        }
                        resp.saveAsFile(file);
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void print(String text){
        Message msg=new Message();
        msg.what=WHAT_PRINT;
        msg.obj=text;
        handler.sendMessage(msg);
    }

    // 文件选择
    // https://www.jianshu.com/p/a229722b55fe
    // 分享与接收分享
    // https://www.jianshu.com/p/30fe5307689c

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initActivity();
        requirePermission();
        resolveSharedContent();
    }

    private void initActivity(){
        edtSendMessage =findViewById(R.id.edtSendMessage);
        ckbRunServer=findViewById(R.id.ckbRunServer);
        spnLocalAddress=findViewById(R.id.spnLocalAddress);
        spnConnectAddress=findViewById(R.id.spnLanAddress);
        lsvRecv=findViewById(R.id.lsvRecv);
        btnChoiceFile=findViewById(R.id.btnChoiceFile);
        btnConnect=findViewById(R.id.ckbConnect);
        btnScanLan=findViewById(R.id.btnScanLan);
        btnSendMessage=findViewById(R.id.btnSendMessage);

        spnLocalAddress.setAdapter(localAdapter);
        spnConnectAddress.setAdapter(lanAdapter);
        lsvRecv.setAdapter(msgAdapter);

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
                tv=new TextView(MainActivityBak.this);
            }
            Object item=getItem(position);
            tv.setText(item.toString());
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
                tv=new TextView(MainActivityBak.this);
            }
            Object item=getItem(position);
            tv.setText(item.toString());
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
                tv=new TextView(MainActivityBak.this);
            }
            Object item=getItem(position);
            tv.setText(item.toString());
            return tv;
        }
    }

    private void requirePermission(){
        Toast.makeText(this,"软件需要权限以保证正常运行：\n" +
                "\t1.网络权限：用于进行信息交流\n" +
                "\t2.存储读取权限：用于读取或存储文件",Toast.LENGTH_LONG);
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
        Message msg=new Message();
        msg.what=WHAT_SEND_TEXT;
        msg.obj=text;
        handler.sendMessage(msg);
    }

    void sendFile(File file){
        Message msg=new Message();
        msg.what=WHAT_SEND_FILE;
        msg.obj=file;
        handler.sendMessage(msg);
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
        Message msg=new Message();
        msg.what=WHAT_RUN_SERVER;
        msg.obj=ck;
        handler.sendMessage(msg);
    }

    public void onBtnScanLanClicked(View view) {
        Message msg=new Message();
        msg.what=WHAT_SCAN_LAN;
        handler.sendMessage(msg);
    }

    public void onBtnConnectClicked(View view) {
        Object obj=spnConnectAddress.getSelectedItem();
        print("select:"+obj);
        Message msg=new Message();
        msg.what=WHAT_CONNECT_SERVER;
        msg.obj=obj;
        handler.sendMessage(msg);
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
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if(requestCode==REQ_CODE_CHOICE_FILE){
//            if(data.getData()!=null){
//                String choiceFile=getFilePathFormUri(data.getData());
//                File file=new File(choiceFile);
//                sendFile(file);
//                Toast.makeText(this,"choice file:"+file.getAbsolutePath(),Toast.LENGTH_LONG);
//            }else{
//                Toast.makeText(this,"no file choice.",Toast.LENGTH_LONG);
//            }
//        }
//        super.onActivityResult(requestCode, resultCode, data);
//    }

    private String getFilePathFormUri(Uri uri) {
        String ret=null;
        // 通过ContentProvider查询文件路径
        ContentResolver resolver = this.getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        if (cursor == null) {
            // 未查询到，说明为普通文件，可直接通过URI获取文件路径
            String path = uri.getPath();
            ret=path;
        } else if (cursor.moveToFirst()) {
            // 多媒体文件，从数据库中获取文件的真实路径
            String path = cursor.getString(cursor.getColumnIndex("_data"));
            ret=path;
        }
        cursor.close();
        return ret;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==REQ_CODE_PERMISSION_REQUIRE){
            for(int i=0;i<grantResults.length;i++){
                if(grantResults[i]!= PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this,"权限：["+permissions[i]+"]没有授权，软件可能无法正常运行！",Toast.LENGTH_LONG);
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
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
            if(path!=null){
                File file=new File(path);
                sendFile(file);
                Toast.makeText(this,"choice file:"+file.getAbsolutePath(),Toast.LENGTH_LONG);
            }else{
                Toast.makeText(this,"no file choice.",Toast.LENGTH_LONG);
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
