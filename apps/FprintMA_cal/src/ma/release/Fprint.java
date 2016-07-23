package ma.release;

import java.io.File;

public class Fprint {
	// 返回值类型
	public static final int FP_CANCEL = 8; // 取消
	public static final int FP_FREE = 7; // 闲
	public static final int FP_BUSY = 6; // 忙
	public static final int FP_CHK_UP = 5; // 手指离开
	public static final int FP_PRESSED = 4; // 手指已按
	public static final int FP_CHK_FULL = 3; // 全部接触
	public static final int FP_CHK_PART = 2; // 部分接触
	public static final int FP_CHK_DOWN = 1; // 手指按下
	public static final int FP_OK = 0; // 成功/完成
	public static final int FP_ENROLL_FAIL = -1; // 注册失败
	public static final int FP_MATCH_FAIL = -2; // 匹配失败
	public static final int FP_EMPTY = -3; // 空指纹
	public static final int FP_NEWF_FAIL = -4; // 创建FID失败
	public static final int FP_FILE_FAIL = -10; // 文件打开/关闭失败
	public static final int FP_NOMEM = -11; // 内存分配失败
	public static final int FP_PARA_ERROR = -12; // 参数错误
	public static final int FP_COMM_FAIL = -13; // 通讯失败
	public static final int FP_DB_FAIL = -20; // 数据库打开/关闭失败
	public static final int FP_SQL_FAIL = -21; // SQL执行失败
	public static final int FP_SQL_EMPTY = -22; // SQL数据为空
	public static final int FP_NOCAP = -30; // 未采集数据
	public static final int FP_BKG_FAIL = -31; // 选背景失败
	public static final int FP_IMG_ZERO = -32; // 采集数据为0
	public static final int FP_BAD_PIXEL =-33;	//坏点
	
	public static final int DO_DEBUG = 5;
	public static final int DO_RESTORE = 6;
	public static final int TRUE = 1;
	public static final int FALSE = 0;	
	public static boolean bStop = false;

	static {
		System.loadLibrary("fprint-x64");
	}
  
	public static String getPath() { 
		//File dir = new File("/data/data/ma.calibrate/files");
		File dir = new File("/data/system/users/0/fpdata");
		if (!dir.exists()) {
			try {
				dir.mkdir();
			} catch (Exception e) {
			}
		}
		return dir.toString();
	}

	public static int open() {
		int ret = Jnifp.open("/dev/madev0");
		if (ret >= 0) {
			Jnifp.setWork(DO_DEBUG);
			Util.sleep(200);
			ret = Jnifp.dbOpen(getPath());
		}
		return ret;
	}

	public static int close() {
		int ret;
		ret = Jnifp.setWork(DO_RESTORE);
		//Util.dprint("JTAG", "close setWork ret=" + ret);		
		Jnifp.dbClose(); 
		ret = Jnifp.close();
		Util.dprint("JTAG", "close ret=" + ret);
		return ret;
	}

	public static int calibrate() {
		return Jnifp.calibrate();
	}
	
	public static int initBoot() {
		return Jnifp.initBoot();
	}

	public static int testImage(byte[] buf, int len) {
		return Jnifp.testImage(buf, len);
	}	
	
	public static int testBadPixel() {
		return Jnifp.testBadPixel();
	}

	public static int testPress() {
		return Jnifp.testPress();
	}

	public static int imageMode() {
		return Jnifp.imageMode();
	}
}
