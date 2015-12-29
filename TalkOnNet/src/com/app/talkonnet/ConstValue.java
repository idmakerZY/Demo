package com.app.talkonnet;

public class ConstValue {
	final public static int port=30000;//端口
	
	final public static int newlink=0x123;//新连接的消息
	
	final public static int TrackHz = 8000;//播放频率
	
	final public static int RateInHz = 8000;// 录音的频率，一般电话通话有8000Hz
	
	final public static int RecordBufferSize = 2 * 1024;// 录音缓冲区大小，暂时设定为2k
	
	final public static int TrackBufferSize = 2 * 1024;// 录音缓冲区大小，暂时设定为2k
	
	final public static int RecordTime = 20;// 录音时多久取一次数据，单位是毫秒70
	
	final public static int TrackTime = 10;// 播放过程中停顿多久，单位是毫秒 60

}
