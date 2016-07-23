
package ma.calibrate;

import ma.release.Fprint;
import ma.release.Util;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import ma.calibrate.R;

public class FactoryActivity extends Activity implements OnClickListener {
    private final int MSG_IDLE = 0x100;
    private final int MSG_OPEN_FAIL = 0x101;
    private final int MSG_OPEN_PASS = 0x102;
    private final int MSG_INIT_START = 0x103;
    private final int MSG_INIT_FINISH = 0x104;
    private final int MSG_END = 0x105;
    private final int MSG_WAIT_LAST_INIT_TIMEOUT = 0x106;

    // 每个阶段的进度值
    private final int PROGRESS_NONE = 0; // 初始状态
    private final int PROGRESS_OPEN_PASS = 5; // 设备打开完成
    private final int PROGRESS_INIT_START = 10; // 出厂设置开始
    private final int PROGRESS_INIT_FINISH = 95; // 出厂设置结束
    private final int PROGRESS_ALL_END = 100; // 结束

    // 用户提示内容
    private String strProgressPrefix = "";
    private String strUserTipsText = "";
    private TextView userTipsTextView;

    // 当前进度
    private int nProgressValue = 0;
    private TextView textProgress;
    private ProgressBar progressBar;

    private boolean thread_stop_flag = false;
    private boolean bTimeCountStop = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setHomeButtonEnabled(true);
        }
        
        setContentView(R.layout.ma_factory);
        
        userTipsTextView = (TextView) findViewById(R.id.ma_factory_text);
        textProgress = (TextView) findViewById(R.id.ma_factory_process_txt);
        progressBar = (ProgressBar) findViewById(R.id.progress_horizontal);

        strProgressPrefix = getResources().getString(R.string.ma_factory_progress);
        textProgress.setText(strProgressPrefix + PROGRESS_NONE);

        // 打开界面时提示用户
        strUserTipsText = getResources().getString(R.string.ma_factory_begin) + "\n";
        //userTipsTextView.setText(strUserTipsText);

        textProgress.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);

        new MyThread().start();
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        thread_stop_flag = true;
    }

    @Override
    public void onClick(View arg0) {

    }

    // Fprint.open()和Fprint.initFactory()失败时的提示
    private String errMsg(int ret) {
        String strErrMsg = "";
        switch (ret) {
            case -1:
                strErrMsg = getResources().getString(R.string.ma_fprint_open_neg1);
                break;
            case -2:
                strErrMsg = getResources().getString(R.string.ma_fprint_open_neg2);
                break;
            case -3:
                strErrMsg = getResources().getString(R.string.ma_fprint_open_neg3);
                break;
            case -4:
                strErrMsg = getResources().getString(R.string.ma_fprint_open_neg4);
                break;
            case -11:
                strErrMsg = getResources().getString(R.string.ma_fprint_initfactory_neg1);
                break;
            case -12:
                strErrMsg = getResources().getString(R.string.ma_fprint_initfactory_neg2);
                break;
            case -13:
                strErrMsg = getResources().getString(R.string.ma_fprint_initfactory_neg3);
                break;
            case -14:
                strErrMsg = getResources().getString(R.string.ma_fprint_initfactory_neg4);
                break;
            case -15:
                strErrMsg = getResources().getString(R.string.ma_fprint_initfactory_neg5);
                break;
            case -21:
                strErrMsg = getResources().getString(R.string.ma_fprint_initboot_neg1);
                break;
            case -22:
                strErrMsg = getResources().getString(R.string.ma_fprint_initboot_neg2);
                break;
            case -23:
                strErrMsg = getResources().getString(R.string.ma_fprint_initboot_neg3);
                break;
        }

        return strErrMsg;
    }

    class MyThread extends Thread {

        @Override
        public void run() {
            boolean flag = false;
            for (int i = 0; i < 5; i++) {
                Log.i("JTAG", "I=" + Integer.toString(i));

                if (thread_stop_flag)
                    break;
                Message msg = new Message();
                msg.what = MSG_IDLE;
                if (i == 0) {
                    int ret = Fprint.open();
                    if (ret < 0) {
                        strUserTipsText = errMsg(ret);
                        msg.what = MSG_OPEN_FAIL;
                        thread_stop_flag = true;
                    } else {
                        strUserTipsText = getResources().getString(R.string.ma_factory_prompt);
                        msg.what = MSG_OPEN_PASS;
                    }
                } else if (i == 1) {
                    // 更新出厂设置进度
                    new Thread(new initFactoryRunnable()).start();

                    strUserTipsText = getResources().getString(R.string.ma_factory_capture_begin);
                    msg.what = MSG_INIT_START;
                } else if (i == 2) {
                    int ret = Fprint.calibrate();
                    strUserTipsText = "";
                    if (ret < 0) {
                        strUserTipsText = errMsg(ret - 10) + "\n";
                        thread_stop_flag = true;
                    } else {
                        ret = Fprint.calibrate();
                        if (ret < 0) {
                            strUserTipsText = errMsg(ret - 20) + "\n";
                            thread_stop_flag = true;
                        }
                    }
                    strUserTipsText += getResources().getString(R.string.ma_factory_capture_stop);

                    bTimeCountStop = true;
                    msg.what = MSG_INIT_FINISH;
                } else if (i == 3) {// 显示文字-结束，出厂设置完成
                    strUserTipsText = getResources().getString(
                            R.string.ma_factory_turn_to_capture_page);
                    msg.what = MSG_END;
                } else if (i == 4) {// 文字都显示完成后跳转到Capture界面
                    // msg.what = MSG_EXIT;
                    //Intent it = new Intent(FactoryActivity.this, CaptureActivity.class);
                    //startActivity(it);
                    //Util.writeXML(getApplicationContext(), "calibrated", 1);
                    finish();
                    break;
                }
                mHandler.sendMessage(msg);
                // int time = (i == 0 || i >1) ? 1000 : 100;
                // Util.sleep(time);
            }
            // Fprint.close();
        }
    }

    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i("JTAG", "what=" + Integer.toHexString(msg.what));
            switch (msg.what) {
                case MSG_OPEN_FAIL: // 设备打开失败
                    //userTipsTextView.append(strUserTipsText + "\n");
                    progressBar.setProgress(PROGRESS_NONE);
                    progressBar.setSecondaryProgress(PROGRESS_NONE);
                    break;
                case MSG_OPEN_PASS: // 设备打开成功后告知用户不要触摸模组

                    nProgressValue = PROGRESS_OPEN_PASS;
                    progressBar.setProgress(nProgressValue);
                    progressBar.setSecondaryProgress(PROGRESS_INIT_START);
                    textProgress.setText(strProgressPrefix + nProgressValue);

                    //userTipsTextView.append(strUserTipsText + "\n");
                    break;
                case MSG_INIT_START: // 出厂设置
                    textProgress.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.VISIBLE);

                    nProgressValue = PROGRESS_INIT_START;

                    progressBar.setProgress(nProgressValue);
                    progressBar.setSecondaryProgress(nProgressValue + 1);
                    textProgress.setText(strProgressPrefix + nProgressValue);
                    //userTipsTextView.append(strUserTipsText + "\n");

                    break;
                case MSG_INIT_FINISH: // 出厂设置完成
                    if (!thread_stop_flag) {// 初始化成功
                        nProgressValue = PROGRESS_INIT_FINISH;

                        progressBar.setProgress(nProgressValue);
                        progressBar.setSecondaryProgress(PROGRESS_ALL_END);
                        textProgress.setText(strProgressPrefix + nProgressValue);
                    } else {
                        nProgressValue = PROGRESS_ALL_END;

                        progressBar.setProgress(nProgressValue);
                        progressBar.setSecondaryProgress(PROGRESS_ALL_END);
                        textProgress.setText(strProgressPrefix + nProgressValue);
                    }

                    //userTipsTextView.append(strUserTipsText + "\n");

                    break;
                case MSG_END:
                    nProgressValue = PROGRESS_ALL_END;

                    progressBar.setProgress(nProgressValue);
                    progressBar.setSecondaryProgress(PROGRESS_ALL_END);
                    textProgress.setText(strProgressPrefix + nProgressValue);
                    //userTipsTextView.append(strUserTipsText + "\n");

                    break;
                case MSG_WAIT_LAST_INIT_TIMEOUT:
                    //userTipsTextView.append(strUserTipsText + "\n");
                    break;
            }

        }
    };

    // initFactory()时的更新进度条
    Handler initFactoryHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1 && nProgressValue < PROGRESS_INIT_FINISH) {
                progressBar.setProgress(nProgressValue);
                if (nProgressValue == PROGRESS_INIT_FINISH) {
                    progressBar.setSecondaryProgress(PROGRESS_ALL_END);
                } else {
                    progressBar.setSecondaryProgress(nProgressValue + 1);
                }

                textProgress.setText(strProgressPrefix + nProgressValue);
                Util.dprint("JTAG","Factory initFactoryHandler: MSG_END nProgress=" + nProgressValue
                        + " next pro=" + nProgressValue + 1);
            }
        };
    };

    // initFactory()时的更新线程
    class initFactoryRunnable implements Runnable {
        @Override
        public void run() {
            Util.dprint("JTAG","initFactoryRunnable begin to run");
            int nCount = 0;
            while (true) {
                try {
                    if (bTimeCountStop || nProgressValue >= PROGRESS_INIT_FINISH) {
                        Util.dprint("JTAG","Factory initFactoryRunnable: break");
                        break;
                    }
                    Util.dprint("JTAG","Factory initFactoryRunnable(1) index=" + nCount++);
                    nProgressValue++;
                    Thread.sleep(50);
                    Util.dprint("JTAG","Factory initFactoryRunnable(2) index=" + nCount);

                    Message msg = new Message();
                    msg.what = 1;
                    initFactoryHandler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Util.dprint("JTAG","Factory initFactoryRunnable finish");
        }
    }

}
