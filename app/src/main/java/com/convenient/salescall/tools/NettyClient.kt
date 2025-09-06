package com.convenient.salescall.tools


import android.content.Context.TELEPHONY_SERVICE
import android.telephony.TelephonyManager
import com.convenient.salescall.app.CallApp
import com.convenient.salescall.datas.NettyMessage
import com.convenient.salescall.datas.UuidPrefs
import com.convenient.salescall.receiver.MessageCenter
import com.google.gson.Gson
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.codec.string.StringEncoder
import io.netty.handler.timeout.IdleState
import io.netty.handler.timeout.IdleStateEvent
import io.netty.handler.timeout.IdleStateHandler
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.GenericFutureListener
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import kotlin.math.pow

class NettyClient(private val host: String, private val port: Int) {
    private val group = NioEventLoopGroup()
    private var retryCount = 0
    private val maxRetryDelay = 30L

    // 新增连接状态变量
    private var isConnected = false
    private var currentChannel: Channel? = null

    fun start() {
        if (isConnected) {
            LogUtils.d("NettyClient", "Already connected")
            return
        }
        val bootstrap = Bootstrap().apply {
            group(group)
            channel(NioSocketChannel::class.java)
            option(ChannelOption.SO_KEEPALIVE, true)
            handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        DelimiterBasedFrameDecoder(
                            8192,
                            Unpooled.copiedBuffer("\n".toByteArray(StandardCharsets.UTF_8))
                        ),
                        StringDecoder(StandardCharsets.UTF_8),
                        StringEncoder(StandardCharsets.UTF_8),
                        IdleStateHandler(0, 25, 0, TimeUnit.SECONDS),
                        ClientHandler(this@NettyClient)
                    )
                }
            })
        }
        connect(bootstrap)
    }

    private fun connect(bootstrap: Bootstrap) {
        currentChannel?.close() // 关闭旧连接
        val future = bootstrap.connect(host, port).addListener { future ->
            if (future.isSuccess) {
                currentChannel = (future as ChannelFuture).channel()
                isConnected = true
                retryCount = 0
                LogUtils.d("NettyClient", "Connected to $host:$port")
                // 连接成功后立即发送鉴权消息
            } else {
                isConnected = false
                retryCount++
                val delay = minOf(2.0.pow(retryCount).toLong(), maxRetryDelay)
                LogUtils.d("NettyClient", "Connection failed, retrying in $delay seconds...")
                group.schedule({ connect(bootstrap) }, delay, TimeUnit.SECONDS)
            }
        }.sync()
        // 连接成功后，等待通道激活，然后发送认证
        future.channel().closeFuture().sync(); // 阻塞等待连接关闭
    }

    fun shutdown() {
        currentChannel?.close() // 关闭旧连接
        isConnected = false
        group.shutdownGracefully()
    }

    inner class ClientHandler(private val client: NettyClient) : ChannelInboundHandlerAdapter() {
        lateinit var msg: NettyMessage
        override fun channelActive(ctx: ChannelHandlerContext) {
            LogUtils.d("NettyClient", "Channel active: ${ctx.channel()}")
            // 构造符合格式的消息
            val telephonyManager =
                CallApp.appContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            var phoneNumber = telephonyManager.line1Number ?: "无法获取号码"
            phoneNumber = phoneNumber.replace("+86", "")
            val msg = NettyMessage("AUTH", UuidPrefs.getUuid(CallApp.appContext)!!, phoneNumber)
            val authMsg = Gson().toJson(msg) + "\n"
            LogUtils.d("NettyClient", "已发送鉴权消息:${authMsg}")
            val f = ctx.writeAndFlush(authMsg)
            f.addListener(GenericFutureListener { future: Future<in Void?>? ->
                if (future!!.isSuccess()) {
                    LogUtils.d("NettyClient", "📤 发送成功")
                } else {
                    LogUtils.d("NettyClient", "❌ 发送失败")
                    future.cause().printStackTrace()
                }
            })
            super.channelActive(ctx);
        }

        override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
            if (evt is IdleStateEvent && evt.state() == IdleState.WRITER_IDLE) {
                val telephonyManager =
                    CallApp.appContext.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
                var phoneNumber = telephonyManager.line1Number ?: "无法获取号码"
                phoneNumber = phoneNumber.replace("+86", "")
                val msg =
                    NettyMessage("HEARTBEAT", UuidPrefs.getUuid(CallApp.appContext)!!, phoneNumber)
                val authMsg = Gson().toJson(msg) + "\n"
                ctx.writeAndFlush(authMsg)
                LogUtils.d("NettyClient", "已发送心跳消息:${authMsg}")
            }
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            LogUtils.d("NettyClient", "Connection lost, reconnecting...")
            client.connect(Bootstrap().apply {
                group(group)
                channel(NioSocketChannel::class.java)
                handler(ctx.handler())
            })
        }

        override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
            LogUtils.d("NettyClient", "channelRead——Received from server: $msg")
            msg?.let {
                try {
                    val json = it as String
                    val nettyMessage = Gson().fromJson(json, NettyMessage::class.java)
                    LogUtils.d("NettyClient", "Parsed: $nettyMessage")
                    if (nettyMessage != null && nettyMessage.data != null && nettyMessage.data.isNotEmpty()) {
                        MessageCenter.post(nettyMessage.data)
                    }
                } catch (e: Exception) {
                    LogUtils.e("NettyClient", "JSON 解析失败: ${e.message}")
                }
            }

        }
    }
}


