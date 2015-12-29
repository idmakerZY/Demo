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
	private Socket s;//����
	DataInputStream in;
	DataOutputStream out;
	
	boolean stopRecord = true;
	boolean stopTrack = true;
	boolean stopReceive=true;
	
	final int playSoundPoolSize=5;//����list��ŵ��������ݸ���
	
	/*���漴��Ҫ������������lingkedList���̲߳���ʱ���������ȫ��������
	 * ���linkList�����������������playSoundPoolSize�趨��
	 * �������list�ᱻ�������ݺͲ������������̷߳��ʣ������ͬ�����⣬�������̰߳�ȫ��list
	 * �����Ȳ����߼��жϽ��ͬ�����⣬�����ܻ�Ӵ��ӳ�
	 */
	private LinkedList<byte[]> trackingSoundList;  
	//������յ�����δ׼�����ŵ���������lingkedList��һ����������
	private LinkedList<byte[]> buffSoundList;
	//�Ƿ����ڲ��ţ�����Խ��պͲ����̶߳�trackingSoundList��ͬ������,true��ʾ���ڲ��š�
	volatile private boolean isPlayed=false;
	Handler mHandler;
/*	// �߳������
	private RecordThread recordThread;

	private PlayThread trackThread;
	
	private ReceiveThread receiveThread;*/
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.talk_activity);
		
		//��һ���жϣ���socket����ͬ��ֵ
		Intent intent=getIntent();
		String wherefrom=intent.getStringExtra("comefrom");
		
		Log.d("tag", "The intent is from"+wherefrom);
		
		if(wherefrom.equals("mainactivity"))
		{
			lt=MainActivity.lt;//������ȡ�����Ƿ�������
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
		
/*		// ��ʼ��¼���߳�
		recordThread = new RecordThread();

		// ��ʼ�������߳�
		trackThread = new PlayThread();
		
		//��ʼ�������߳�
		receiveThread =new ReceiveThread();*/
		
		//�����߳�
		new Thread(new RecordThread()).start();
		new Thread(new PlayThread()).start();
		new Thread(new ReceiveThread()).start();
		
		TextView tx=(TextView)findViewById(R.id.textViewTalk);
		tx.setText("����ͨ��");
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
	* �ж��Ƿ�Ͽ����ӣ��Ͽ�����true,û�з���false 
	* @param socket 
	* @return 
	*/  
	public Boolean isServerClose(Socket socket){  
	   try{  
	    socket.sendUrgentData(0);//����1���ֽڵĽ������ݣ�Ĭ������£���������û�п����������ݴ�����Ӱ������ͨ��  
	    return false;  
	   }catch(Exception se){  
	    return true;  
	   }  
	} 
	
	
	/*
	 * �ڲ���ʵ���߳���
	 */
	
	//¼���߳�
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
			
			// Ϊ�ղų�ʼ��
			if (audioRecord == null) {
				bufferSize=initAudioRecord();
			}
			
			audioRecord.startRecording();
			
			while (stopRecord==true) {
				
				//��ȡ���ݵ�����Ĵ�С��minSize*2�Ƿ�Ҫһ��
				buffer = new byte[bufferSize]; // short���Ͷ�Ӧ16bit��Ƶ���ݸ�ʽ��byte���Ͷ�Ӧ��8bit
				try {
					realSize = audioRecord.read(buffer, 0, bufferSize); // ����ֵ�Ǹ�int���͵����ݳ���ֵ

					// ��������,ֻȡ��Ч���鼴��
					realBuffer = Arrays.copyOfRange(buffer, 0, realSize);

					// Log.v("test", "�������ֽ���"+realSize);

					// Log.v("test", "����������ǰʮ����������Ϊ" + buffer[0] + buffer[150]
					// + buffer[800] + buffer[900]);

					jsonStr = gson.toJson(realBuffer);
					
					/**
					 * ¼���߳����������ʽ����IO����IO����ʱ�����������̣߳����ܻ�©��ĳЩӦ�ü�¼����������
					 * ����TCP�ٶȽ���
					 */
					// �����Ա�����
					if (in != null && out != null) {
						try {
							// ��������˶˷�����Ϣ
							// byte[] buf=jsonStr.getBytes("UTF-8");
							// out.write(buf);
							out.writeUTF(jsonStr);
							out.flush();
							Log.v("tag", "�����������");
						} catch (IOException ex) {
							Log.v("tag", "���������쳣");
						}
					} else {
						// ��ʾ
						Log.v("tag", "�Ѻͷ������Ͽ�����");
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
			//������Ϣ��֪ͨ�Է��ر�����
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
			// ��ȡ��������С���ֽ����������趨�Ļ�����̫С
			int minSize = AudioRecord.getMinBufferSize(ConstValue.RateInHz,
					AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
			//Log.v("test", "¼��minSize" + minSize);

			/*
			 * ��ʼ��¼������ audioSource�� ¼��Դ
			 * ,sampleRateInHz��Ĭ�ϵĲ���Ƶ��,channelConfig��������Ƶͨ������ audioFormat����Ƶ����֧�ָ�ʽ
			 * ��/˫ͨ��, bufferSizeInBytes�� ���������������ֽڣ�
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
		 * ��ͣ¼�������¼�������ͷ�¼����Դ
		 */
		void stopAudioRecord() {
			if (audioRecord != null) {
				audioRecord.stop();
				audioRecord.release();
				audioRecord = null;
			}
		}

	}
	
	//�����߳�
	class ReceiveThread implements Runnable{
		
		String jsonStr=null;//���յ�������
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
						break;//�����߳�
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
							
							//������ڲ��ţ����߲����б���������Ѿ������ˣ���ֱ�Ӽӵ�������
							if(isPlayed || trackingSoundList.size()>=playSoundPoolSize){
								buffSoundList.add(soundBytes);
							}
							else
							{
								//�������������ȫû���������򲻱ص���˳���ҵ���ֱ�Ӽ���������б��м��ɡ�
								if(buffSoundList.size()==0){
									trackingSoundList.add(soundBytes);
								}else{
									trackingSoundList.addAll(buffSoundList);
									buffSoundList.clear();
									trackingSoundList.add(soundBytes);
								}
							}
							
							//û�����ڲ�������Ŀ�㹻�ͷ�����Ϣ�����̣߳�֪ͨ�䲥��
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
				
				//socket���������ӳɹ��ˣ���û�йر�
				if(s!=null && (s.isConnected()==true && s.isClosed()==false))
				{
					try {
						//in.close();
						//out.close();
						//s.close();
						s.shutdownInput();//��رգ�������ʹ��out��������
						in=null;
						//out=null;
						//s=null;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.d("tag", "��ر�in�쳣");
					}
				}
				
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			
		}//end run()
		
	}
	
	//�����߳�
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
			
			// Ϊ�ղų�ʼ��
			if (audioTrack == null) {
				bufferSize=initAudioTrack();
			}
			
			try {
				audioTrack.play();
			} catch (Exception e) {
				Log.v("tag", "��������ʧ��");
				e.printStackTrace();
			}
			Log.v("tag", "��ʼ����");
			while (stopTrack==true) {

				if(trackingSoundList.size() != 0)
				{
					Log.v("tag", "ͨ��");
					isPlayed=true;
					buffer = trackingSoundList.getFirst(); // short���Ͷ�Ӧ16bit��Ƶ���ݸ�ʽ��byte���Ͷ�Ӧ��8bit
					bufferSize = buffer.length;
					realSize = audioTrack.write(buffer, 0, bufferSize); // ����ֵ�Ǹ�int���͵����ݳ���ֵ
					trackingSoundList.remove(0);
				}
				else
				{
					isPlayed=false;
				}

				// Log.v("test", "�������յ���ʮ����������Ϊ" + buffer[0] + buffer[150]
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
			// ��ȡ��������С���ֽ����������趨�Ļ�����̫С
			int minSize = AudioTrack.getMinBufferSize(ConstValue.RateInHz,
					AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
			Log.v("test", "����minSize" + minSize);

			/*
			 * ��ʼ��¼������ streamType�� ��Ƶ��
			 * ,sampleRateInHz��Ĭ�ϵĲ���Ƶ��,channelConfig��������Ƶͨ������ audioFormat����Ƶ����֧�ָ�ʽ
			 * ��/˫ͨ��, bufferSizeInBytes�� ���������������ֽڣ������һ������Ӧ���ǽ���ģʽ
			 */
			try {
				audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
						ConstValue.TrackHz, AudioFormat.CHANNEL_OUT_MONO,
						AudioFormat.ENCODING_PCM_16BIT, minSize*2,//ConstValue.TrackBufferSize,Const.TrackBufferSize,���ֵ�ǲ�����minSize
						AudioTrack.MODE_STREAM);
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return minSize*2;
		}
		
		/**
		 * ��ͣ���ţ����ͷŲ�����Դ
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
