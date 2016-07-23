package ma.release;

public class Jnifp {  
	/* 打开设备
	 * @fdev 设备名
	 * @return:
	 * 		成功: >=0
	 * 		设备打开失败: FP_FILE_FAIL
	 * 		内存分配失败: FP_NOMEM
	 */
	public static native int open(String dev);
	
	/* 关闭设备
	 * @return:
	 * 		成功: FP_OK
	 * 		关闭失败: FP_FILE_FAIL
	 */
	public static native int close();
	
	/* 设置工作
	 * @return 成功: FP_OK
	 */
	public static native int setWork(int doWhat);
	
	/* 校准
	 * @return:
	 *  	成功: FP_OK
	 *  	内存分配失败: FP_NOMEM
	 *		通讯失败：FP_COMM_FAIL
	 *  	选背景失败: FP_BKG_FAIL
	 * 		SQL执行失败: FP_SQL_FAIL
	 */
	public static native int calibrate();
	
	/* 开机初始化
	 * @return:
	 *		成功: FP_OK
	 *  	SQL执行失败: FP_SQL_FAIL
	 */
	public static native int initBoot();
	
	/* 获取厂商信息
	 * @buf 厂商信息
	 * @len 长度
	 * @return 成功: FP_OK
	 *  	通讯失败: FP_COMM_FAIL
	 */
	public static native String getVendor();
	
	/* 获取库版本
	 * @buf 版本信息
	 * @len 长度
	 * @return 成功：FP_OK
	 */
	public static native String getVersion();
	
	/* 打开数据库
	 * @path 数据库路径
	 * @return:
	 *  	成功: FP_OK
	 *  	失败: FP_DB_FAIL
	 */
	public static native int dbOpen(String path);
	
	/* 更新数据 (内存模板更新到数据库)
	 * @fid 指纹ID
	 * @return:
	 * 		成功: FP_OK
	 *		SQL执行失败: FP_SQL_FAIL
	 */
	public static native int dbUpdate(int fid);
			
	/* 关闭数据库
	 * @return:
	 *  	成功: FP_OK
	 */
	public static native int dbClose();
	
	/* 手指检测
	 * @timeout 超时
	 * @return
	 * 		无手指：FP_OK
	 * 		全部接触: FP_CHK_FULL
	 * 		部分接触：FP_CHK_PART
	 * 		手指离开: FP_CHK_UP
	 */
	public static native int check(int timeout);
	
	/* 初始化注册
	 * @return 成功: FP_OK
	 */
	public static native int initEnroll(int times);
	
	/* 注册指纹
	 * @fid 指纹ID, 范围：1～5
	 * @return:
	 * 		成功: >0
	 * 		注册失败: FP_ENROLL_FAIL
	 * 		参数错误: FP_PARA_ERROR
	 */
	public static native int enroll(int fid);
	
	/* 匹配指纹
	 * @fid 手指id
	 * @level 等级（1～MAXF由低到高）
	 * @return:
	 * 		成功: FP_OK
	 * 		空指纹: FP_EMPTY
	 * 		匹配失败: FP_MATCH_FAIL
	 * 		参数错误: FP_PARA_ERROR
	 */
	public static native int match(int fid);
	
	/* 清除注册指纹
	 * @fid 指纹ID
	 * @return:
	 * 		成功: FP_OK
	 * 		参数错误: FP_PARA_ERROR
	 * 		SQL执行失败: FP_SQL_FAIL
	 */
	public static native int clear(int fid);
	
	/* 获取注册状态
	 * @fid 指纹ID
	 * @return:
	 * 		已注册: >0
	 * 		未注册: =0
	 */
	public static native int doState(int fid);
	
	/* 采图模式
	 * @return:
	 * 		成功: FP_OK
	 *  	通讯失败: FP_COMM_FAIL
	 * 		参数错误: FP_PARA_ERROR
	 */
	public static native int imageMode();
	
	/* 测试SPI
	 * @return:
	 *  	成功: FP_OK
	 *  	通讯失败: FP_COMM_FAIL
	 */
	public static native int testSpi();
	
	/* 测试图像
	 * @bmp 位图数据
	 * @len 位图大小
	 * @return 分数值
	 */
	public static native int testImage(byte[] buf, int len);
	
	/* 测试中断
	 * @return
	 *  	成功: 电平(0或1）
	 *  	通讯失败: FP_COMM_FAIL
	 */
	public static native int testInterrupt();
	
	/* 测试坏点
	 * @return 成功: FP_OK
	 * 		坏点: FP_BAD_PIXEL
	 * 		通讯失败:FP_COMM_FAIL
	 */
	public static native int testBadPixel();
	
	/* 是否按压
	 * @return 未按压: FP_OK
	 * 		按压: FP_CHK_DOWN
	 * 		通讯失败: FP_COMM_FAIL
	 */
	public static native int testPress();
} 


