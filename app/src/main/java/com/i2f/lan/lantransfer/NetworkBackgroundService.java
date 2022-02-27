package com.i2f.lan.lantransfer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import i2f.net.core.NetTransfer;
import i2f.net.core.NetTransferResponse;
import i2f.net.core.NetUtil;
import i2f.net.tcp.TcpClient;
import i2f.net.tcp.TcpServer;
import i2f.net.tcp.impl.ClientAccepter;

public class NetworkBackgroundService extends Service {

    public static final String KEY_SERVICE_CMD = "cmd";

    public static final String KEY_IP="ip";
    public static final String KEY_PORT="port";
    public static final String KEY_TEXT="text";
    public static final String KEY_FILE="file";

    public static final int CMD_NULL=0;

    public static final int CMD_RUN_SERVER = 0x101;
    public static final int CMD_STOP_SERVER = 0x102;
    public static final int CMD_SERVER_STATUS=0x103;

    public static final int CMD_CONNECT_CLIENT = 0x201;
    public static final int CMD_DISCONNECT_CLIENT = 0x202;
    public static final int CMD_CLIENT_STATUS=0x203;

    public static final int CMD_SEND_STRING = 0x301;
    public static final int CMD_SEND_FILE = 0x302;

    public static final int CMD_SCAN_LAN = 0x401;
    public static final int CMD_SCAN_LAN_CACHE=0x402;

    public static TcpServer server;
    public static TcpClient client;
    public static Map<String,Set<String>> ipAddress=new HashMap<>();
    public static ExecutorService pool;
    public static int serverPort=MainActivity.SERVER_PORT;
    public static String connectIp;
    public static int connectPort=MainActivity.SERVER_PORT;

    static {
        pool= Executors.newFixedThreadPool(3);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if(pool!=null){
                    if(!pool.isShutdown()){
                        pool.shutdown();
                    }
                }
                try{
                    if(server!=null){
                        server.close();
                    }
                }catch (Exception e){

                }
                try {
                    if (client != null) {
                        client.close();
                    }
                }catch (Exception e){

                }
            }
        }));
    }

    public static void runServer(Context ctx,int port){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_RUN_SERVER);
        intent.putExtra(KEY_PORT,port);
        ctx.startService(intent);
    }

    public static void stopServer(Context ctx){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_STOP_SERVER);
        ctx.startService(intent);
    }

    public static void serverStatus(Context ctx){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_SERVER_STATUS);
        ctx.startService(intent);
    }

    public static void connectServer(Context ctx,String ip,int port){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_CONNECT_CLIENT);
        intent.putExtra(KEY_IP,ip);
        intent.putExtra(KEY_PORT,port);
        ctx.startService(intent);
    }

    public static void disconnectServer(Context ctx){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_DISCONNECT_CLIENT);
        ctx.startService(intent);
    }

    public static void clientStatus(Context ctx){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_CLIENT_STATUS);
        ctx.startService(intent);
    }

    public static void sendString(Context ctx,String text){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_SEND_STRING);
        intent.putExtra(KEY_TEXT,text);
        ctx.startService(intent);
    }

    public static void sendFile(Context ctx,String file){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_SEND_FILE);
        intent.putExtra(KEY_FILE,file);
        ctx.startService(intent);
    }

    public static void scanLanAddress(Context ctx,int port){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_SCAN_LAN);
        intent.putExtra(KEY_PORT,port);
        ctx.startService(intent);
    }

    public static void scanLanAddressCache(Context ctx){
        Intent intent=new Intent();
        intent.setClass(ctx,NetworkBackgroundService.class);
        intent.putExtra(KEY_SERVICE_CMD,CMD_SCAN_LAN_CACHE);
        ctx.startService(intent);
    }

    public static int getServiceCmd(Intent intent){
        return intent.getIntExtra(KEY_SERVICE_CMD,CMD_NULL);
    }

    private void dispatchServiceIntent(Intent intent){
        int cmd=getServiceCmd(intent);
        switch (cmd){
            case CMD_NULL:
                onNullCmd(intent);
                break;
            case CMD_RUN_SERVER:
                onRunServerCmd(intent);
                break;
            case CMD_STOP_SERVER:
                onStopServerCmd(intent);
                break;
            case CMD_SERVER_STATUS:
                onServerStatusCmd(intent);
                break;
            case CMD_CONNECT_CLIENT:
                onConnectClientCmd(intent);
                break;
            case CMD_DISCONNECT_CLIENT:
                onDisconnectClientCmd(intent);
                break;
            case CMD_CLIENT_STATUS:
                onClientStatusCmd(intent);
                break;
            case CMD_SEND_STRING:
                onSendStringCmd(intent);
                break;
            case CMD_SEND_FILE:
                onSendFileCmd(intent);
                break;
            case CMD_SCAN_LAN:
                onScanLanCmd(intent);
                break;
            case CMD_SCAN_LAN_CACHE:
                onScanLanCacheCmd(intent);
                break;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dispatchServiceIntent(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void onNullCmd(Intent intent){
        MainActivity.callbackSystemLog(getApplicationContext(),"service not cmd found");
    }
    private void onRunServerCmd(Intent intent){
        final int port=intent.getIntExtra(KEY_PORT,MainActivity.SERVER_PORT);
        pool.submit(new Runnable() {
            @Override
            public void run() {
                if(server==null){
                    try{
                        MainActivity.callbackSystemLog(getApplicationContext(),"server listening...");
                        server=new TcpServer(port,new TransferClientAccepter());
                        serverPort=port;
                        MainActivity.callbackSystemLog(getApplicationContext(),"server listen.");
                    }catch (Exception e){
                        MainActivity.callbackSystemError(getApplicationContext(),"start server error:"+e.getMessage()+" of "+e.getClass().getName());
                    }
                }
            }
        });
    }
    private void onStopServerCmd(Intent intent){
        pool.submit(new Runnable() {
            @Override
            public void run() {
                if(server!=null){
                    try{
                        MainActivity.callbackSystemLog(getApplicationContext(),"server closing...");
                        server.close();
                        MainActivity.callbackSystemLog(getApplicationContext(),"server close.");
                    }catch (Exception e){
                        MainActivity.callbackSystemError(getApplicationContext(),"close server error:"+e.getMessage()+" of "+e.getClass().getName());
                    }
                }
                server=null;
            }
        });
    }
    private void onServerStatusCmd(Intent intent){
        if(server==null){
            MainActivity.callbackServerStatus(getApplicationContext(),false,serverPort);
        }else if(server.isShutdown){
            MainActivity.callbackServerStatus(getApplicationContext(),false,serverPort);
        }else{
            MainActivity.callbackServerStatus(getApplicationContext(),true,serverPort);
        }
    }
    private void onConnectClientCmd(Intent intent){
        final String ip=intent.getStringExtra(KEY_IP);
        final int port=intent.getIntExtra(KEY_PORT,MainActivity.SERVER_PORT);
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try{
                    MainActivity.callbackSystemLog(getApplicationContext(),"client connect...");
                    client=new TcpClient(ip,port);
                    connectIp=ip;
                    connectPort=port;
                    MainActivity.callbackSystemLog(getApplicationContext(),"client connected.");
                    Socket sock=client.getSocket();
                    TransferClientProcessor target=new TransferClientProcessor(sock);
                    Thread thread=new Thread(target);
                    thread.start();
                }catch (Exception e){
                    MainActivity.callbackSystemError(getApplicationContext(),"connect server error:"+e.getMessage()+" of "+e.getClass().getName());
                }
            }
        });
    }
    private void onDisconnectClientCmd(Intent intent){
        pool.submit(new Runnable() {
            @Override
            public void run() {
                if(client!=null){
                    try{
                        MainActivity.callbackSystemLog(getApplicationContext(),"client closing...");
                        client.close();
                        client=null;
                        MainActivity.callbackSystemLog(getApplicationContext(),"client closed.");
                    }catch (Exception e){
                        MainActivity.callbackSystemError(getApplicationContext(),"close client error:"+e.getMessage()+" of "+e.getClass().getName());
                    }
                }
            }
        });
    }
    private void onClientStatusCmd(Intent intent){
        if(client==null){
            MainActivity.callbackClientStatus(getApplicationContext(),false,connectIp,connectPort);
        }else if(client.getSocket().isClosed()){
            MainActivity.callbackClientStatus(getApplicationContext(),false,connectIp,connectPort);
        }else{
            MainActivity.callbackClientStatus(getApplicationContext(),true,connectIp,connectPort);
        }
    }
    private void onSendStringCmd(Intent intent){
        final String text=intent.getStringExtra(KEY_TEXT);
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try{
                    if(client==null){
                        MainActivity.callbackSystemError(getApplicationContext(),"target server not connect!");
                    }else{
                        OutputStream os=client.getOutputStream();
                        NetTransfer.sendString(text,os);
                        MainActivity.callbackSystemLog(getApplicationContext(),"sent text success.");
                    }
                }catch (Exception e){
                    MainActivity.callbackSystemError(getApplicationContext(),"send msg error:"+e.getMessage()+" of "+e.getClass().getName());
                }
            }
        });
    }
    private void onSendFileCmd(Intent intent){
        final String file=intent.getStringExtra(KEY_FILE);
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try{
                    if(client==null){
                        MainActivity.callbackSystemError(getApplicationContext(),"target server not connect!");
                    }else{
                        MainActivity.callbackSystemLog(getApplicationContext(),"sending file ...");
                        OutputStream os=client.getOutputStream();
                        NetTransfer.sendFile(file,os);
                        MainActivity.callbackSystemLog(getApplicationContext(),"sent file success.");
                    }
                }catch (Exception e){
                    MainActivity.callbackSystemError(getApplicationContext(),"send file error:"+e.getMessage()+" of "+e.getClass().getName());
                }
            }
        });
    }
    private void onScanLanCmd(Intent intent){
        final int port=intent.getIntExtra(KEY_PORT,MainActivity.SERVER_PORT);
        pool.submit(new Runnable() {
            @Override
            public void run() {
                try{
                    MainActivity.callbackSystemLog(getApplicationContext(),"scans lan server ...");
                    Map<InetAddress, Set<InetAddress>> lan= NetUtil.getAllLanInfo();
                    MainActivity.callbackSystemLog(getApplicationContext(),"lan server address find "+lan.size());
                    MainActivity.callbackSystemLog(getApplicationContext(),"scans lan online server on port "+port+" ...");
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
                        MainActivity.callbackSystemLog(getApplicationContext(),"local address "+myaddr.getHostAddress()+" find "+lanaddrs.size());
                        Set<String> ips=new HashSet<>();
                        for(InetAddress addr : lanaddrs){
                            if(addr instanceof Inet6Address){
                                continue;
                            }
                            try{
                                TcpClient pclient=new TcpClient(addr.getHostAddress(),port);
                                pclient.close();
                                finds.add(addr.getHostAddress());
                                ips.add(addr.getHostAddress());
                                MainActivity.callbackSystemLog(getApplicationContext(),"find server "+addr.getHostAddress());
                            }catch(Exception e){
                                MainActivity.callbackSystemError(getApplicationContext(),"connect "+addr.getHostAddress()+" error of "+e.getMessage()+" of "+e.getClass().getName());
                            }
                        }
                        if(ips.size()>0){
                            ipAddress.put(myaddr.getHostAddress(),ips);
                        }
                    }
                    try{
                        TcpClient pclient=new TcpClient("127.0.0.1",port);
                        pclient.close();
                        finds.add("127.0.0.1");
                        Set<String> ips=new HashSet<>();
                        ips.add("127.0.0.1");
                        ipAddress.put("127.0.0.1",ips);
                    }catch(Exception e){
                        MainActivity.callbackSystemError(getApplicationContext(),"connect 127.0.0.1 error of "+e.getMessage()+" of "+e.getClass().getName());
                    }
                    MainActivity.callbackLanScanResult(getApplicationContext(),ipAddress);
                }catch (Exception e){
                    MainActivity.callbackSystemError(getApplicationContext(),"scan error:"+e.getMessage()+" of "+e.getClass().getName());
                }
            }
        });
    }

    private void onScanLanCacheCmd(Intent intent){
        MainActivity.callbackLanScanResult(getApplicationContext(),ipAddress);
    }

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
                        MainActivity.callbackClientRecv(getApplicationContext(),"server "+sock.getInetAddress().getHostAddress()+":"+str);
                        if("exit".equals(str)){
                            sock.close();
                            MainActivity.callbackSystemLog(getApplicationContext(),"client close");
                            break;
                        }
                    }
                }
            }catch(Exception e){
                MainActivity.callbackSystemError(getApplicationContext(),"client process recv error:"+e.getMessage()+" of "+e.getClass().getName());
            }
        }

    }

    public class TransferClientAccepter extends ClientAccepter {
        @Override
        protected void sockProcess(int index, Socket sock) {
            MainActivity.callbackSystemLog(getApplicationContext(),"client accept:"+sock.getInetAddress().getHostAddress());
            try{
                InputStream is= sock.getInputStream();
                OutputStream os=sock.getOutputStream();
                while(true) {
                    NetTransferResponse resp = NetTransfer.recv(is);
                    if(resp.isTextPlain()){
                        String str=resp.getAsString();
                        MainActivity.callbackServerRecv(getApplicationContext(),"client "+sock.getInetAddress().getHostAddress()+":"+str);
                        if("exit".equals(str)){
                            MainActivity.callbackSystemLog(getApplicationContext(),"client exit:"+sock.getInetAddress().getHostAddress());
                            NetTransfer.sendString("exit",os);
                            sock.close();
                            break;
                        }
                        if("hello".equals(str)){
                            MainActivity.callbackSystemLog(getApplicationContext(),"client hello:"+sock.getInetAddress().getHostAddress());
                            InetAddress addr=sock.getInetAddress();
                            String clientIp=addr.getHostAddress();
                            String hostName=addr.getHostName();
                            NetTransfer.sendString("ip="+clientIp+","+"host="+hostName,os);
                        }
                    }else if(resp.isFile()){
                        String fileName=resp.getName();
                        MainActivity.callbackSystemLog(getApplicationContext(),"recv file:"+fileName+" len:"+resp.getContentLength());
                        File dir=new File(Environment.getExternalStorageDirectory(),"Download");
                        File file=new File(dir,fileName);
                        if(!file.getParentFile().exists()){
                            file.getParentFile().mkdirs();
                        }
                        if(file.exists()){
                            file=new File(dir,new Date().getTime()+"_"+fileName);
                        }
                        resp.saveAsFile(file);
                        MainActivity.callbackServerRecv(getApplicationContext(),"save client file:"+fileName+"\n\t-> "+file.getAbsolutePath());
                    }
                }
            }catch(Exception e){
                MainActivity.callbackSystemError(getApplicationContext(),"server process client error:"+e.getMessage()+" of "+e.getClass().getName());
            }
        }
    }
}
