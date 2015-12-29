package com.app.talkonnet;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedList;

import com.google.gson.Gson;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TalkActivity extends Activity {

	ListenConnect lt;
	private Socket s;//连接
	DataInputStream in;
	DataOutputStream out;
	
	boolean stopRecord = true;
	boolean stopTrack = true;
	boolean stopReceive=true;
	
	final int playSoundPoolSize=5;//播放list存放的声音数据个数
	
	/*保存即将要播放声音包的lingkedList，线程播放时播放这里的全部声音包
	 * 这个linkList保存的声音包数量由playSoundPoolSize设定。
	 * 由于这个list会被接收数据和播放声音两个线程访问，会存在同步问题，可以用线程安全的list
	 * 这里先采用逻辑判断解决同步问题，但可能会加大延迟
	 */
	private LinkedList<byte[]> trackingSoundList;  
	//保存接收到但尚未准备播放的声音包的lingkedList，一个缓冲区。
	private LinkedList<byte[]> buffSoundList;
	//是否正在播放，解决对接收和播放线程对trackingSoundList的同步问题,true表示正在播放。
	volatile private boolean isPlayed=false;
	Handler mHandler;
/*	// 线程体对象
	private RecordThread recordThread;

	private PlayThread trackThread;
	
	private ReceiveThread receiveThread;*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.talk_activity);
		
		//加一个判断，对socket赋不同的值
		Intent intent=getIntent();
		String wherefrom=intent.getStringExtra("comefrom");
		
		Log.d("tag", "The intent is from"+wherefrom);
		
		if(wherefrom.equals("mainactivity"))
		{
			lt=MainActivity.lt;//这样获取连接是否有问题
			s=lt.getS();
		}
		else
		{
			s=CallActivity.s;
		}
		if(s==null)
		{
		  Log.d("tag", "s==null");
		}
		
		try {
			in = new DataInputStream(
					s.getInputStream());
			out = new DataOutputStream(s.getOutputStream());
		} catch (StreamCorruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		trackingSoundList=new LinkedList<byte[]>();
		buffSoundList=new LinkedList<byte[]>();
		
		mHandler=new Handler(){

			@SuppressLint("HandlerLeak")
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				if(msg.what==0x10)
				{
					Intent intent=new Intent(TalkActivity.this,MainActivity.class);
					startActivity(intent);
					TalkActivity.this.finish();
				}
				
				super.handleMessage(msg);
			}
			
		};
		
/*		// 初始化录音线程
		recordThread = new RecordThread();

		// 初始化播音线程
		trackThread = new PlayThread();
		
		//初始化接收线程
		receiveThread =new ReceiveThread();*/
		
		//启动线程
		new Thread(new RecordThread()).start();
		new Thread(new PlayThread()).start();
		new Thread(new ReceiveThread()).start();
		
		TextView tx=(TextView)findViewById(R.id.textViewTalk);
		tx.setText("正在通话");
		Button btn=(Button)findViewById(R.id.buttonStop);
		btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				stopTalk();
				Intent intent=new Intent(TalkActivity.this,MainActivity.class);
				startActivity(intent);
				TalkActivity.this.finish();
			}
		});
		
		
	}//end onCreate()
	
	/**
	 * 
	 */
	private void stopTalk(){
		stopRecord=false;
		stopReceive=false;
		stopTrack=false;
	}
	
	/** 
	* 判断是否断开连接，断开返回true,没有返回false 
	* @param socket 
	* @return 
	*/  
	public Boolean isServerClose(Socket socket){  
	   try{  
	    socket.sendUrgentData(0);//发送1个字节的紧急数据，默认情况下，服务器端没有开启紧急数据处理，不影响正常通信  
	    return false;  
	   }catch(Exception se){  
	    return true;  
	   }  
	} 
	
	
	/*
	 * 内部类实现线程体
	 */
	
	//录音线程
	class RecordThread implements Runnable {
	    //boolean stopRecord = true;
	    AudioRecord audioRecord = null;
		Gson gson = new Gson();
		
		String jsonStr=null;
		byte[] buffer=null;
		byte[] realBuffer=null;
		int bufferSize = ConstValue.RecordBufferSize;
		int realSize;
		
		public RecordThread() {
			// TODO Auto-generated constructor stub
		}


		public void run() {
			// TODO Auto-generated method stub
			
			// 为空才初始化
			if (audioRecord == null) {
				bufferSize=initAudioRecord();
			}
			
			audioRecord.startRecording();
			
			while (stopRecord==true) {
				
				//读取数据的数组的大小和minSize*2是否要一致
				buffer = new byte[bufferSize]; // short类型对应16bit音频数据格式，byte类型对应于8bit
				try {
					realSize = audioRecord.read(buffer, 0, bufferSize); // 返回值是个int类型的数据长度值

					// 发送数据,只取有效数组即可
					realBuffer = Arrays.copyOfRange(buffer, 0, realSize);

					// Log.v("test", "声音包字节数"+realSize);

					// Log.v("test", "语音包发出前十个包的内容为" + buffer[0] + buffer[150]
					// + buffer[800] + buffer[900]);

					jsonStr = gson.toJson(realBuffer);
					
					/**
					 * 录音线程里采用阻塞式网络IO，在IO进行时，会阻塞线线程，可能会漏掉某些应该记录的声音数据
					 * 而且TCP速度较慢
					 */
					// 连接仍保持则发
					if (in != null && out != null) {
						try {
							// 向服务器端端发送信息
							// byte[] buf=jsonStr.getBytes("UTF-8");
							// out.write(buf);
							out.writeUTF(jsonStr);
							out.flush();
							Log.v("tag", "发送数据完毕");
						} catch (IOException ex) {
							Log.v("tag", "发送数据异常");
						}
					} else {
						// 提示
						Log.v("tag", "已和服务器断开连接");
					}
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					Thread.sleep(ConstValue.RecordTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			//发送消息，通知对方关闭连接
			String msgStop=gson.toJson("stoplink");
			try {
				out.writeUTF(msgStop);
				out.flush();
				if(s!=null && (s.isConnected()==true && s.isClosed()==false))
				{
					out.close();
					s.close();
				}
				msgStop=null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			stopAudioRecord();
		}
		
		int initAudioRecord() {
			// 获取缓冲区最小的字节数，以免设定的缓冲区太小
			int minSize = AudioRecord.getMinBufferSize(ConstValue.RateInHz,
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			//Log.v("test", "录音minSize" + minSize);

			/*
			 * 初始化录音对象 audioSource： 录音源
			 * ,sampleRateInHz：默认的采样频率,channelConfig：描述音频通道设置 audioFormat：音频数据支持格式
			 * 单/双通道, bufferSizeInBytes： 缓冲区的总数（字节）
			 */
			try {
				audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
						ConstValue.RateInHz, AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT, minSize*2);//ConstValue.RecordBufferSize);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return minSize*2;
		}
		
		/**
		 * 暂停录音对象的录音，并释放录音资源
		 */
		void stopAudioRecord() {
			if (audioRecord != null) {
				audioRecord.stop();
				audioRecord.release();
				audioRecord = null;
			}
		}

	}
	
	//接收线程
	class ReceiveThread implements Runnable{
		
		String jsonStr=null;//接收到的数据
		Gson gson=new Gson();
		
		public ReceiveThread(){
			
		}
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			try {
				while(stopReceive==true)
				{
/*					if(isServerClose(s)==true)
					{
						stopTalk();
						break;//结束线程
					}*/
					
					//Thread.sleep(10);
					if(in!=null)
					{
						jsonStr = in.readUTF();
						//gson.fromJson(jsonStr, String.class)
						if(jsonStr.equals("stoplink"))
						{
							//Log.d("tag", "stoplink received");
							stopTalk();
							Message msg=Message.obtain();
							msg.what=0x010;
							mHandler.sendMessage(msg);
						}
						else
						{
							//Log.d("tag", "receive sound data");
							byte[] soundBytes=gson.fromJson(jsonStr, byte[].class);
							
							//如果正在播放，或者播放列表的声音包已经够多了，就直接加到缓冲区
							if(isPlayed || trackingSoundList.size()>=playSoundPoolSize){
								buffSoundList.add(soundBytes);
							}
							else
							{
								//如果缓冲区里完全没有声音包则不必担心顺序乱掉，直接加入待播放列表中即可。
								if(buffSoundList.size()==0){
									trackingSoundList.add(soundBytes);
								}else{
									trackingSoundList.addAll(buffSoundList);
									buffSoundList.clear();
									trackingSoundList.add(soundBytes);
								}
							}
							
							//没有正在播放且数目足够就发送消息给主线程，通知其播放
							if(!isPlayed &&trackingSoundList.size()>=playSoundPoolSize){
								isPlayed=true;
								Thread.sleep(10);
							}
						}
					}
					else
					{
						Log.d("tag", "in is null");
					}
				}
				
				//socket必须先连接成功了，且没有关闭
				if(s!=null && (s.isConnected()==true && s.isClosed()==false))
				{
					try {
						//in.close();
						//out.close();
						//s.close();
						s.shutdownInput();//半关闭，还可以使用out发送数据
						in=null;
						//out=null;
						//s=null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.d("tag", "半关闭in异常");
					}
				}
				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
		}//end run()
		
	}
	
	//播放线程
	class PlayThread implements Runnable {
		//private boolean stopTrack = true;
		private AudioTrack audioTrack=null;
		
		public PlayThread(){
			
		}
		
		byte[] buffer=null;
		int bufferSize = ConstValue.TrackBufferSize;
		int realSize=0;
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			// 为空才初始化
			if (audioTrack == null) {
				bufferSize=initAudioTrack();
			}
			
			try {
				audioTrack.play();
			} catch (Exception e) {
				Log.v("tag", "启动播放失败");
				e.printStackTrace();
			}
			Log.v("tag", "开始播放");
			while (stopTrack==true) {

				if(trackingSoundList.size() != 0)
				{
					Log.v("tag", "通话");
					isPlayed=true;
					buffer = trackingSoundList.getFirst(); // short类型对应16bit音频数据格式，byte类型对应于8bit
					bufferSize = buffer.length;
					realSize = audioTrack.write(buffer, 0, bufferSize); // 返回值是个int类型的数据长度值
					trackingSoundList.remove(0);
				}
				else
				{
					isPlayed=false;
				}

				// Log.v("test", "语音包收到后十个包的内容为" + buffer[0] + buffer[150]
				// + buffer[800] + buffer[900]);

				try {
					Thread.sleep(ConstValue.TrackTime);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			isPlayed=false;
			stopAudioTrack();

		}
		
		int initAudioTrack() {
			// 获取缓冲区最小的字节数，以免设定的缓冲区太小
			int minSize = AudioTrack.getMinBufferSize(ConstValue.RateInHz,
					AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
			Log.v("test", "播放minSize" + minSize);

			/*
			 * 初始化录音对象 streamType： 音频流
			 * ,sampleRateInHz：默认的采样频率,channelConfig：描述音频通道设置 audioFormat：音频数据支持格式
			 * 单/双通道, bufferSizeInBytes： 缓冲区的总数（字节），最后一个参数应该是解码模式
			 */
			try {
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						ConstValue.TrackHz, AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, minSize*2,//ConstValue.TrackBufferSize,Const.TrackBufferSize,这个值是不是用minSize
						AudioTrack.MODE_STREAM);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return minSize*2;
		}
		
		/**
		 * 暂停播放，并释放播放资源
		 */
		public void stopAudioTrack() {
			if (audioTrack != null) {
				audioTrack.stop();
				audioTrack.release();
				audioTrack = null;
			}
		}

	}//end PlayThread

}
