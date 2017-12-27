rtmp推拉流的两种实现

```
SimpleRtmp 纯java实现
https://github.com/faucamp/SimpleRtmp

LibRTMP C/ C++ 实现
http://blog.csdn.net/leixiaohua1020/article/details/15814587

使用FFMpeg实现
http://blog.csdn.net/leixiaohua1020/article/details/39803457
```

rtmp协议通信流程

```
http://blog.csdn.net/simongyley/article/details/29851337
http://wwwimages.adobe.com/www.adobe.com/content/dam/acom/en/devnet/rtmp/pdf/rtmp_specification_1.0.pdf
http://blog.csdn.net/leixiaohua1020/article/details/11704355
http://blog.csdn.net/lipengshiwo/article/details/53267674
```

一些问题
```
RTMP传输数据是是否需要封装？FLV？
https://www.cnblogs.com/haibindev/archive/2011/12/29/2305712.html

RTMP发送audio/vedio [H264 AAC]数据之前，要先发送aac/avc sequence header
发送的audio/vedio数据结构和flvTag的结构类似

RTMP发送H264要先发送PPS,SPS数据？
RTMP发送FlvTag字段却不用先发送PPS，SPS？
http://blog.csdn.net/bsplover/article/details/7426511

答案是Flv 中有aac/avc sequence header 的TAG 而且总是第一个。。
http://blog.csdn.net/mm792261167/article/details/69396493

FlvTag video payload/RTMP video payload的内容是什么？
?去除掉同步字的h264 NALU??

RTMP延迟？
https://zhuanlan.zhihu.com/p/24606221
```