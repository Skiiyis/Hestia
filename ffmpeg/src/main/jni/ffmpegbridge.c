#include <jni.h>
#include <libavutil/imgutils.h>
#include "ffmpeg.h"
#include "ffmpeglog.h"
#include "ffmpegbrideg_muxing.h"

JNIEXPORT jint JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_commandRun(JNIEnv *env, jclass type,
                                                         jobjectArray cmd) {

    int argc = (*env)->GetArrayLength(env, cmd);
    char *argv[argc];
    for (int i = 0; i < argc; i++) {
        jstring command = (*env)->GetObjectArrayElement(env, cmd, i);
        argv[i] = (char *) (*env)->GetStringUTFChars(env, command, 0);
    }
    //解析command给ffmepg
    LOGE(ISDEBUG, "到jni层了");
    return command_line_run(argc, argv);
}

JNIEXPORT jstring JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_supportedProtocol(JNIEnv *env, jclass type) {

    char info[40000] = {0};
    av_register_all();

    struct URLProtocol *pup = NULL;
    //Input
    struct URLProtocol **p_temp = &pup;
    avio_enum_protocols((void **) p_temp, 0);
    while ((*p_temp) != NULL) {
        sprintf(info, "%s[In ][%10s]\n", info, avio_enum_protocols((void **) p_temp, 0));
    }
    pup = NULL;
    //Output
    avio_enum_protocols((void **) p_temp, 1);
    while ((*p_temp) != NULL) {
        sprintf(info, "%s[Out][%10s]\n", info, avio_enum_protocols((void **) p_temp, 1));
    }

    //LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jstring JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_supportedAVFormat(JNIEnv *env, jclass type) {

    char info[40000] = {0};

    av_register_all();

    AVInputFormat *if_temp = av_iformat_next(NULL);
    AVOutputFormat *of_temp = av_oformat_next(NULL);
    //Input
    while (if_temp != NULL) {
        sprintf(info, "%s[In ][%10s]\n", info, if_temp->name);
        if_temp = if_temp->next;
    }
    //Output
    while (of_temp != NULL) {
        sprintf(info, "%s[Out][%10s]\n", info, of_temp->name);
        of_temp = of_temp->next;
    }
    //LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jstring JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_supportedAVCodecInfo(JNIEnv *env, jclass type) {

    char info[40000] = {0};

    av_register_all();

    AVCodec *c_temp = av_codec_next(NULL);

    while (c_temp != NULL) {
        if (c_temp->decode != NULL) {
            sprintf(info, "%s[Dec]", info);
        } else {
            sprintf(info, "%s[Enc]", info);
        }
        switch (c_temp->type) {
            case AVMEDIA_TYPE_VIDEO:
                sprintf(info, "%s[Video]", info);
                break;
            case AVMEDIA_TYPE_AUDIO:
                sprintf(info, "%s[Audio]", info);
                break;
            default:
                sprintf(info, "%s[Other]", info);
                break;
        }
        sprintf(info, "%s[%10s]\n", info, c_temp->name);


        c_temp = c_temp->next;
    }

    return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jstring JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_supportedAVFilterInfo(JNIEnv *env, jclass type) {

    char info[40000] = {0};
    avfilter_register_all();
    AVFilter *f_temp = (AVFilter *) avfilter_next(NULL);
    sprintf(info, "%s[%10s]\n", info, f_temp->name);
    LOGE(ISDEBUG, "avFilterInfo:%s", info);

    return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jstring JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_ffmpegConfigInfo(JNIEnv *env, jclass type) {
    char info[10000] = {0};
    av_register_all();

    sprintf(info, "%s\n", avcodec_configuration());

    //LOGE("%s", info);
    return (*env)->NewStringUTF(env, info);
}

JNIEXPORT void JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_decode(JNIEnv *env, jclass type, jstring filePath_,
                                                     jobject callBack) {

    jclass jClass = (*env)->GetObjectClass(env, callBack);
    jmethodID method_OnDecode = (*env)->GetMethodID(env, jClass, "onDecode", "([B)V");

    AVFormatContext *pFormatCtx;
    int i, videoindex;
    AVCodecContext *pCodecCtx;
    AVCodec *pCodec;
    AVFrame *pFrame, *pFrameYUV;
    uint8_t *out_buffer;
    AVPacket *packet;
    int y_size;
    int ret, got_picture;
    struct SwsContext *img_convert_ctx;
    int frame_cnt;
    clock_t time_start, time_finish;
    double time_duration = 0.0;

    char input_str[500] = {0};
    char info[1000] = {0};

    //初始化准备
    av_register_all();
    avformat_network_init();
    pFormatCtx = avformat_alloc_context();
    sprintf(input_str, "%s", (*env)->GetStringUTFChars(env, filePath_, NULL));

    if (avformat_open_input(&pFormatCtx, input_str, NULL, NULL) != 0) {
        LOGE(ISDEBUG, "Couldn't open input stream.\n");
        return;
    }
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGE(ISDEBUG, "Couldn't find stream information.\n");
        return;
    }

    //获取视频信道
    videoindex = -1;
    for (i = 0; i < pFormatCtx->nb_streams; i++)
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoindex = i;
            break;
        }
    if (videoindex == -1) {
        LOGE(ISDEBUG, "Couldn't find a video stream.\n");
        return;
    }

    //获取可用的codec
    pCodecCtx = pFormatCtx->streams[videoindex]->codec;
    pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGE(ISDEBUG, "Couldn't find Codec.\n");
        return;
    }
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGE(ISDEBUG, "Couldn't open codec.\n");
        return;
    }

    pFrame = av_frame_alloc();
    pFrameYUV = av_frame_alloc();
    out_buffer = (unsigned char *) av_malloc(
            av_image_get_buffer_size(AV_PIX_FMT_YUV420P, pCodecCtx->width, pCodecCtx->height, 1));
    av_image_fill_arrays(pFrameYUV->data, pFrameYUV->linesize, out_buffer, AV_PIX_FMT_YUV420P,
                         pCodecCtx->width, pCodecCtx->height, 1);

    packet = (AVPacket *) av_malloc(sizeof(AVPacket));

    img_convert_ctx = sws_getContext(pCodecCtx->width, pCodecCtx->height, pCodecCtx->pix_fmt,
                                     pCodecCtx->width, pCodecCtx->height, AV_PIX_FMT_YUV420P,
                                     SWS_BICUBIC, NULL, NULL, NULL);


    sprintf(info, "[Input     ]%s\n", input_str);
    sprintf(info, "%s[Format    ]%s\n", info, pFormatCtx->iformat->name);
    sprintf(info, "%s[Codec     ]%s\n", info, pCodecCtx->codec->name);
    sprintf(info, "%s[Resolution]%dx%d\n", info, pCodecCtx->width, pCodecCtx->height);

    frame_cnt = 0;
    time_start = clock();

    //开始decode
    jbyteArray byte_Array = NULL;

    while (av_read_frame(pFormatCtx, packet) >= 0) {
        if (packet->stream_index == videoindex) {
            ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
            if (ret < 0) {
                LOGE(ISDEBUG, "Decode Error.\n");
                return;
            }
            if (got_picture) {
                sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize,
                          0, pCodecCtx->height,
                          pFrameYUV->data, pFrameYUV->linesize);

                y_size = pCodecCtx->width * pCodecCtx->height;

                //填充yuv数据
                if (byte_Array == NULL) {
                    byte_Array = (*env)->NewByteArray(env, y_size * 6);
                }
                (*env)->SetByteArrayRegion(env, byte_Array, 0, y_size,
                                           (const jbyte *) pFrameYUV->data[0]);
                (*env)->SetByteArrayRegion(env, byte_Array, y_size, y_size * 5 / 4,
                                           (const jbyte *) pFrameYUV->data[1]);
                (*env)->SetByteArrayRegion(env, byte_Array, y_size * 5 / 4, y_size * 3 / 2,
                                           (const jbyte *) pFrameYUV->data[2]);
                (*env)->CallVoidMethod(env, callBack, method_OnDecode, byte_Array);
                //Output info
                char pictype_str[10] = {0};
                switch (pFrame->pict_type) {
                    case AV_PICTURE_TYPE_I:
                        sprintf(pictype_str, "I");
                        break;
                    case AV_PICTURE_TYPE_P:
                        sprintf(pictype_str, "P");
                        break;
                    case AV_PICTURE_TYPE_B:
                        sprintf(pictype_str, "B");
                        break;
                    default:
                        sprintf(pictype_str, "Other");
                        break;
                }
                LOGE(ISDEBUG, "Frame Index: %5d. Type:%s", frame_cnt, pictype_str);
                frame_cnt++;
            }
        }
        av_free_packet(packet);
    }
    //flush decoder
    //FIX: Flush Frames remained in Codec
    while (1) {
        ret = avcodec_decode_video2(pCodecCtx, pFrame, &got_picture, packet);
        if (ret < 0)
            break;
        if (!got_picture)
            break;
        sws_scale(img_convert_ctx, (const uint8_t *const *) pFrame->data, pFrame->linesize, 0,
                  pCodecCtx->height,
                  pFrameYUV->data, pFrameYUV->linesize);
        int y_size = pCodecCtx->width * pCodecCtx->height;
        //填充yuv数据
        if (byte_Array == NULL) {
            byte_Array = (*env)->NewByteArray(env, y_size * 3 / 2);
        }
        (*env)->SetByteArrayRegion(env, byte_Array, 0, y_size,
                                   (const jbyte *) pFrameYUV->data[0]);
        (*env)->SetByteArrayRegion(env, byte_Array, y_size, y_size * 5 / 4,
                                   (const jbyte *) pFrameYUV->data[1]);
        (*env)->SetByteArrayRegion(env, byte_Array, y_size * 5 / 4, y_size * 3 / 2,
                                   (const jbyte *) pFrameYUV->data[2]);
        (*env)->CallVoidMethod(env, callBack, method_OnDecode, byte_Array);
        //Output info
        char pictype_str[10] = {0};
        switch (pFrame->pict_type) {
            case AV_PICTURE_TYPE_I:
                sprintf(pictype_str, "I");
                break;
            case AV_PICTURE_TYPE_P:
                sprintf(pictype_str, "P");
                break;
            case AV_PICTURE_TYPE_B:
                sprintf(pictype_str, "B");
                break;
            default:
                sprintf(pictype_str, "Other");
                break;
        }
        LOGE(ISDEBUG, "Frame Index: %5d. Type:%s", frame_cnt, pictype_str);
        frame_cnt++;
    }
    time_finish = clock();
    time_duration = (double) (time_finish - time_start);

    sprintf(info, "%s[Time      ]%fms\n", info, time_duration);
    sprintf(info, "%s[Count     ]%d\n", info, frame_cnt);

    sws_freeContext(img_convert_ctx);
    av_frame_free(&pFrameYUV);
    av_frame_free(&pFrame);
    avcodec_close(pCodecCtx);
    avformat_close_input(&pFormatCtx);

}

static AVFormatContext *pFormatCtx;
static AVOutputFormat *fmt;
static AVStream *video_st;
static AVCodecContext *pCodecCtx;
static AVCodec *pCodec;
static AVFrame *pFrame;
static AVPacket pkt;
static uint8_t *picture_buf;
static int y_size;
static int frameIndex;

JNIEXPORT jint JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_prepareEncoder(JNIEnv *env, jclass type,
                                                             jstring outputUrl_, jint width,
                                                             jint height, jstring mineType_,
                                                             jint bitRate, jint frameRate) {
    const char *outputUrl_str = (*env)->GetStringUTFChars(env, outputUrl_, 0);
    const char *mineType = (*env)->GetStringUTFChars(env, mineType_, 0);
    frameIndex = 0;
    bitRate = 400000;

    av_register_all();
    pFormatCtx = avformat_alloc_context();
    //Guess Format
    fmt = av_guess_format(NULL, outputUrl_str, NULL);
    pFormatCtx->oformat = fmt;

    if (avio_open(&pFormatCtx->pb, outputUrl_str, AVIO_FLAG_READ_WRITE) < 0) {
        LOGE(ISDEBUG, "Failed to open output file! \n");
        return -1;
    }
    video_st = avformat_new_stream(pFormatCtx, 0);
    video_st->time_base.num = 1;
    video_st->time_base.den = frameRate;

    if (video_st == NULL) {
        return -1;
    }
    //Param that must set
    pCodecCtx = video_st->codec;
    pCodecCtx->codec_id = fmt->video_codec;
    pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
    pCodecCtx->pix_fmt = AV_PIX_FMT_YUV420P;
    pCodecCtx->width = width;
    pCodecCtx->height = height;
    pCodecCtx->time_base.num = 1;
    pCodecCtx->time_base.den = frameRate;
    pCodecCtx->bit_rate = bitRate;
    pCodecCtx->gop_size = 250;

    //H264
    //pCodecCtx->me_range = 16;
    //pCodecCtx->max_qdiff = 4;
    //pCodecCtx->qcompress = 0.6;
    pCodecCtx->qmin = 10;
    pCodecCtx->qmax = 51;
    //Optional Param
    pCodecCtx->max_b_frames = 3;

    // Set Option
    AVDictionary *param = 0;
    pCodec = avcodec_find_encoder(pCodecCtx->codec_id);
    if (!pCodec) {
        LOGE(ISDEBUG, "Can not find encoder! \n");
        return -1;
    }
    if (avcodec_open2(pCodecCtx, pCodec, &param) < 0) {
        LOGE(ISDEBUG, "Failed to open encoder! \n");
        return -1;
    }
    pFrame = av_frame_alloc();
    int picture_size = avpicture_get_size(pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height);
    av_image_fill_arrays(pFrame->data, pFrame->linesize, NULL,
                         pCodecCtx->pix_fmt, pCodecCtx->width, pCodecCtx->height, 1);
    avformat_write_header(pFormatCtx, NULL);
    av_new_packet(&pkt, picture_size);
    y_size = pCodecCtx->width * pCodecCtx->height;
    (*env)->ReleaseStringUTFChars(env, mineType_, mineType);
    (*env)->ReleaseStringUTFChars(env, outputUrl_, outputUrl_str);
    return 0;
}

JNIEXPORT jint JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_encode(JNIEnv *env, jclass type,
                                                     jbyteArray yuv420p_) {
    jbyte *yuv420p = (*env)->GetByteArrayElements(env, yuv420p_, NULL);

    pFrame->data[0] = (uint8_t *) (yuv420p);
    pFrame->data[1] = (uint8_t *) (yuv420p + (size_t) y_size);
    pFrame->data[2] = (uint8_t *) (yuv420p + (size_t) y_size * 5 / 4);
    //PTS
    pFrame->pts = frameIndex;
    int got_picture = 0;
    //Encode
    int ret = avcodec_encode_video2(pCodecCtx, &pkt, pFrame, &got_picture);
    if (ret < 0) {
        LOGE(ISDEBUG, "Failed to encode! \n");
        return -1;
    }
    frameIndex++;
    if (got_picture == 1) {
        LOGE(ISDEBUG, "Succeed to encode frame: %5d\tsize:%5d\n", frameIndex, pkt.size);
        pkt.stream_index = video_st->index;
        ret = av_write_frame(pFormatCtx, &pkt);
        av_free_packet(&pkt);
    }
    (*env)->ReleaseByteArrayElements(env, yuv420p_, yuv420p, 0);
    return ret;
}

int flush_encoder(AVFormatContext *fmt_ctx, unsigned int stream_index) {
    int ret;
    int got_frame;
    AVPacket enc_pkt;
    if (!(fmt_ctx->streams[stream_index]->codec->codec->capabilities &
          CODEC_CAP_DELAY))
        return 0;
    while (1) {
        enc_pkt.data = NULL;
        enc_pkt.size = 0;
        av_init_packet(&enc_pkt);
        ret = avcodec_encode_video2(fmt_ctx->streams[stream_index]->codec, &enc_pkt,
                                    NULL, &got_frame);
        av_frame_free(NULL);
        if (ret < 0)
            break;
        if (!got_frame) {
            ret = 0;
            break;
        }
        LOGE(ISDEBUG, "Flush Encoder: Succeed to encode 1 frame!\tsize:%5d\n", enc_pkt.size);
        /* mux encoded frame */
        ret = av_write_frame(fmt_ctx, &enc_pkt);
        if (ret < 0)
            break;
    }
    return ret;
}

JNIEXPORT jint JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_release(JNIEnv *env, jclass type) {

    //Flush Encoder
    int ret = flush_encoder(pFormatCtx, 0);
    if (ret < 0) {
        LOGE(ISDEBUG, "Flushing encoder failed\n");
        return -1;
    }

    //Write file trailer
    av_write_trailer(pFormatCtx);

    //Clean
    if (video_st) {
        avcodec_close(video_st->codec);
        av_free(pFrame);
        av_free(picture_buf);
    }
    avio_close(pFormatCtx->pb);
    avformat_free_context(pFormatCtx);
}

JNIEXPORT void JNICALL
Java_io_github_sawameimei_ffmpeg_FFmpegBridge_muxing(JNIEnv *env, jclass type,
                                                     jstring inputFileVideo_, jstring outputFile_) {
    const char *inputFileVideo = (*env)->GetStringUTFChars(env, inputFileVideo_, 0);
    const char *outputFile = (*env)->GetStringUTFChars(env, outputFile_, 0);

    int ret = android_ffmpeg_muxing((char *) inputFileVideo,
                                    NULL,
                                    (char *) outputFile);


    (*env)->ReleaseStringUTFChars(env, inputFileVideo_, inputFileVideo);
    (*env)->ReleaseStringUTFChars(env, outputFile_, outputFile);
    LOGE(ISDEBUG, "end");
}