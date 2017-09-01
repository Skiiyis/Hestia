package io.github.sawameimei.audio;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * 该实例中，我们使用AudioRecord类来完成我们的音频录制程序
 * AudioRecord类，我们可以使用三种不同的read方法来完成录制工作，
 * 每种方法都有其实用的场合
 * 一、实例化一个AudioRecord类我们需要传入几种参数
 * 1、AudioSource：这里可以是MediaRecorder.AudioSource.MIC
 * 2、SampleRateInHz:录制频率，可以为8000hz或者11025hz等，不同的硬件设备这个值不同
 * 3、ChannelConfig:录制通道，可以为AudioFormat.CHANNEL_CONFIGURATION_MONO和AudioFormat.CHANNEL_CONFIGURATION_STEREO
 * 4、AudioFormat:录制编码格式，可以为AudioFormat.ENCODING_16BIT和8BIT,其中16BIT的仿真性比8BIT好，但是需要消耗更多的电量和存储空间
 * 5、BufferSize:录制缓冲大小：可以通过getMinBufferSize来获取
 * 这样我们就可以实例化一个AudioRecord对象了
 * 二、创建一个文件，用于保存录制的内容
 * 同上篇
 * 三、打开一个输出流，指向创建的文件
 * DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))
 * 四、现在就可以开始录制了，我们需要创建一个字节数组来存储从AudioRecorder中返回的音频数据，但是
 * 注意，我们定义的数组要小于定义AudioRecord时指定的那个BufferSize
 * short[]buffer = new short[BufferSize/4];
 * startRecording();
 * 然后一个循环，调用AudioRecord的read方法实现读取
 * 另外使用MediaPlayer是无法播放使用AudioRecord录制的音频的，为了实现播放，我们需要
 * 使用AudioTrack类来实现
 * AudioTrack类允许我们播放原始的音频数据
 * <p>
 * <p>
 * 一、实例化一个AudioTrack同样要传入几个参数
 * 1、StreamType:在AudioManager中有几个常量，其中一个是STREAM_MUSIC;
 * 2、SampleRateInHz：最好和AudioRecord使用的是同一个值
 * 3、ChannelConfig：同上
 * 4、AudioFormat：同上
 * 5、BufferSize：通过AudioTrack的静态方法getMinBufferSize来获取
 * 6、Mode：可以是AudioTrack.MODE_STREAM和MODE_STATIC，关于这两种不同之处，可以查阅文档
 * 二、打开一个输入流，指向刚刚录制内容保存的文件，然后开始播放，边读取边播放
 **/

/**
 * 音频基础知识参考 http://ticktick.blog.51cto.com/823160/1748506
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{RECORD_AUDIO, READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE, INTERNET, CAMERA}, 1);
        }

        findViews();
    }

    private TextView title;
    private Button _switch;
    private Button record;
    private Button recordingStop;
    private Button playing;
    private AudioOperation[] operations = new AudioOperation[]{new PCMRecordByAudioRecorder(), new ThreeGPRecorderByMediaRecorder(), new WAVRecordByAudioRecorder()};
    private int currentOperation = 0;

    /**
     * Find the Views in the layout<br />
     * <br />
     * Auto-created on 2017-09-01 10:49:45 by Android Layout Finder
     * (http://www.buzzingandroid.com/tools/android-layout-finder)
     */
    private void findViews() {
        title = (TextView) findViewById(R.id.title);
        _switch = (Button) findViewById(R.id._switch);
        record = (Button) findViewById(R.id.record);
        recordingStop = (Button) findViewById(R.id.recording_stop);
        playing = (Button) findViewById(R.id.playing);
        title.setText("存储路径:\n" + operations[currentOperation].filePath());

        _switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentOperation++;
                if (currentOperation >= operations.length) {
                    currentOperation = 0;
                }
                title.setText("存储路径:\n" + operations[currentOperation].filePath());
            }
        });
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                operations[currentOperation].startRecord();
            }
        });
        recordingStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                operations[currentOperation].stopRecord();
            }
        });
        playing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                operations[currentOperation].play();
            }
        });
    }
}
