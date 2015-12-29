package com.app.talkonnet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.os.Handler;
import android.os.Message;

public class ListenConnect implements Runnable {
	private Handler handler;//用于和主线程通信
    private boolean havelinked=false;
    private Socket s;
    ServerSocket ss;
	public ListenConnect(Handler handler) {
		super();
		this.handler = handler;
	}
	@Override
	public void run(){
		// TODO Auto-generated method stub
		try {
			ss=new ServerSocket(ConstValue.port);
			while(true)
			{
				if(!havelinked)
				{
					s=ss.accept();
					Message msg=Message.obtain();
					msg.what=ConstValue.newlink;
					msg.obj=s;
					handler.sendMessage(msg);
					//havelinked=true;//在外边对havelinked进行改变，可能会有点问题
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public boolean getHavelinked() {
		return havelinked;
	}
	public void setHavelinked(boolean havelinked) {
		this.havelinked = havelinked;
	}
	public Socket getS() {
		return s;
	}
	public void setS(Socket s) {
		this.s = s;
	}

}
